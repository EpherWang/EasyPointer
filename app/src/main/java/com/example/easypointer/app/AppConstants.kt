package com.example.easypointer.app

/**
 * Centralized constants to avoid scattering magic values.
 */
object AppConstants {
    const val TAG_APP = "EasyPointer"

    const val SOCKET_PORT = 34567
    const val SOCKET_TIMEOUT_MS = 0

    const val NOTIFICATION_CHANNEL_ID = "easy_pointer_socket_channel"
    const val NOTIFICATION_CHANNEL_NAME = "EasyPointer Socket Service"
    const val NOTIFICATION_ID = 101

    const val ACTION_START_SOCKET = "com.example.easypointer.action.START_SOCKET"
    const val ACTION_STOP_SOCKET = "com.example.easypointer.action.STOP_SOCKET"

    const val DEFAULT_POINTER_SIZE_DP = 16
    const val DEFAULT_POINTER_COLOR = 0xFFFF2D55.toInt()

    const val ERROR_SERVICE_UNAVAILABLE = "accessibility_service_unavailable"
}
