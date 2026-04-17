package com.example.easypointer.model

/**
 * Socket command model.
 */
sealed class PointerCommand {
    data object Show : PointerCommand()
    data object Hide : PointerCommand()
    data class Move(val x: Int, val y: Int) : PointerCommand()
    data class Offset(val dx: Int, val dy: Int) : PointerCommand()
    data object Toggle : PointerCommand()
    data object Ping : PointerCommand()
}
