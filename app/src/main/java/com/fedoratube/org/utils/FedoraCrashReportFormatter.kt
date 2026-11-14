package com.fedoratube.org.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

typealias FlowCrashBreadcrumb = FedoraCrashBreadcrumb
typealias FlowCrashReportSnapshot = FedoraCrashReportSnapshot
typealias FlowCrashReportFormatter = FedoraCrashReportFormatter

data class FedoraCrashBreadcrumb(
    val timestampMs: Long,
    val phase: String,
    val detail: String
)

data class FedoraCrashReportSnapshot(
    val timestampMs: Long,
    val threadName: String,
    val threadId: Long,
    val exceptionClass: String,
    val exceptionMessage: String?,
    val stackTrace: String,
    val deviceInfo: String,
    val memoryInfo: String,
    val breadcrumbs: List<FedoraCrashBreadcrumb>
)

object FedoraCrashReportFormatter {
    private const val TOP_FRAME_COUNT = 8
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    fun build(snapshot: FedoraCrashReportSnapshot): String = buildString {
        appendLine("=".repeat(60))
        appendLine("FEDORATUBE CRASH REPORT - ${dateFormat.format(Date(snapshot.timestampMs))}")
        appendLine("=".repeat(60))
        appendLine()
        appendLine("Crash Input")
        appendLine("Thread: ${snapshot.threadName} (id=${snapshot.threadId})")
        appendLine("Exception: ${snapshot.exceptionClass}")
        appendLine("Message: ${snapshot.exceptionMessage ?: "none"}")
        appendLine("OutOfMemoryError: ${snapshot.exceptionClass == OutOfMemoryError::class.java.name}")
        appendLine()
        appendLine("Device Info:")
        appendLine(snapshot.deviceInfo)
        appendLine()
        appendLine("Memory State:")
        appendLine(snapshot.memoryInfo)
        appendLine()
        appendLine("State/Cache Updates Before Crash")
        appendBreadcrumbs(snapshot.breadcrumbs)
        appendLine()
        appendLine("Final Output")
        appendLine("Top Stack Frames (same build):")
        topStackFrames(snapshot.stackTrace).forEach { appendLine(it) }
        appendLine()
        appendLine("Full Stack Trace:")
        appendLine(snapshot.stackTrace)
    }

    fun topStackFrames(stackTrace: String, limit: Int = TOP_FRAME_COUNT): List<String> =
        stackTrace
            .lineSequence()
            .map { it.trim() }
            .filter { it.startsWith("at ") || it.startsWith("Caused by:") }
            .take(limit)
            .toList()

    private fun StringBuilder.appendBreadcrumbs(breadcrumbs: List<FlowCrashBreadcrumb>) {
        if (breadcrumbs.isEmpty()) {
            appendLine("No lifecycle or state breadcrumbs recorded.")
            return
        }

        breadcrumbs.forEach { entry ->
            appendLine("${timeFormat.format(Date(entry.timestampMs))} ${entry.phase}: ${entry.detail}")
        }
    }
}
