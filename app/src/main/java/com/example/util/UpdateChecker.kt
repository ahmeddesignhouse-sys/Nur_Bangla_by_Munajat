package com.example.util

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.BuildConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

data class AppUpdateInfo(
    val latestVersionCode: Int,
    val latestVersionName: String,
    val apkUrl: String,
    val changelog: String,
    val forceUpdate: Boolean
)

sealed class UpdateCheckResult {
    data class UpdateAvailable(val info: AppUpdateInfo) : UpdateCheckResult()
    data class UpToDate(val latestVersionCode: Int, val latestVersionName: String) : UpdateCheckResult()
    data object NoInternet : UpdateCheckResult()
    data class Error(val message: String) : UpdateCheckResult()
}

sealed class UpdateDialogState {
    data object Hidden : UpdateDialogState()
    data class Available(val info: AppUpdateInfo) : UpdateDialogState()
    data class Downloading(
        val info: AppUpdateInfo,
        val progressPercent: Int,
        val downloadedBytes: Long,
        val totalBytes: Long
    ) : UpdateDialogState()
    data class NeedsInstallPermission(val info: AppUpdateInfo, val apkFile: File) : UpdateDialogState()
    data class DownloadFailed(val info: AppUpdateInfo, val errorMessage: String) : UpdateDialogState()
}

object UpdateChecker {
    private const val TAG = "UpdateChecker"

    const val VERSION_JSON_URL =
        "https://raw.githubusercontent.com/ahmeddesignhouse-sys/nur-bangla-updates/main/version.json"

    const val DOWNLOAD_FOLDER_NAME = "Nur Bangla"

    fun isUpdateAvailable(
        latestVersionCode: Int,
        currentVersionCode: Int = BuildConfig.VERSION_CODE
    ): Boolean = latestVersionCode > currentVersionCode

    fun parseVersionJson(jsonText: String): AppUpdateInfo {
        val obj = JSONObject(jsonText)
        val versionCode = obj.optInt("latestVersionCode", 0)
        val versionName = obj.optString("latestVersionName", "").trim()
        val apkUrl = obj.optString("apkUrl", "").trim()
        val changelog = obj.optString("changelog", "").trim()
        val forceUpdate = obj.optBoolean("forceUpdate", false)
        require(versionCode > 0 && versionName.isNotBlank() && apkUrl.isNotBlank()) {
            "Invalid version.json payload"
        }
        return AppUpdateInfo(
            latestVersionCode = versionCode,
            latestVersionName = versionName,
            apkUrl = apkUrl,
            changelog = changelog,
            forceUpdate = forceUpdate
        )
    }

    fun isOnline(context: Context): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (_: Exception) {
            false
        }
    }

    suspend fun checkForUpdate(
        context: Context,
        currentVersionCode: Int = BuildConfig.VERSION_CODE
    ): UpdateCheckResult = withContext(Dispatchers.IO) {
        if (!isOnline(context)) {
            return@withContext UpdateCheckResult.NoInternet
        }
        try {
            val freshUrl = "$VERSION_JSON_URL?t=${System.currentTimeMillis()}"
            val conn = openConnectionWithRedirects(freshUrl, connectTimeoutMs = 10_000, readTimeoutMs = 12_000)
            try {
                val code = conn.responseCode
                if (code !in 200..299) {
                    return@withContext UpdateCheckResult.Error("HTTP $code")
                }
                val body = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                val info = parseVersionJson(body)
                if (isUpdateAvailable(info.latestVersionCode, currentVersionCode)) {
                    UpdateCheckResult.UpdateAvailable(info)
                } else {
                    UpdateCheckResult.UpToDate(info.latestVersionCode, info.latestVersionName)
                }
            } finally {
                conn.disconnect()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Update check failed: ${e.message}")
            if (!isOnline(context)) {
                UpdateCheckResult.NoInternet
            } else {
                UpdateCheckResult.Error(e.localizedMessage ?: "Network error")
            }
        }
    }

    suspend fun downloadApk(
        context: Context,
        apkUrl: String,
        versionName: String,
        onProgress: (percent: Int, downloadedBytes: Long, totalBytes: Long) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        val baseDownloads = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        val targetDir = if (baseDownloads != null) {
            File(baseDownloads, DOWNLOAD_FOLDER_NAME)
        } else {
            File(context.cacheDir, "updates")
        }
        if (!targetDir.exists()) {
            targetDir.mkdirs()
        }

        val safeVersion = versionName.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val finalFile = File(targetDir, "NurBangla-v$safeVersion.apk")
        val tempFile = File(targetDir, "NurBangla-v$safeVersion.apk.part")
        if (tempFile.exists()) tempFile.delete()

        var conn: HttpURLConnection? = null
        try {
            conn = openConnectionWithRedirects(apkUrl, connectTimeoutMs = 15_000, readTimeoutMs = 30_000)
            val code = conn.responseCode
            if (code !in 200..299) {
                return@withContext Result.failure(IllegalStateException("HTTP $code"))
            }

            val totalBytes = conn.contentLengthLong
            var downloadedBytes = 0L
            var lastReportedAt = 0L

            conn.inputStream.use { input ->
                FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(16 * 1024)
                    while (true) {
                        coroutineContext.ensureActive()
                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        downloadedBytes += read

                        val now = System.currentTimeMillis()
                        if (now - lastReportedAt >= 100L || (totalBytes > 0L && downloadedBytes >= totalBytes)) {
                            lastReportedAt = now
                            val pct = if (totalBytes > 0L) {
                                ((downloadedBytes * 100L) / totalBytes).toInt().coerceIn(0, 100)
                            } else {
                                0
                            }
                            withContext(Dispatchers.Main) {
                                onProgress(pct, downloadedBytes, totalBytes)
                            }
                        }
                    }
                    output.flush()
                }
            }

            if (finalFile.exists()) finalFile.delete()
            if (!tempFile.renameTo(finalFile)) {
                tempFile.copyTo(finalFile, overwrite = true)
                tempFile.delete()
            }

            withContext(Dispatchers.Main) {
                onProgress(100, downloadedBytes, if (totalBytes > 0L) totalBytes else downloadedBytes)
            }
            Result.success(finalFile)
        } catch (e: CancellationException) {
            if (tempFile.exists()) tempFile.delete()
            throw e
        } catch (e: Exception) {
            if (tempFile.exists()) tempFile.delete()
            Log.e(TAG, "APK download failed", e)
            Result.failure(e)
        } finally {
            conn?.disconnect()
        }
    }

    private fun openConnectionWithRedirects(
        startUrl: String,
        connectTimeoutMs: Int,
        readTimeoutMs: Int,
        maxRedirects: Int = 5
    ): HttpURLConnection {
        var currentUrl = startUrl
        repeat(maxRedirects) {
            val conn = (URL(currentUrl).openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = false
                requestMethod = "GET"
                connectTimeout = connectTimeoutMs
                readTimeout = readTimeoutMs
                useCaches = false
                setRequestProperty("Cache-Control", "no-cache")
                setRequestProperty("User-Agent", "NurBanglaAndroid/${BuildConfig.VERSION_NAME}")
            }
            val code = conn.responseCode
            if (code in listOf(
                    HttpURLConnection.HTTP_MOVED_PERM,
                    HttpURLConnection.HTTP_MOVED_TEMP,
                    HttpURLConnection.HTTP_SEE_OTHER,
                    307,
                    308
                )
            ) {
                val location = conn.getHeaderField("Location")
                conn.disconnect()
                if (location.isNullOrBlank()) {
                    throw IllegalStateException("Redirect missing Location header")
                }
                currentUrl = URL(URL(currentUrl), location).toString()
            } else {
                return conn
            }
        }
        throw IllegalStateException("Too many redirects while fetching update")
    }

    fun canInstallPackages(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    fun openUnknownSourcesSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    fun launchApkInstaller(context: Context, apkFile: File): Boolean {
        return try {
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch APK installer", e)
            false
        }
    }

    fun formatMegabytes(bytes: Long, isEnglish: Boolean): String {
        val mb = bytes.coerceAtLeast(0L).toDouble() / (1024.0 * 1024.0)
        val formatted = String.format(Locale.US, "%.1f", mb)
        return if (isEnglish) "$formatted MB" else "${BanglaUtils.toBanglaDigits(formatted)} মে.বা."
    }
}

@Composable
fun UpdateDialog(
    state: UpdateDialogState,
    onStartDownload: (AppUpdateInfo) -> Unit,
    onCancelDownload: (AppUpdateInfo) -> Unit,
    onDismiss: () -> Unit,
    onOpenInstallPermissionSettings: () -> Unit,
    onRetryInstall: (AppUpdateInfo, File) -> Unit
) {
    val isEn = isEnglishLang(LocalAppLanguage.current)
    val lifecycleOwner = LocalLifecycleOwner.current

    // When returning from Install Unknown Apps settings, automatically retry install if granted
    if (state is UpdateDialogState.NeedsInstallPermission) {
        DisposableEffect(lifecycleOwner, state.apkFile) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    onRetryInstall(state.info, state.apkFile)
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }
    }

    when (state) {
        is UpdateDialogState.Hidden -> Unit

        is UpdateDialogState.Available -> {
            val info = state.info
            val verLabel = if (isEn) info.latestVersionName else BanglaUtils.toBanglaDigits(info.latestVersionName)
            AlertDialog(
                onDismissRequest = { if (!info.forceUpdate) onDismiss() },
                properties = DialogProperties(
                    dismissOnBackPress = !info.forceUpdate,
                    dismissOnClickOutside = !info.forceUpdate
                ),
                icon = {
                    Icon(
                        imageVector = Icons.Default.SystemUpdate,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(30.dp)
                    )
                },
                title = {
                    Text(
                        text = tr(
                            isEn,
                            "ভার্সন $verLabel পাওয়া গেছে",
                            "Version $verLabel is available"
                        ),
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (info.forceUpdate) {
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = tr(
                                        isEn,
                                        "অ্যাপটি চালু রাখতে এই গুরুত্বপূর্ণ আপডেটটি ইনস্টল করা আবশ্যক।",
                                        "This important update is required to continue using the app."
                                    ),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }

                        Text(
                            text = tr(isEn, "নতুন যা যুক্ত হয়েছে:", "What's new:"),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = info.changelog.ifBlank {
                                tr(
                                    isEn,
                                    "নতুন সুবিধা ও কর্মক্ষমতা উন্নয়ন।",
                                    "New features and stability improvements."
                                )
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { onStartDownload(info) },
                        modifier = Modifier.testTag("update_now_button")
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(tr(isEn, "এখনই আপডেট করুন", "Update Now"))
                    }
                },
                dismissButton = if (!info.forceUpdate) {
                    {
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("update_later_button")
                        ) {
                            Text(tr(isEn, "পরে", "Later"))
                        }
                    }
                } else {
                    null
                },
                modifier = Modifier.testTag("update_available_dialog")
            )
        }

        is UpdateDialogState.Downloading -> {
            val info = state.info
            val pctText = "${BanglaUtils.formatNumber(state.progressPercent, isEn)}%"
            val downloadedStr = UpdateChecker.formatMegabytes(state.downloadedBytes, isEn)
            val sizeText = if (state.totalBytes > 0L) {
                "$downloadedStr / ${UpdateChecker.formatMegabytes(state.totalBytes, isEn)}"
            } else {
                downloadedStr
            }

            AlertDialog(
                onDismissRequest = {},
                properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
                icon = {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                title = {
                    Text(
                        text = tr(
                            isEn,
                            "আপডেট ডাউনলোড হচ্ছে...",
                            "Downloading Update..."
                        ),
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (state.totalBytes > 0L) {
                            LinearProgressIndicator(
                                progress = { (state.progressPercent / 100f).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                            )
                        } else {
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = sizeText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = pctText,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Text(
                            text = tr(
                                isEn,
                                "সেভ ফোল্ডার: Download/${UpdateChecker.DOWNLOAD_FOLDER_NAME}/",
                                "Saving to: Download/${UpdateChecker.DOWNLOAD_FOLDER_NAME}/"
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = { onCancelDownload(info) },
                        modifier = Modifier.testTag("update_cancel_button")
                    ) {
                        Text(tr(isEn, "বাতিল", "Cancel"))
                    }
                },
                modifier = Modifier.testTag("update_downloading_dialog")
            )
        }

        is UpdateDialogState.NeedsInstallPermission -> {
            AlertDialog(
                onDismissRequest = { if (!state.info.forceUpdate) onDismiss() },
                properties = DialogProperties(
                    dismissOnBackPress = !state.info.forceUpdate,
                    dismissOnClickOutside = !state.info.forceUpdate
                ),
                icon = {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                title = {
                    Text(
                        text = tr(
                            isEn,
                            "অ্যাপ ইনস্টলের অনুমতি প্রয়োজন",
                            "Install Permission Required"
                        ),
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = tr(
                            isEn,
                            "অনুগ্রহ করে নূর বাংলা অ্যাপের জন্য 'অজানা অ্যাপ ইনস্টল' (Install unknown apps) অনুমতি দিন, তারপর আবার 'ইনস্টল করুন' ট্যাপ করুন।",
                            "Please allow Install unknown apps for Nur Bangla, then tap Install again."
                        )
                    )
                },
                confirmButton = {
                    Button(
                        onClick = onOpenInstallPermissionSettings,
                        modifier = Modifier.testTag("update_open_install_settings")
                    ) {
                        Text(tr(isEn, "অনুমতি সেটিংস খুলুন", "Allow in Settings"))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { onRetryInstall(state.info, state.apkFile) },
                        modifier = Modifier.testTag("update_install_again_button")
                    ) {
                        Icon(Icons.Default.InstallMobile, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.size(4.dp))
                        Text(tr(isEn, "আবার ইনস্টল করুন", "Install Again"))
                    }
                },
                modifier = Modifier.testTag("update_permission_dialog")
            )
        }

        is UpdateDialogState.DownloadFailed -> {
            val info = state.info
            AlertDialog(
                onDismissRequest = { if (!info.forceUpdate) onDismiss() },
                properties = DialogProperties(
                    dismissOnBackPress = !info.forceUpdate,
                    dismissOnClickOutside = !info.forceUpdate
                ),
                icon = {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                title = {
                    Text(
                        text = tr(isEn, "ডাউনলোড ব্যর্থ হয়েছে", "Download Failed"),
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = tr(
                            isEn,
                            "আপডেট ফাইলটি ডাউনলোড করা যায়নি (${state.errorMessage})। আপনার ইন্টারনেট সংযোগ যাচাই করে পুনরায় চেষ্টা করুন।",
                            "Could not download the update (${state.errorMessage}). Please check your connection and try again."
                        )
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { onStartDownload(info) },
                        modifier = Modifier.testTag("update_retry_button")
                    ) {
                        Text(tr(isEn, "আবার চেষ্টা করুন", "Retry"))
                    }
                },
                dismissButton = if (!info.forceUpdate) {
                    {
                        TextButton(onClick = onDismiss) {
                            Text(tr(isEn, "বন্ধ করুন", "Close"))
                        }
                    }
                } else {
                    null
                },
                modifier = Modifier.testTag("update_failed_dialog")
            )
        }
    }
}
