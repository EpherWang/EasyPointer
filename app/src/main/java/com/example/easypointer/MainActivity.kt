package com.example.easypointer

import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.easypointer.app.AppConstants
import com.example.easypointer.app.PointerController
import com.example.easypointer.databinding.ActivityMainBinding
import com.example.easypointer.model.PointerCommand
import com.example.easypointer.service.SocketForegroundService
import kotlinx.coroutines.launch

/**
 * Main control panel for accessibility status, socket state and quick pointer tests.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupButtons()
        observeState()
    }

    override fun onResume() {
        super.onResume()
        PointerController.updateAccessibilityEnabled(isAccessibilityServiceEnabled())
        PointerController.publishOverlayState()
    }

    private fun setupButtons() {
        binding.btnOpenAccessibilitySettings.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        binding.btnStartSocket.setOnClickListener { startSocketService() }
        binding.btnStopSocket.setOnClickListener { stopSocketService() }

        binding.btnShowPointer.setOnClickListener {
            sendUiCommand(PointerCommand.Show)
        }
        binding.btnHidePointer.setOnClickListener {
            sendUiCommand(PointerCommand.Hide)
        }
        binding.btnMoveCenter.setOnClickListener {
            val ok = PointerController.moveToCenterFromUi()
            if (!ok) {
                PointerController.updateStatus(getString(R.string.status_accessibility_unavailable))
            }
        }
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                PointerController.uiState.collect { state ->
                    binding.tvAccessibilityStatus.text = if (state.accessibilityEnabled) {
                        getString(R.string.accessibility_on)
                    } else {
                        getString(R.string.accessibility_off)
                    }
                    binding.tvSocketStatus.text = if (state.socketRunning) {
                        getString(R.string.socket_running)
                    } else {
                        getString(R.string.socket_stopped)
                    }
                    binding.tvPort.text = getString(R.string.port_value, state.port)
                    binding.tvLastCommand.text = getString(R.string.last_command_value, state.lastCommand)
                    binding.tvPointerCoord.text = getString(
                        R.string.pointer_coord_value,
                        state.pointerX,
                        state.pointerY,
                        state.pointerVisible
                    )
                    binding.tvStatusMessage.text = getString(R.string.status_message_value, state.statusMessage)
                }
            }
        }
    }

    private fun sendUiCommand(command: PointerCommand) {
        val ok = PointerController.dispatchCommand(command)
        if (!ok) {
            PointerController.updateStatus(getString(R.string.status_accessibility_unavailable))
        } else {
            PointerController.publishOverlayState()
        }
    }

    private fun startSocketService() {
        val intent = Intent(this, SocketForegroundService::class.java).apply {
            action = AppConstants.ACTION_START_SOCKET
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun stopSocketService() {
        val intent = Intent(this, SocketForegroundService::class.java).apply {
            action = AppConstants.ACTION_STOP_SOCKET
        }
        startService(intent)
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val expected = ComponentName(this, com.example.easypointer.service.MyAccessibilityService::class.java)
        val enabled = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val splitter = TextUtils.SimpleStringSplitter(':').apply { setString(enabled) }
        splitter.forEach {
            if (ComponentName.unflattenFromString(it) == expected) {
                return true
            }
        }
        return false
    }
}
