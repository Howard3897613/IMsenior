package com.example.IMsenior.fcm

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging

object FcmTokenUploader {
    fun uploadFcmTokenIfNeeded() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        if (user.isAnonymous) return

        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                FirebaseFirestore.getInstance()
                    .collection("users").document(user.uid)
                    .collection("fcmTokens").document(token)
                    .set(mapOf(
                        "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                        "platform" to "android"
                    ))
            }
            .addOnFailureListener { e ->
                Log.e("FcmTokenUploader", "get token failed", e)
            }
    }
}
