package com.vaultguard.app.utils

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PersistableBundle

class ClipboardManager(private val context: Context) {

    private val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    private val handler = Handler(Looper.getMainLooper())
    private val CLEAR_DELAY_MS = 45000L // 45 seconds

    fun copyToClipboard(label: String, text: String) {
        val clip = ClipData.newPlainText(label, text)

        // Mark as sensitive content for Android 13+ (API 33)
        // This prevents the clipboard overlay from showing the sensitive content unmasked
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val extras = PersistableBundle().apply {
                putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
            }
            clip.description.extras = extras
        }

        clipboard.setPrimaryClip(clip)

        // Auto-clear logic
        scheduleClear()
    }

    private fun scheduleClear() {
        // Cancel any previous clear tasks to reset timer
        handler.removeCallbacksAndMessages(null)
        
        handler.postDelayed({
            clearClipboard()
        }, CLEAR_DELAY_MS)
    }

    private fun clearClipboard() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            clipboard.clearPrimaryClip()
        } else {
            clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
        }
    }
}
