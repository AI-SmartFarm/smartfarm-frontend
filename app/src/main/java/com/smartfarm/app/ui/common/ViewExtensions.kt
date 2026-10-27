package com.smartfarm.app.ui.common

import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import com.smartfarm.app.R

/**
 * 여러 화면에서 같이 쓰는 작은 도우미 함수들.
 */

/** 배지를 "적정"(초록) 또는 "주의"(빨강)로 바꿈 */
fun TextView.showStatusBadge(isNormal: Boolean) {
    if (isNormal) {
        setText(R.string.status_normal)
        setBackgroundResource(R.drawable.bg_badge_green)
        setTextColor(ContextCompat.getColor(context, R.color.green_primary))
    } else {
        setText(R.string.status_warning)
        setBackgroundResource(R.drawable.bg_badge_red)
        setTextColor(ContextCompat.getColor(context, R.color.red_primary))
    }
}

/**
 * 상단바 오른쪽에 새로고침(↻) 버튼을 달고, 누르면 onRefresh 실행.
 * 화면을 나가면 버튼도 자동으로 사라집니다.
 */
fun Fragment.addRefreshMenu(onRefresh: () -> Unit) {
    requireActivity().addMenuProvider(object : MenuProvider {
        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            menuInflater.inflate(R.menu.menu_refresh, menu)
        }

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
            if (menuItem.itemId != R.id.action_refresh) return false
            onRefresh()
            return true
        }
    }, viewLifecycleOwner, Lifecycle.State.RESUMED)
}
