package com.example.easypointer.service

import android.accessibilityservice.AccessibilityService
import android.content.res.Configuration
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.example.easypointer.app.AppConstants
import com.example.easypointer.app.PointerController
import com.example.easypointer.model.PointerCommand
import com.example.easypointer.overlay.PointerOverlayManager
import com.example.easypointer.socket.SocketServer

/**
 * Accessibility service that owns and updates the pointer overlay.
 */
class MyAccessibilityService : AccessibilityService(),
    PointerController.OverlayCommandTarget,
    SocketServer.Listener {

    private var overlayManager: PointerOverlayManager? = null
    private var socketServer: SocketServer? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i(AppConstants.TAG_APP, "Accessibility service connected")
        overlayManager = PointerOverlayManager(this)
        PointerController.registerOverlayTarget(this)
        PointerController.updateAccessibilityEnabled(true)
        PointerController.updateStatus("Accessibility connected")
        startSocketIfNeeded()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // No event traversal/read required by design.
    }

    override fun onInterrupt() {
        Log.w(AppConstants.TAG_APP, "Accessibility service interrupted")
    }

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        cleanup()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        cleanup()
        super.onDestroy()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        overlayManager?.handleDisplayChanged()
        PointerController.publishOverlayState()
    }

    override fun showPointer() {
        overlayManager?.show()
    }

    override fun hidePointer() {
        overlayManager?.hide()
    }

    override fun togglePointer() {
        overlayManager?.toggle()
    }

    override fun moveTo(x: Int, y: Int) {
        overlayManager?.moveTo(x, y)
    }

    override fun offsetBy(dx: Int, dy: Int) {
        overlayManager?.offsetBy(dx, dy)
    }

    override fun moveToCenter() {
        overlayManager?.moveToCenter()
    }

    override fun getCurrentX(): Int = overlayManager?.getX() ?: 0

    override fun getCurrentY(): Int = overlayManager?.getY() ?: 0

    override fun isPointerVisible(): Boolean = overlayManager?.isVisible() ?: false

    override fun onServerStarted(port: Int) {
        PointerController.updateSocketRunning(true)
        PointerController.updateStatus("Socket listening on $port")
    }

    override fun onServerStopped() {
        PointerController.updateSocketRunning(false)
        PointerController.updateStatus("Socket stopped")
    }

    override fun onLineReceived(line: String) {
        PointerController.updateLastCommand(line)
    }

    override fun onCommand(command: PointerCommand): String? {
        val dispatched = PointerController.dispatchCommand(command)
        return if (dispatched || command == PointerCommand.Ping) {
            PointerController.publishOverlayState()
            if (command is PointerCommand.Move || command is PointerCommand.Offset) {
                null
            } else {
                "OK"
            }
        } else {
            "ERROR ${AppConstants.ERROR_SERVICE_UNAVAILABLE}"
        }
    }

    override fun onError(message: String, t: Throwable?) {
        Log.e(AppConstants.TAG_APP, message, t)
        PointerController.updateStatus("Socket error: $message")
    }

    private fun startSocketIfNeeded() {
        if (socketServer?.isRunning() == true) return
        socketServer = SocketServer(AppConstants.SOCKET_PORT, this).also { it.start() }
    }

    private fun stopSocketIfNeeded() {
        socketServer?.stop()
        socketServer = null
        PointerController.updateSocketRunning(false)
    }

    private fun cleanup() {
        stopSocketIfNeeded()
        PointerController.unregisterOverlayTarget(this)
        PointerController.updateAccessibilityEnabled(false)
        PointerController.updateStatus("Accessibility disconnected")
        try {
            overlayManager?.release()
        } catch (t: Throwable) {
            Log.e(AppConstants.TAG_APP, "Overlay release failed", t)
        }
        overlayManager = null
    }
}
