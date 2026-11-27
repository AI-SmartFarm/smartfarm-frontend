package com.smartfarm.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.smartfarm.app.MainActivity
import com.smartfarm.app.R

/**
 * 병해충 푸시(FCM)를 받았을 때 앱 상태에 따라 나눠 처리합니다.
 *  - 앱이 화면에 떠 있으면 → MainActivity 가 바로 새 진단을 확인해서 경고 팝업
 *  - 앱이 꺼져 있거나 뒤에 있으면 → 휴대폰 상단 알림. 누르면 앱이 열리고
 *    MainActivity 의 진단 확인(화면에 나타날 때 + 15초마다)이 팝업을 띄움
 *
 * 팝업 내용은 진단 이력 API(API-008)에서 가져오므로, 푸시의 제목·내용은 휴대폰 알림 문구로만 씁니다.
 */
object PestAlertNotifier {

    private const val TAG = "FCM"
    private const val CHANNEL_ID = "pest_alert"

    /** 화면에 떠 있는 MainActivity 가 등록. null 이면 앱이 화면에 없음 → 휴대폰 알림 */
    @Volatile
    var foregroundListener: (() -> Unit)? = null

    fun onPushReceived(context: Context, title: String?, body: String?) {
        val listener = foregroundListener
        if (listener != null) {
            listener()
        } else {
            showSystemNotification(context, title, body)
        }
    }

    private fun showSystemNotification(context: Context, title: String?, body: String?) {
        createChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            // 앱이 이미 열려 있으면 새로 만들지 않고 그 화면으로
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_leaf)
            .setContentTitle(title ?: context.getString(R.string.pest_notification_title))
            .setContentText(body ?: context.getString(R.string.pest_notification_body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(System.currentTimeMillis().toInt(), notification)
        } catch (e: SecurityException) {
            // 사용자가 알림 권한을 거부한 경우
            Log.w(TAG, "알림 권한이 없어 휴대폰 알림을 띄우지 못함: ${e.message}")
        }
    }

    /** Android 8 이상은 알림 "채널"이 있어야 알림이 뜸 (이미 있으면 아무 일도 안 함) */
    private fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.pest_notification_channel),
            NotificationManager.IMPORTANCE_HIGH
        )
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}
