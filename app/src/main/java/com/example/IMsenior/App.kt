package com.example.IMsenior

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class App : Application() {
    override fun onCreate() {
        super.onCreate()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "expiry",                       // channelId：要和後端/Manifest 的一致
                getString(R.string.expiryalert),                      // 使用者在系統看到的頻道名稱
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "到期前 3/2/1 天、當天、已過期的提醒"
                enableVibration(true)
            }

            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }
}
