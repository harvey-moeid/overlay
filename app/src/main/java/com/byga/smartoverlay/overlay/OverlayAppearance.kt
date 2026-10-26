package com.byga.smartoverlay.overlay

import android.view.WindowManager
import androidx.datastore.preferences.core.intPreferencesKey

object OverlayAppearance {
    const val DEFAULT_DP = 28
    const val MIN_DP = 24
    const val MAX_DP = 56
    const val TOUCH_DP = 48
    // A new key applies the compact default to upgrades that saved the old 80–128dp size.
    val sizeKey = intPreferencesKey("bubble_visual_size_dp")
    const val PANEL_FLAGS = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
        WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
    fun visualDp(value: Int) = value.coerceIn(MIN_DP, MAX_DP)
    fun touchDp(value: Int) = maxOf(TOUCH_DP, visualDp(value))
}
