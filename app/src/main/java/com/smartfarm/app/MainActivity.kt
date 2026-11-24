package com.smartfarm.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.google.firebase.messaging.FirebaseMessaging
import com.smartfarm.app.data.PestAlert
import com.smartfarm.app.data.PestAlertChecker
import com.smartfarm.app.data.PestRepository
import com.smartfarm.app.databinding.ActivityMainBinding
import com.smartfarm.app.service.PestAlertNotifier
import com.smartfarm.app.ui.warning.WarningDialogFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 앱 전체의 "틀"을 담당하는 Activity (앱에 Activity 는 이것 하나).
 *  - Toolbar          : 화면 제목, ← 뒤로가기, ↻ 새로고침
 *  - NavHostFragment  : 화면(Fragment)이 바뀌는 영역
 *  - 병해충 알림       : FCM 토큰 등록, 알림 권한 요청, 새 감염 진단 확인 → 경고 팝업
 * 실제 화면 내용은 전부 Fragment 가 담당합니다.
 */
class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "FCM"
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration

    /** Android 13 이상: "알림 허용할까요?" 창의 결과 */
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) {
                Toast.makeText(this, R.string.notification_permission_denied, Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applySystemBarPadding()
        setupNavigation()

        // 앱 시작 시 FCM 토큰을 백엔드에 등록 (병해충 푸시를 받기 위해)
        initFcmToken()
        askNotificationPermission()
    }

    /**
     * 앱이 화면에 나타날 때마다(처음 실행, 다른 앱에서 돌아옴, 휴대폰 알림을 눌러 열림)
     * 새 감염 진단이 있는지 확인 → 있으면 경고 팝업.
     * 화면에 떠 있는 동안 푸시가 오면 PestAlertNotifier 가 이 확인을 다시 실행합니다.
     */
    override fun onStart() {
        super.onStart()
        PestAlertNotifier.foregroundListener = { checkNewPestAlert() }
        checkNewPestAlert()
    }

    override fun onStop() {
        super.onStop()
        PestAlertNotifier.foregroundListener = null
    }

    /** 진단 이력(API-008)에서 새 감염 진단을 찾아 팝업 */
    private fun checkNewPestAlert() {
        lifecycleScope.launch {
            val alert = PestAlertChecker.checkNewInfection(applicationContext) ?: return@launch
            showPestAlert(alert)
        }
    }

    /** Android 13(API 33) 이상은 알림을 띄우려면 사용자 허락이 필요 → 처음 한 번 물어봄 */
    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return   // 12 이하는 자동 허용
        val granted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    /** 이 기기의 FCM 토큰을 받아 백엔드(API-010)에 등록. 같은 토큰은 서버가 덮어씀 */
    private fun initFcmToken() {
        try {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w(TAG, "FCM 토큰 가져오기 실패", task.exception)
                    return@addOnCompleteListener
                }
                val token = task.result
                lifecycleScope.launch(Dispatchers.IO) {
                    PestRepository().registerFcmToken(token)
                        .onSuccess { Log.d(TAG, "FCM 토큰 등록 성공") }
                        .onFailure { Log.e(TAG, "FCM 토큰 등록 실패: ${it.message}") }
                }
            }
        } catch (e: Exception) {
            // Firebase 설정(google-services.json)이 없어도 앱이 꺼지지 않게
            Log.w(TAG, "Firebase 초기화 안 됨: ${e.message}")
        }
    }

    private fun setupNavigation() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false) // 기본 제목 대신 tvToolbarTitle 사용

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment
        navController = navHostFragment.navController

        // 대시보드에서는 ← 버튼 없음, 나머지 화면에서는 ← 버튼이 자동으로 생김
        appBarConfiguration = AppBarConfiguration(setOf(R.id.homeFragment))
        setupActionBarWithNavController(navController, appBarConfiguration)

        // 화면이 바뀔 때마다 Toolbar 제목 변경 (팝업이 뜰 때는 뒤 화면 제목을 그대로 둠)
        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.navigatorName == "dialog") return@addOnDestinationChangedListener
            updateToolbarTitle(destination)
        }
    }

    /**
     * 어느 화면에서든 병해충 발생 알림 팝업 띄우기.
     * 새 감염 진단을 찾으면 checkNewPestAlert() 가 이 함수를 불러 줍니다.
     */
    fun showPestAlert(alert: PestAlert) {
        navController.navigate(
            R.id.action_global_warningDialog,
            WarningDialogFragment.createArgs(alert)
        )
    }

    private fun updateToolbarTitle(destination: NavDestination) {
        val title = binding.tvToolbarTitle
        title.text = destination.label // nav_graph.xml 의 android:label

        // 제목 옆 아이콘 (디자인 시안 기준)
        val icon = when (destination.id) {
            R.id.soilLogFragment -> R.drawable.ic_water_drop
            R.id.temperatureLogFragment -> R.drawable.ic_thermometer
            else -> R.drawable.ic_leaf
        }
        title.setCompoundDrawablesRelativeWithIntrinsicBounds(icon, 0, 0, 0)

        // 디자인 시안 기준: 대시보드·내 작물 확인은 왼쪽 정렬, 온도·토양 로그는 가운데 정렬
        val alignStart = destination.id == R.id.homeFragment ||
                destination.id == R.id.cropCheckFragment

        val params = title.layoutParams as Toolbar.LayoutParams
        params.gravity = if (alignStart) Gravity.START or Gravity.CENTER_VERTICAL else Gravity.CENTER
        title.layoutParams = params
    }

    // Toolbar의 ← 버튼을 눌렀을 때
    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    // 상태바/내비게이션바 영역만큼 여백을 줘서 화면이 가려지지 않게 함 (Android 15 대응)
    private fun applySystemBarPadding() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            WindowInsetsCompat.CONSUMED
        }
    }
}
