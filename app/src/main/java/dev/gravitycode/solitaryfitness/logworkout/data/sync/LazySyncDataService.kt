package dev.gravitycode.solitaryfitness.logworkout.data.sync

import dev.gravitycode.caimito.kotlin.core.Log
import dev.gravitycode.caimito.kotlin.core.ResultOf
import dev.gravitycode.caimito.kotlin.core.error
import dev.gravitycode.solitaryfitness.logworkout.data.repo.WorkoutLogsRepository
import dev.gravitycode.solitaryfitness.util.data.DataCorruptionError
import dagger.Lazy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class LazySyncDataService(
    private val sourceRepository: Lazy<WorkoutLogsRepository>,
    private val destinationRepository: Lazy<WorkoutLogsRepository>
) : SyncDataService {

    companion object {

        const val TAG = "LazySyncDataService"
    }

    override suspend fun sync(mode: SyncMode, retryAttempts: Int): Flow<ResultOf<LocalDate>> {
        Log.v(TAG, "sync($mode, $retryAttempts)")

        val sourceRepository = sourceRepository.get()
        val destinationRepository = destinationRepository.get()

        return sourceRepository.metaData.getRecords().map { date ->
            val shouldCopy = when (mode) {
                SyncMode.PRESERVE -> !destinationRepository.metaData.containsRecord(date)
                SyncMode.OVERWRITE -> true
            }

            var result: ResultOf<LocalDate>? = null

            if (shouldCopy) {
                var i = 0
                do {
                    val readResult = sourceRepository.readWorkoutLog(date)
                    val writeResult = if (readResult.isSuccess) {
                        val log = readResult.getOrNull()
                        Log.v(TAG, "syncing record $date...")
                        destinationRepository.writeWorkoutLog(date, log!!)
                    } else {
                        /*
                        * TODO: Could readWorkoutLog fail for other reasons? Would need to check for an
                        *   IOException first? `Result.getOrThrow` may be useful here, combined with a multi-
                        *   level try-catch such as:
                        *
                        *       try {
                        *           result.getOrThrow()
                        *       }catch(ioe: IOException) {
                        *           Result.failure(ioe)
                        *       }catch() {
                        *           throw DataCorruptionError("metadata is out of sync with repository")
                        *       }
                        * */
                        throw DataCorruptionError("metadata is out of sync with repository")
                    }

                    if (writeResult.isSuccess) {
                        Log.v(TAG, "successfully synced record: $date")
                        result = ResultOf.success(date)
                    } else {
                        if (i < retryAttempts) {
                            Log.d(TAG, "Sync failed for $date, retrying...")
                        } else {
                            error("Failed to sync record for $date", writeResult)
                            result = ResultOf.failure(date, writeResult.exceptionOrNull()!!)
                        }
                    }
                } while (writeResult.isFailure && i++ < retryAttempts)
            } else {
                Log.d(TAG, "skipped $date")
                result = ResultOf.success(date)
            }

            if (result != null) {
                return@map result
            } else {
                throw IllegalStateException("no result generated for $date")
            }
        }
    }
}