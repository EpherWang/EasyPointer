package com.example.easypointer.app

import com.example.easypointer.model.PointerCommand
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * App-level coordinator for service state and pointer command dispatch.
 *
 * Keeps communication between foreground socket service and accessibility service explicit,
 * while avoiding hard references to service instances.
 */
object PointerController {

    interface OverlayCommandTarget {
        fun showPointer()
        fun hidePointer()
        fun togglePointer()
        fun moveTo(x: Int, y: Int)
        fun offsetBy(dx: Int, dy: Int)
        fun moveToCenter()
        fun getCurrentX(): Int
        fun getCurrentY(): Int
        fun isPointerVisible(): Boolean
    }

    data class UiState(
        val accessibilityEnabled: Boolean = false,
        val socketRunning: Boolean = false,
        val port: Int = AppConstants.SOCKET_PORT,
        val lastCommand: String = "-",
        val pointerX: Int = 0,
        val pointerY: Int = 0,
        val pointerVisible: Boolean = false,
        val statusMessage: String = "Idle"
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    @Volatile
    private var overlayTarget: OverlayCommandTarget? = null

    fun registerOverlayTarget(target: OverlayCommandTarget) {
        overlayTarget = target
        publishOverlayState()
    }

    fun unregisterOverlayTarget(target: OverlayCommandTarget) {
        if (overlayTarget === target) {
            overlayTarget = null
            _uiState.value = _uiState.value.copy(
                pointerVisible = false,
                statusMessage = "Accessibility overlay unavailable"
            )
        }
    }

    fun updateAccessibilityEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(accessibilityEnabled = enabled)
    }

    fun updateSocketRunning(running: Boolean) {
        _uiState.value = _uiState.value.copy(socketRunning = running)
    }

    fun updateLastCommand(text: String) {
        _uiState.value = _uiState.value.copy(lastCommand = text)
    }

    fun updateStatus(message: String) {
        _uiState.value = _uiState.value.copy(statusMessage = message)
    }

    fun dispatchCommand(command: PointerCommand): Boolean {
        val target = overlayTarget ?: return false
        when (command) {
            PointerCommand.Show -> target.showPointer()
            PointerCommand.Hide -> target.hidePointer()
            PointerCommand.Toggle -> target.togglePointer()
            is PointerCommand.Move -> target.moveTo(command.x, command.y)
            is PointerCommand.Offset -> target.offsetBy(command.dx, command.dy)
            PointerCommand.Ping -> {
                // no overlay operation needed
            }
        }
        publishOverlayState()
        return true
    }

    fun moveToCenterFromUi(): Boolean {
        val target = overlayTarget ?: return false
        target.moveToCenter()
        publishOverlayState()
        return true
    }

    fun publishOverlayState() {
        val target = overlayTarget
        if (target != null) {
            _uiState.value = _uiState.value.copy(
                pointerX = target.getCurrentX(),
                pointerY = target.getCurrentY(),
                pointerVisible = target.isPointerVisible()
            )
        }
    }
}
