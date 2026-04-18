package com.example.easypointer.socket

import android.util.Log
import com.example.easypointer.app.AppConstants
import com.example.easypointer.model.PointerCommand
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Simple line-based TCP server.
 * Accepts multiple connections serially.
 */
class SocketServer(
    private val port: Int,
    private val listener: Listener
) {

    interface Listener {
        fun onServerStarted(port: Int)
        fun onServerStopped()
        fun onLineReceived(line: String)
        fun onCommand(command: PointerCommand): String?
        fun onError(message: String, t: Throwable? = null)
    }

    private val running = AtomicBoolean(false)
    @Volatile
    private var serverSocket: ServerSocket? = null
    @Volatile
    private var workerThread: Thread? = null

    fun start() {
        if (!running.compareAndSet(false, true)) return

        workerThread = Thread({
            try {
                ServerSocket(port).also { serverSocket = it }
                listener.onServerStarted(port)
                while (running.get()) {
                    val client = try {
                        serverSocket?.accept()
                    } catch (t: Throwable) {
                        if (running.get()) {
                            listener.onError("accept_failed", t)
                        }
                        null
                    }
                    if (client != null) {
                        handleClient(client)
                    }
                }
            } catch (t: Throwable) {
                listener.onError("server_start_failed", t)
            } finally {
                safeClose()
                running.set(false)
                listener.onServerStopped()
            }
        }, "socket-server-thread").apply { start() }
    }

    fun stop() {
        if (!running.compareAndSet(true, false)) return
        safeClose()
        workerThread?.interrupt()
        workerThread = null
    }

    fun isRunning(): Boolean = running.get()

    private fun handleClient(socket: Socket) {
        socket.use { client ->
            val input = BufferedReader(InputStreamReader(client.getInputStream()))
            val output = BufferedWriter(OutputStreamWriter(client.getOutputStream()))
            while (running.get()) {
                val line = try {
                    input.readLine()
                } catch (t: Throwable) {
                    listener.onError("client_read_failed", t)
                    null
                }
                if (line == null) break

                listener.onLineReceived(line)
                val result = CommandParser.parse(line)
                val response = when {
                    result.error != null -> "ERROR ${result.error}"
                    result.command == PointerCommand.Ping -> "PONG"
                    result.command != null -> listener.onCommand(result.command)
                    else -> "ERROR unknown"
                }
                if (response != null) {
                    try {
                        output.write(response)
                        output.newLine()
                        output.flush()
                    } catch (t: Throwable) {
                        listener.onError("client_write_failed", t)
                        break
                    }
                }
            }
        }
    }

    private fun safeClose() {
        try {
            serverSocket?.close()
        } catch (t: Throwable) {
            Log.w(AppConstants.TAG_APP, "Ignored server close error", t)
        } finally {
            serverSocket = null
        }
    }
}
