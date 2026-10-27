package com.smartfarm.app

import android.os.Bundle
import android.view.Gravity
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.smartfarm.app.data.FakeFarmRepository
import com.smartfarm.app.data.PestAlert
import com.smartfarm.app.databinding.ActivityMainBinding
import com.smartfarm.app.ui.warning.WarningDialogFragment

/**
 * 앱 전체의 "틀"을 담당하는 Activity (앱에 Activity는 이것 하나).
 *  - Toolbar          : 화면 제목, ← 뒤로가기, (온도·토양 화면에서) ↻ 새로고침
 *  - NavHostFragment  : 화면(Fragment)이 바뀌는 영역
 * 실제 화면 내용은 전부 Fragment가 담당합니다.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        applySystemBarPadding()
        setupNavigation()
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

        // [테스트용] 상단바 제목을 길게 누르면 병해충 알림 팝업이 뜸
        // TODO: FCM 연결 후에는 알림을 받았을 때 showPestAlert(...)를 호출하고 이 코드는 삭제
        binding.tvToolbarTitle.setOnLongClickListener {
            showPestAlert(FakeFarmRepository().getLatestPestAlert())
            true
        }
    }

    /**
     * 어느 화면에서든 병해충 발생 알림 팝업 띄우기.
     * 나중에 FCM 메시지를 받았을 때도 이 함수를 호출하면 됩니다.
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

        // 제목 옆 아이콘 (0 = 없음) - 디자인 시안 기준
        val icon = when (destination.id) {
            R.id.soilLogFragment -> R.drawable.ic_water_drop
            R.id.temperatureLogFragment -> 0
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
