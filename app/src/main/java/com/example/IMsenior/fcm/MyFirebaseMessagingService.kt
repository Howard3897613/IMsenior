// com.example.IMsenior.fcm.MyFirebaseMessagingService
package com.example.IMsenior.fcm

import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.IMsenior.MainActivity
import com.example.IMsenior.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(msg: RemoteMessage) {
        // 取標題、內文（先用 notification，沒有就用 data）
        val title = msg.notification?.title ?: msg.data["title"] ?: "食材到期提醒"
        val body  = msg.notification?.body  ?: msg.data["body"]  ?: ""

        // 點擊通知要開啟 MainActivity，並把 data 帶過去
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra("fromFcm", true)
            putExtra("foodDocPath", msg.data["foodDocPath"])
        }
        val pi = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 「expiry」頻道要跟 App.kt & Manifest 的 channelId 一致
        val builder = NotificationCompat.Builder(this, "expiry")
            .setSmallIcon(R.mipmap.ic_launcher) // 或你自己的小圖示
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        NotificationManagerCompat.from(this).notify((System.currentTimeMillis()%100000).toInt(), builder.build())
    }
}
