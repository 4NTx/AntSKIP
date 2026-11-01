package com.artur.antskip.data

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.ArrayDeque

object DiagnosticLogger {
    private const val MAX_LOGS = 80
    private val buffer = ArrayDeque<String>(MAX_LOGS)
    private val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")

    @Synchronized
    fun log(message: String) {
        val timestamp = LocalDateTime.now().format(formatter)
        if (buffer.size >= MAX_LOGS) {
            buffer.removeFirst()
        }
        buffer.addLast("[$timestamp] $message")
    }

    @Synchronized
    fun getLogs(): String =
        if (buffer.isEmpty()) "" else buffer.joinToString("\n")

    @Synchronized
    fun clear() {
        buffer.clear()
    }
}
