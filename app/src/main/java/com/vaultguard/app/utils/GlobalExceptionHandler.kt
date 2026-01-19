package com.vaultguard.app.utils

import android.content.Context
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * A centralized place to catch ANY crash in the application. This is "Number 5" (Error Handling) in
 * Enterprise Architecture.
 */
class GlobalExceptionHandler(
        private val context: Context,
        private val defaultHandler: Thread.UncaughtExceptionHandler?
) : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        // 1. Capture the Crash Details
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        val crashLog =
                """
            |$timestamp
            |CRASH Detected on Thread: ${thread.name}
            |Exception: ${throwable.message}
            |Stacktrace:
            |${android.util.Log.getStackTraceString(throwable)}
            |-------------------------------------------
        """.trimMargin()

        // 2. Log to Console (for Dev only)
        Log.e("GlobalErrorHandler", "🔥 FATAL CRASH CAUGHT: $crashLog")

        // 3. Save to SharedPreferences (Invisible to User, Pending Upload) ☁️
        // We use the application context to get the default prefs
        try {
            val prefs = context.getSharedPreferences("crash_buffer", Context.MODE_PRIVATE)
            prefs.edit().putString("pending_crash", crashLog).apply()
            Log.d("GlobalErrorHandler", "✅ Crash buffered to Prefs for next-launch upload.")
        } catch (e: Exception) {
            Log.e("GlobalErrorHandler", "Failed to buffer crash", e)
        }

        // 4. Delegate to Android System
        defaultHandler?.uncaughtException(thread, throwable)
    }
}
