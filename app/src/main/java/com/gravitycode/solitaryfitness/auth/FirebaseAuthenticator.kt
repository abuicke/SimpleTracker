package com.gravitycode.solitaryfitness.auth

import android.util.Log
import androidx.activity.ComponentActivity
import com.firebase.ui.auth.AuthMethodPickerLayout
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.google.firebase.auth.FirebaseAuth
import com.gravitycode.solitaryfitness.R
import com.gravitycode.solitaryfitness.util.data.GetActivityResult
import com.gravitycode.solitaryfitness.util.error.debugError
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * @see [com.firebase.ui.auth.ErrorCodes] for error code meanings
 * */
class FirebaseAuthenticator(
    private val activity: ComponentActivity,
    private val firebaseAuth: FirebaseAuth,
    private val firebaseAuthUi: AuthUI
) : Authenticator {

    private companion object {

        private const val TAG = "FirebaseAuthenticator"
    }

    /**
     * Authentication providers
     * */
    private val providers = arrayListOf(
//        AuthUI.IdpConfig.EmailBuilder().build(),
        // AuthUI.IdpConfig.PhoneBuilder().build(),
        AuthUI.IdpConfig.GoogleBuilder().build(),
        // AuthUI.IdpConfig.FacebookBuilder().build(),
        // AuthUI.IdpConfig.TwitterBuilder().build()
    )

    /**
     * Custom Firebase Auth UI layout
     * */
    private val authMethodPickerLayout = AuthMethodPickerLayout.Builder(R.layout.auth_method_picker_layout)
        .setGoogleButtonId(R.id.btn_google)
        .build()

    private val signInIntent = firebaseAuthUi.createSignInIntentBuilder()
        .setIsSmartLockEnabled(false)
        .setAvailableProviders(providers)
        .setAuthMethodPickerLayout(authMethodPickerLayout)
        .build()

    private val contract = FirebaseAuthUIActivityResultContract()
    private val getFirebaseSignInResult = GetActivityResult(activity, contract)

    private var user: User? = null

    init {
        val currentFirebaseUser = firebaseAuth.currentUser
        if (currentFirebaseUser != null) {
            this.user = User(currentFirebaseUser)
            Log.d(TAG, "user already signed in as: $user")
        }
    }

    override suspend fun signIn(): Result<User> {
        if (user != null) {
            val errMsg = "already signed in as: $user"
            debugError(errMsg)
            return Result.failure(IllegalStateException(errMsg))
        }

        val result = getFirebaseSignInResult(signInIntent)
        val response = result.idpResponse

        return if (result.resultCode == ComponentActivity.RESULT_OK) {
            Log.d(TAG, "sign in successful\n$response")
            val firebaseUser = firebaseAuth.currentUser!!
            val user = User(firebaseUser)
            this.user = user
            Result.success(user)
        } else {
            Result.failure(
                if (response != null && response.error != null) {
                    response.error!!
                } else {
                    AuthenticationException(
                        "unspecified firebase ui exception, result code: ${result.resultCode}"
                    )
                }
            )
        }
    }

    override suspend fun signOut(): Result<Unit> {
        return suspendCoroutine { continuation ->
            if (user == null) {
                val errMsg = "no user signed in"
                debugError(errMsg)
                continuation.resume(Result.failure(IllegalStateException(errMsg)))
            }

            firebaseAuthUi.signOut(activity)
                .addOnFailureListener {
                    val errMsg = "failed to sign out user: $user"
                    Log.d(TAG, errMsg)
                    val exception = AuthenticationException(errMsg)
                    val result = Result.failure<Unit>(exception)
                    continuation.resume(result)
                }
                .addOnSuccessListener {
                    Log.d(TAG, "signed out user: $user")
                    user = null
                    val result = Result.success(Unit)
                    continuation.resume(result)
                }
        }
    }

    override fun isUserSignedIn() = user != null

    override fun getSignedInUser() = user
}