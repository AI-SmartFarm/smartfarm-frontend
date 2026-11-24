package com.smartfarm.app.service

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.smartfarm.app.data.PestRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * FCM(Firebase Cloud Messaging) 수신 서비스. AndroidManifest.xml 에 등록되어 있습니다.
 *  - onNewToken       : 토큰이 새로 발급되면 백엔드에 다시 등록
 *  - onMessageReceived: 병해충 푸시를 받으면 PestAlertNotifier 로 넘김
 */
class MyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "FCM"
    }

    private val pestRepository = PestRepository()

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        CoroutineScope(Dispatchers.IO).launch {
            pestRepository.registerFcmToken(token)
                .onSuccess { Log.d(TAG, "새 FCM 토큰 등록 성공") }
                .onFailure { Log.e(TAG, "새 FCM 토큰 등록 실패: ${it.message}") }
        }
    }

    /**
     * 푸시는 "새 진단을 확인하라"는 신호로만 쓰고,
     * 팝업 내용은 진단 이력 API(API-008)에서 가져옵니다 (PestAlertChecker).
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "푸시 수신: ${remoteMessage.notification?.title}")
        PestAlertNotifier.onPushReceived(
            context = applicationContext,
            title = remoteMessage.notification?.title,
            body = remoteMessage.notification?.body
        )
    }
}
