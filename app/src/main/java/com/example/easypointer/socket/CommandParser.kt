package com.example.easypointer.socket

import com.example.easypointer.model.PointerCommand

/**
 * Parses line-based socket commands.
 */
object CommandParser {
    data class ParseResult(
        val command: PointerCommand? = null,
        val error: String? = null
    )

    fun parse(rawInput: String): ParseResult {
        val text = rawInput.trim()
        if (text.isEmpty()) {
            return ParseResult(error = "empty_command")
        }

        val parts = text.split(Regex("\\s+"))
        val op = parts[0].uppercase()

        return when (op) {
            "SHOW" -> {
                if (parts.size == 1) ParseResult(command = PointerCommand.Show)
                else ParseResult(error = "SHOW_takes_no_args")
            }

            "HIDE" -> {
                if (parts.size == 1) ParseResult(command = PointerCommand.Hide)
                else ParseResult(error = "HIDE_takes_no_args")
            }

            "TOGGLE" -> {
                if (parts.size == 1) ParseResult(command = PointerCommand.Toggle)
                else ParseResult(error = "TOGGLE_takes_no_args")
            }

            "PING" -> {
                if (parts.size == 1) ParseResult(command = PointerCommand.Ping)
                else ParseResult(error = "PING_takes_no_args")
            }

            "MOVE" -> parse2Int(parts, "MOVE") { x, y -> PointerCommand.Move(x, y) }
            "OFFSET" -> parse2Int(parts, "OFFSET") { x, y -> PointerCommand.Offset(x, y) }
            else -> ParseResult(error = "unknown_command")
        }
    }

    private fun parse2Int(
        parts: List<String>,
        name: String,
        factory: (Int, Int) -> PointerCommand
    ): ParseResult {
        if (parts.size != 3) {
            return ParseResult(error = "${name}_requires_2_int_args")
        }

        val first = parts[1].toIntOrNull() ?: return ParseResult(error = "${name}_arg1_not_int")
        val second = parts[2].toIntOrNull() ?: return ParseResult(error = "${name}_arg2_not_int")
        return ParseResult(command = factory(first, second))
    }
}
