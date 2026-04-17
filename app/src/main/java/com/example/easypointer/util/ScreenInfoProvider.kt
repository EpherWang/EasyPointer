package com.example.easypointer.util

import android.content.Context
import android.graphics.Point
import android.os.Build
import android.view.WindowManager

/**
 * Screen metrics helper with clamp support.
 */
class ScreenInfoProvider(private val context: Context) {

    data class ScreenSize(val width: Int, val height: Int)

    fun getScreenSize(): ScreenSize {
        val wm = context.getSystemService(WindowManager::class.java)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = wm.currentWindowMetrics.bounds
            ScreenSize(bounds.width(), bounds.height())
        } else {
            @Suppress("DEPRECATION")
            Point().also { wm.defaultDisplay.getRealSize(it) }.let {
                ScreenSize(it.x, it.y)
            }
        }
    }

    fun clamp(x: Int, y: Int): Pair<Int, Int> {
        val size = getScreenSize()
        val clampedX = x.coerceIn(0, (size.width - 1).coerceAtLeast(0))
        val clampedY = y.coerceIn(0, (size.height - 1).coerceAtLeast(0))
        return clampedX to clampedY
    }
}
