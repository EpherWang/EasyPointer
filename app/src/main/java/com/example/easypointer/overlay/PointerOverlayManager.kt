package com.example.easypointer.overlay

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import com.example.easypointer.app.AppConstants
import com.example.easypointer.util.ScreenInfoProvider

/**
 * Manages a non-interactive pointer overlay view.
 */
class PointerOverlayManager(
    context: Context,
    pointerColor: Int = AppConstants.DEFAULT_POINTER_COLOR,
    pointerSizeDp: Int = AppConstants.DEFAULT_POINTER_SIZE_DP
) {
    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private val windowManager = appContext.getSystemService(WindowManager::class.java)
    private val screenInfoProvider = ScreenInfoProvider(appContext)

    private var pointerView: View? = null
    private var isAdded: Boolean = false
    private var centerX: Int = 0
    private var centerY: Int = 0

    private val pointerSizePx = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        pointerSizeDp.toFloat(),
        appContext.resources.displayMetrics
    ).toInt().coerceAtLeast(2)

    private val layoutParams = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.TOP or Gravity.START
    }

    init {
        val screen = screenInfoProvider.getScreenSize()
        centerX = screen.width / 2
        centerY = screen.height / 2
    }

    fun show() = runOnMain {
        ensureView()
        if (!isAdded && pointerView != null) {
            try {
                updateLayoutParamsForCenter(centerX, centerY)
                windowManager.addView(pointerView, layoutParams)
                isAdded = true
            } catch (t: Throwable) {
                Log.e(AppConstants.TAG_APP, "Failed to add overlay view", t)
            }
        }
    }

    fun hide() = runOnMain {
        if (isAdded && pointerView != null) {
            try {
                windowManager.removeView(pointerView)
            } catch (t: Throwable) {
                Log.e(AppConstants.TAG_APP, "Failed to remove overlay view", t)
            } finally {
                isAdded = false
            }
        }
    }

    fun toggle() {
        if (isVisible()) hide() else show()
    }

    fun moveTo(targetX: Int, targetY: Int) = runOnMain {
        val (x, y) = screenInfoProvider.clamp(targetX, targetY)
        centerX = x
        centerY = y

        if (isAdded && pointerView != null) {
            try {
                updateLayoutParamsForCenter(centerX, centerY)
                windowManager.updateViewLayout(pointerView, layoutParams)
            } catch (t: Throwable) {
                Log.e(AppConstants.TAG_APP, "Failed to update overlay position", t)
            }
        }
    }

    fun offsetBy(dx: Int, dy: Int) {
        moveTo(centerX + dx, centerY + dy)
    }

    fun moveToCenter() {
        val screen = screenInfoProvider.getScreenSize()
        moveTo(screen.width / 2, screen.height / 2)
    }

    fun handleDisplayChanged() {
        val (x, y) = screenInfoProvider.clamp(centerX, centerY)
        moveTo(x, y)
    }

    fun isVisible(): Boolean = isAdded

    fun getX(): Int = centerX

    fun getY(): Int = centerY

    fun release() = runOnMain {
        hide()
        pointerView = null
    }

    private fun ensureView() {
        if (pointerView != null) return
        pointerView = View(appContext).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.BLUE)
            }
            layoutParams = WindowManager.LayoutParams(pointerSizePx, pointerSizePx)
        }
    }

    private fun updateLayoutParamsForCenter(x: Int, y: Int) {
        layoutParams.x = x - pointerSizePx / 2
        layoutParams.y = y - pointerSizePx / 2
        layoutParams.width = pointerSizePx
        layoutParams.height = pointerSizePx
    }

    private fun runOnMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) block() else mainHandler.post(block)
    }
}
