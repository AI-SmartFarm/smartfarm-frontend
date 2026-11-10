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
 * 푸시(FCM)를 받았을 때
 *  - 앱이 화면에 떠 있으면 → MainActivity 에게 "진단 확인해서 팝업 띄워"
 *  - 앱이 꺼져 있거나 뒤에 있으면 → 폰 상단 알림. 누르면 앱이 열리고,
 *    MainActivity.onStart 의 진단 확인이 팝업을 띄움
 *
 * 팝업 내용은 푸시가 아니라 진단 이력 API(API-008)에서 가져오므로
 * 백엔드는 푸시에 무엇을 넣든 상관없습니다 (제목·내용이 있으면 폰 알림 문구로만 씀).
 */
object PestAlertNotifier {

    private const val TAG = "FCM_TEST"
    private const val CHANNEL_ID = "pest_alert"
    private const val CHANNEL_NAME = "병해충 알림"

    /** 화면에 떠 있는 MainActivity 가 등록. null 이면 앱이 화면에 없음 → 폰 알림 */
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
            .setContentTitle(title ?: "🐛 병해충 감지 알림")
            .setContentText(body ?: "AI가 새 병해충 진단을 보냈어요. 눌러서 확인하세요.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(System.currentTimeMillis().toInt(), notification)
        } catch (e: SecurityException) {
            // 사용자가 알림 권한을 거부한 경우
            Log.w(TAG, "알림 권한이 없어 폰 알림을 띄우지 못함: ${e.message}")
        }
    }

    /** Android 8 이상은 알림 "채널"이 있어야 알림이 뜸 (이미 있으면 아무 일도 안 함) */
    private fun createChannel(context: Context) {
        val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH)
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}
