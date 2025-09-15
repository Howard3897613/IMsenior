package com.example.IMsenior

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract

// ★ 新增這三個 import（非 KTX 版，最穩）
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging

class SignInActivity : AppCompatActivity() {

    private val signInLauncher =
        registerForActivityResult(FirebaseAuthUIActivityResultContract()) { result ->
            if (result.resultCode == RESULT_OK) {
                // ★ 登入成功：刷新並上傳 FCM token，完成後再進主畫面
                uploadFcmTokenThenGoHome()
            } else {
                // 登入失敗
                Toast.makeText(this, "登入失敗", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val providers = arrayListOf(
            AuthUI.IdpConfig.EmailBuilder().build()
            // AuthUI.IdpConfig.GoogleBuilder().build() // 之後要用可打開
        )

        val signInIntent = AuthUI.getInstance()
            .createSignInIntentBuilder()
            .setAvailableProviders(providers)
            .setTheme(R.style.Theme_IMsenior) // optional
            .build()

        signInLauncher.launch(signInIntent)
    }

    /** 取得/刷新 token → 上傳到 users/{uid}/fcmTokens/{token} → 進主畫面 */
    private fun uploadFcmTokenThenGoHome() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null || user.isAnonymous) {
            goHome()
            return
        }

        // 先刪舊 token，再拿新 token（避免殘留）
        FirebaseMessaging.getInstance().deleteToken().addOnCompleteListener {
            FirebaseMessaging.getInstance().token
                .addOnSuccessListener { token ->
                    FirebaseFirestore.getInstance()
                        .collection("users").document(user.uid)
                        .collection("fcmTokens").document(token)
                        .set(
                            mapOf(
                                "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                                "platform" to "android"
                            )
                        )
                        .addOnCompleteListener {
                            goHome() // 無論成功/失敗都進主畫面
                        }
                }
                .addOnFailureListener { e ->
                    Log.e("SignInActivity", "get FCM token failed", e)
                    goHome()
                }
        }
    }

    private fun goHome() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
