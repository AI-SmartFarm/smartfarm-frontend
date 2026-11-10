package com.smartfarm.app.service

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.smartfarm.app.data.PestRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MyFirebaseMessagingService : FirebaseMessagingService() {

    private val pestRepository = PestRepository()

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM_TEST", "새 FCM 토큰 발급됨: $token")

        CoroutineScope(Dispatchers.IO).launch {
            val result = pestRepository.registerFcmToken(token)
            result.onSuccess {
                Log.d("FCM_TEST", "백엔드 FCM 토큰 등록 성공")
            }.onFailure { error ->
                Log.e("FCM_TEST", "백엔드 FCM 토큰 등록 실패: ${error.message}")
            }
        }
    }

    /**
     * 푸시를 받았을 때. 푸시는 "새 진단 확인해 봐" 신호로만 쓰고,
     * 팝업 내용은 진단 이력 API(API-008)에서 가져옴 (PestAlertChecker).
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(
            "FCM_TEST",
            "FCM 메시지 수신: ${remoteMessage.notification?.title} / ${remoteMessage.notification?.body} / data=${remoteMessage.data}"
        )
        PestAlertNotifier.onPushReceived(
            context = applicationContext,
            title = remoteMessage.notification?.title,
            body = remoteMessage.notification?.body
        )
    }
}
