package com.gravitycode.solitaryfitness.test_implementations

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.gravitycode.solitaryfitness.auth.Authenticator
import com.gravitycode.solitaryfitness.auth.User
import com.gravitycode.solitaryfitness.util.data.deleteDocuments
import com.gravitycode.solitaryfitness.log_workout.data.FirestoreWorkoutLogsRepository

class TestFirestoreWorkoutLogsRepository(
    private val firestore: FirebaseFirestore,
    userName: String,
    userEmail: String,
    profilePicUrl: String
) : FirestoreWorkoutLogsRepository(
    TestAppController(),
    firestore,
    TestAuthenticator(TEST_USER_ID, userName, userEmail, profilePicUrl)
) {

    private companion object {

        const val TEST_USER_ID = "0"
    }

    override fun users(): CollectionReference {
        return firestore.collection("test-users")
    }

    suspend fun clearTestRecords() {
        // Can't delete a collection or a document that only contains a collection and no data fields
        // directly, need to delete everything it contains and it will automatically be removed. If all a
        // document contains is a collection, it won't even show up in a query for some genius reason, but
        // if you add a field to it it will. So in this case I need to delete all the documents contained
        // within the workout-logs subcollection and the test user will be automatically removed.
        workoutLogs(TEST_USER_ID).deleteDocuments()
    }
}