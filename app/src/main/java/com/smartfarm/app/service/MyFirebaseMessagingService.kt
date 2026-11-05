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

    // 1. FCM 토큰이 새롭게 발급되거나 갱신될 때 자동 호출 (API-010 연동)
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM_TEST", "새 FCM 토큰 발급됨: $token")

        // 백엔드 서버에 토큰 등록 요청
        CoroutineScope(Dispatchers.IO).launch {
            val result = pestRepository.registerFcmToken(token)
            result.onSuccess {
                Log.d("FCM_TEST", "백엔드 FCM 토큰 등록 성공")
            }.onFailure { error ->
                Log.e("FCM_TEST", "백엔드 FCM 토큰 등록 실패: ${error.message}")
            }
        }
    }

    // 2. 푸시 알림 메시지 수신 시 처리
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d("FCM_TEST", "FCM 메시지 수신: ${remoteMessage.notification?.title} / ${remoteMessage.notification?.body}")
        // 필요 시 시스템 상단 알림(Notification Bar) 띄우기 로직 추가 영역
    }
}