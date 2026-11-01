package com.artur.antskip.ui.updater

import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

sealed interface UpdateState {
    data object NotChecked : UpdateState
    data object Checking : UpdateState
    data class UpToDate(val latestVersion: String) : UpdateState
    data class UpdateAvailable(val latestVersion: String, val releaseUrl: String) : UpdateState
    data class CheckFailed(val message: String) : UpdateState
}

class GitHubUpdateChecker(private val context: Context) {
    private val mainHandler = Handler(Looper.getMainLooper())

    fun installedVersionName(): String =
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.31"
        } catch (_: PackageManager.NameNotFoundException) {
            "1.31"
        }

    fun checkForUpdates(onResult: (UpdateState) -> Unit) {
        onResult(UpdateState.Checking)
        val installed = installedVersionName()

        thread(name = "ant-skip-update-check") {
            val state = try {
                val release = fetchFromApi(installed) ?: fetchFromRedirect(installed)
                if (release != null && isNewerVersion(release.version, installed)) {
                    UpdateState.UpdateAvailable(release.version, release.url)
                } else if (release != null) {
                    UpdateState.UpToDate(release.version)
                } else {
                    UpdateState.CheckFailed("Nao foi possivel verificar")
                }
            } catch (e: Exception) {
                UpdateState.CheckFailed(e.message ?: "Falha na conexao")
            }
            mainHandler.post { onResult(state) }
        }
    }

    private fun fetchFromApi(installedVersion: String): ReleaseInfo? {
        val connection = (URL(API_URL).openConnection() as HttpURLConnection).apply {
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            setRequestProperty("User-Agent", "AntSKIP/$installedVersion")
            setRequestProperty("Accept", "application/vnd.github+json")
        }
        return try {
            if (connection.responseCode != 200) return null
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(body)
            val tagName = json.getString("tag_name").removePrefix("v").trim()
            val htmlUrl = json.getString("html_url")
            ReleaseInfo(tagName, htmlUrl)
        } finally {
            connection.disconnect()
        }
    }

    private fun fetchFromRedirect(installedVersion: String): ReleaseInfo? {
        val connection = (URL(LATEST_RELEASES_URL).openConnection() as HttpURLConnection).apply {
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            instanceFollowRedirects = false
            setRequestProperty("User-Agent", "AntSKIP/$installedVersion")
        }
        return try {
            val code = connection.responseCode
            if (code !in 300..399) return null
            val location = connection.getHeaderField("Location").orEmpty()
            val url = when {
                location.startsWith("http", ignoreCase = true) -> location
                location.startsWith("/") -> "https://github.com$location"
                else -> return null
            }
            val version = url.substringAfterLast('/').removePrefix("v")
            if (version.isBlank() || version == url) return null
            ReleaseInfo(version, url)
        } finally {
            connection.disconnect()
        }
    }

    private fun isNewerVersion(latest: String, installed: String): Boolean {
        val latestParts = latest.toVersionParts()
        val installedParts = installed.toVersionParts()
        val maxSize = maxOf(latestParts.size, installedParts.size)
        repeat(maxSize) { index ->
            val latestPart = latestParts.getOrElse(index) { 0 }
            val installedPart = installedParts.getOrElse(index) { 0 }
            if (latestPart != installedPart) return latestPart > installedPart
        }
        return false
    }

    private fun String.toVersionParts(): List<Int> =
        removePrefix("v")
            .split('.')
            .map { value -> value.takeWhile { it.isDigit() }.toIntOrNull() ?: 0 }

    private data class ReleaseInfo(val version: String, val url: String)

    companion object {
        const val API_URL = "https://api.github.com/repos/4NTx/AntSKIP/releases/latest"
        const val LATEST_RELEASES_URL = "https://github.com/4NTx/AntSKIP/releases/latest"
        private const val TIMEOUT_MS = 6_000
    }
}
