package com.example.todolistapp

import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth

class FacebookLoginHandler(private val activity: AppCompatActivity, private val auth: FirebaseAuth) {

    private val callbackManager: CallbackManager = CallbackManager.Factory.create()

    init {
        // Daftarkan callback ke LoginManager
        LoginManager.getInstance().registerCallback(callbackManager, object : FacebookCallback<LoginResult> {
            override fun onSuccess(loginResult: LoginResult) {
                Log.d("FacebookLoginHandler", "facebook:onSuccess:$loginResult")
                handleFacebookAccessToken(loginResult.accessToken.token)
            }

            override fun onCancel() {
                Log.d("FacebookLoginHandler", "facebook:onCancel")
                Toast.makeText(activity, "Facebook login cancelled.", Toast.LENGTH_SHORT).show()
            }

            override fun onError(error: FacebookException) {
                Log.d("FacebookLoginHandler", "facebook:onError", error)
                Toast.makeText(activity, "Facebook login failed: ${error.message}", Toast.LENGTH_LONG).show()
            }
        })
    }

    fun signIn() {
        // Memulai alur login Facebook
        LoginManager.getInstance().logInWithReadPermissions(activity, listOf("email", "public_profile"))
    }

    private fun handleFacebookAccessToken(token: String) {
        val credential = FacebookAuthProvider.getCredential(token)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(activity) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d("FacebookLoginHandler", "signInWithCredential:success")
                    navigateToHome()
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w("FacebookLoginHandler", "signInWithCredential:failure", task.exception)
                    Toast.makeText(activity, "Firebase authentication with Facebook failed.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    // Metode ini HARUS dipanggil dari onActivityResult di LoginActivity
    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        callbackManager.onActivityResult(requestCode, resultCode, data)
    }

    fun signOut() {
        auth.signOut()
        LoginManager.getInstance().logOut()
    }

    private fun navigateToHome() {
        val intent = Intent(activity, HomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        activity.startActivity(intent)
        activity.finish()
    }
}

