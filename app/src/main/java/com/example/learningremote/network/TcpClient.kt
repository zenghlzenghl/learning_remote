package com.example.learningremote.network

import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket

object TcpClient {
    private const val TAG = "TcpClient"
    private const val PORT = 8888

    private var socket: Socket? = null
    private var writer: PrintWriter? = null
    private var reader: BufferedReader? = null
    private var isRunning = false

    var onMessageReceived: ((String) -> Unit)? = null
    var onConnectionChanged: ((Boolean) -> Unit)? = null

    fun connect(ip: String): Boolean {
        return try {
            socket = Socket(ip, PORT)
            writer = PrintWriter(socket?.getOutputStream(), true)
            reader = BufferedReader(InputStreamReader(socket?.getInputStream()))
            isRunning = true

            Thread {
                try {
                    while (isRunning && socket?.isConnected == true) {
                        val line = reader?.readLine()
                        if (line != null) {
                            Log.d(TAG, "Received: $line")
                            onMessageReceived?.invoke(line)
                        } else {
                            break
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Read error: ${e.message}")
                }
                disconnect()
            }.start()

            onConnectionChanged?.invoke(true)
            Log.d(TAG, "Connected to $ip:$PORT")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Connection failed: ${e.message}")
            onConnectionChanged?.invoke(false)
            false
        }
    }

    fun send(command: String): Boolean {
        return try {
            writer?.println(command)
            writer?.flush()
            Log.d(TAG, "Sent: $command")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Send error: ${e.message}")
            false
        }
    }

    fun disconnect() {
        isRunning = false
        try {
            writer?.close()
            reader?.close()
            socket?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Disconnect error: ${e.message}")
        }
        socket = null
        writer = null
        reader = null
        onConnectionChanged?.invoke(false)
        Log.d(TAG, "Disconnected")
    }

    fun isConnected(): Boolean {
        return socket?.isConnected == true && !socket?.isClosed!!
    }
}