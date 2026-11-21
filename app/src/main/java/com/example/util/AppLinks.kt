package com.example.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

/**
 * Single source of truth for Nur Bangla's public download URL.
 * Swapping this to the Google Play Store URL in a future release is a one-line change.
 */
object AppLinks {
    const val APP_DOWNLOAD_URL: String =
        "https://github.com/ahmeddesignhouse-sys/nur-bangla-updates/releases/tag/v1.8"

    fun shareCaptionFooter(isEnglish: Boolean): String = if (isEnglish) {
        "Download Nur Bangla: $APP_DOWNLOAD_URL"
    } else {
        "নূর বাংলা ডাউনলোড করুন: $APP_DOWNLOAD_URL"
    }

    fun openDownloadPage(context: Context): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(APP_DOWNLOAD_URL)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun copyDownloadLink(context: Context, showToast: Boolean = true) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText("Nur Bangla Download Link", APP_DOWNLOAD_URL))
        if (showToast) {
            Toast.makeText(
                context,
                "লিংক কপি হয়েছে / Link copied",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}
