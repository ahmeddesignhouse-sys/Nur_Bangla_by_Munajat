package com.example.data.repository

import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.URLEncoder
import java.net.UnknownHostException
import java.util.Collections
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.cos

/**
 * Exact diagnostic record for a single free place-data source attempt (Overpass mirror or Nominatim).
 */
data class SourceAttemptDiagnostic(
    val sourceName: String,
    val httpStatus: Int? = null,
    val elapsedMs: Long = 0L,
    val errorKind: String,
    val detail: String = ""
) {
    fun formatLine(isEnglish: Boolean): String {
        val sec = String.format(Locale.US, "%.1fs", elapsedMs / 1000.0)
        val statusPart = when {
            httpStatus != null && httpStatus !in 200..299 -> "HTTP $httpStatus"
            errorKind.isNotBlank() -> errorKind
            else -> if (isEnglish) "failed" else "ব্যর্থ"
        }
        return "• $sourceName — $statusPart ($sec)"
    }
}

sealed class FreePlacesFetchOutcome {
    data class OverpassSuccess(
        val body: String,
        val mirrorHost: String,
        val elapsedMs: Long,
        val diagnostics: List<SourceAttemptDiagnostic>
    ) : FreePlacesFetchOutcome()

    data class NominatimSuccess(
        val body: String,
        val elapsedMs: Long,
        val diagnostics: List<SourceAttemptDiagnostic>
    ) : FreePlacesFetchOutcome()

    data class AllFailed(
        val diagnostics: List<SourceAttemptDiagnostic>,
        val timedOutOverall: Boolean,
        val lastException: Exception?
    ) : FreePlacesFetchOutcome()
}

/**
 * 100% FREE OpenStreetMap search engine (ZERO API key, ZERO billing, ZERO payment card):
 * 1. Tries 5 free Overpass mirrors in strict priority order via HTTP POST (`data=...`),
 *    stopping immediately at the FIRST mirror that answers with valid JSON.
 * 2. Falls back to free OpenStreetMap Nominatim (`nominatim.openstreetmap.org`) respecting
 *    `User-Agent: NurBanglaAndroid/1.8` and max 1 request/second rate limiting.
 * 3. Enforces a TRUE non-blocking hard 15-second overall deadline (`TOTAL_SEARCH_DEADLINE_MS`):
 *    uses a detached supervised IO scope + socket disconnect on timeout so blocking JVM
 *    `HttpURLConnection` calls can NEVER hold the caller past 15 seconds.
 * 4. Captures exact per-source diagnostics (host, HTTP status, elapsed seconds, error type).
 */
object FreePlacesNetworkEngine {

    private const val TAG = "FreePlacesEngine"
    const val USER_AGENT = "NurBanglaAndroid/1.8"
    const val CONNECT_TIMEOUT_MS = 4_500
    const val READ_TIMEOUT_MS = 9_000
    const val TOTAL_SEARCH_DEADLINE_MS = 15_000L
    private const val STAGGER_WINDOW_MS = 1_200L
    private const val NOMINATIM_MIN_INTERVAL_MS = 1_100L

    /**
     * Five FREE Overpass mirrors tried in priority order.
     */
    val OVERPASS_MIRRORS: List<String> = listOf(
        "https://overpass-api.de/api/interpreter",
        "https://overpass.kumi.systems/api/interpreter",
        "https://overpass.private.coffee/api/interpreter",
        "https://overpass.nchc.org.tw/api/interpreter",
        "https://maps.mail.ru/osm/tools/overpass/api/interpreter"
    )

    private val lastNominatimCallMs = AtomicLong(0L)

    class HttpStatusException(val statusCode: Int) : IllegalStateException("HTTP $statusCode")

    fun buildNominatimSearchUrl(
        query: String,
        lat: Double,
        lon: Double,
        radiusMeters: Int
    ): String {
        val safeRadiusKm = (radiusMeters.coerceIn(500, 25_000)) / 1000.0
        val latDelta = safeRadiusKm / 111.32
        val cosLat = cos(Math.toRadians(lat)).coerceAtLeast(0.15)
        val lonDelta = safeRadiusKm / (111.32 * cosLat)

        val west = lon - lonDelta
        val east = lon + lonDelta
        val north = lat + latDelta
        val south = lat - latDelta

        val encodedQ = URLEncoder.encode(query, "UTF-8")
        val viewbox = String.format(Locale.US, "%.5f,%.5f,%.5f,%.5f", west, north, east, south)
        val latStr = String.format(Locale.US, "%.5f", lat)
        val lonStr = String.format(Locale.US, "%.5f", lon)

        return "https://nominatim.openstreetmap.org/search" +
            "?q=$encodedQ&format=json&lat=$latStr&lon=$lonStr&bounded=1&viewbox=$viewbox&addressdetails=1&limit=40"
    }

    fun validateOverpassJsonBody(body: String) {
        val trimmed = body.trim()
        if (!trimmed.startsWith("{")) {
            throw IllegalStateException("non-JSON response")
        }
        val root = JSONObject(trimmed)
        val elements = root.optJSONArray("elements")
            ?: throw IllegalStateException("missing elements array")
        val remark = root.optString("remark", "").trim()
        if (elements.length() == 0 && remark.isNotBlank() &&
            (remark.contains("timeout", ignoreCase = true) ||
                remark.contains("error", ignoreCase = true) ||
                remark.contains("memory", ignoreCase = true))
        ) {
            throw IllegalStateException("Overpass remark: $remark")
        }
    }

    fun validateNominatimJsonBody(body: String): Int {
        val trimmed = body.trim()
        if (!trimmed.startsWith("[")) {
            throw IllegalStateException("non-JSON array response")
        }
        val arr = JSONArray(trimmed)
        return arr.length()
    }

    /**
     * Runs the 5 Overpass mirrors in priority order, followed by Nominatim fallback if needed,
     * returning strictly within [totalDeadlineMs] (default 15 seconds) even if remote sockets hang.
     */
    suspend fun fetchOverpassThenNominatim(
        overpassQuery: String,
        nominatimQuery: String,
        lat: Double,
        lon: Double,
        radiusMeters: Int,
        totalDeadlineMs: Long = TOTAL_SEARCH_DEADLINE_MS,
        overpassMirrors: List<String> = OVERPASS_MIRRORS
    ): FreePlacesFetchOutcome = withContext(Dispatchers.IO) {
        val overallStartMs = System.currentTimeMillis()
        val deadlineAtMs = overallStartMs + totalDeadlineMs
        val diagnostics = Collections.synchronizedList(mutableListOf<SourceAttemptDiagnostic>())
        val nominatimUrl = buildNominatimSearchUrl(nominatimQuery, lat, lon, radiusMeters)

        val inFlightStarts = Collections.synchronizedMap(LinkedHashMap<String, Long>())
        val activeConnections = Collections.synchronizedList(mutableListOf<HttpURLConnection>())
        val lastException = AtomicReference<Exception?>(null)

        val winner = CompletableDeferred<FreePlacesFetchOutcome>()
        val finishedCount = AtomicInteger(0)
        val totalSources = overpassMirrors.size + 1
        val hasWinner = AtomicBoolean(false)

        // Detached supervised scope so withTimeoutOrNull returns IMMEDIATELY at deadline
        // without waiting on uninterruptible blocking HttpURLConnection calls.
        val workerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        fun completeIfAllDone() {
            if (finishedCount.incrementAndGet() >= totalSources && !hasWinner.get()) {
                winner.complete(
                    FreePlacesFetchOutcome.AllFailed(
                        diagnostics = synchronized(diagnostics) { diagnostics.toList() },
                        timedOutOverall = false,
                        lastException = lastException.get()
                    )
                )
            }
        }

        fun remainingBudgetMs(): Int {
            return (deadlineAtMs - System.currentTimeMillis()).coerceAtLeast(500L).toInt()
        }

        workerScope.launch {
            for ((index, endpoint) in overpassMirrors.withIndex()) {
                if (hasWinner.get() || System.currentTimeMillis() >= deadlineAtMs) return@launch
                val host = runCatching { URL(endpoint).host }.getOrDefault(endpoint)
                val fastFailSignal = Channel<Unit>(Channel.CONFLATED)

                workerScope.launch {
                    val startedMs = System.currentTimeMillis()
                    inFlightStarts[host] = startedMs
                    runCatching {
                        Log.d(TAG, "Overpass POST #${index + 1}/${overpassMirrors.size} -> $host")
                    }
                    try {
                        val budget = remainingBudgetMs()
                        val body = postOverpassQueryCancellable(
                            endpointUrl = endpoint,
                            query = overpassQuery,
                            connectTimeoutMs = minOf(CONNECT_TIMEOUT_MS, budget),
                            readTimeoutMs = minOf(READ_TIMEOUT_MS, budget),
                            activeConnections = activeConnections
                        )
                        validateOverpassJsonBody(body)
                        val elapsedMs = System.currentTimeMillis() - startedMs
                        inFlightStarts.remove(host)
                        runCatching {
                            Log.i(TAG, "Overpass mirror answered: $host in ${elapsedMs}ms (${body.length} chars)")
                        }
                        if (hasWinner.compareAndSet(false, true)) {
                            winner.complete(
                                FreePlacesFetchOutcome.OverpassSuccess(
                                    body = body,
                                    mirrorHost = host,
                                    elapsedMs = elapsedMs,
                                    diagnostics = synchronized(diagnostics) { diagnostics.toList() }
                                )
                            )
                        }
                    } catch (ce: CancellationException) {
                        throw ce
                    } catch (e: Exception) {
                        inFlightStarts.remove(host)
                        lastException.set(e)
                        val elapsedMs = System.currentTimeMillis() - startedMs
                        val diag = classifyFailure(host, elapsedMs, e)
                        diagnostics.add(diag)
                        runCatching {
                            Log.w(TAG, "Overpass mirror $host failed: ${diag.errorKind} (${elapsedMs}ms)")
                        }
                        fastFailSignal.trySend(Unit)
                    } finally {
                        completeIfAllDone()
                    }
                }

                // Wait up to STAGGER_WINDOW_MS (1.2s) or until this mirror fails fast
                val waitWindow = minOf(STAGGER_WINDOW_MS, remainingBudgetMs().toLong())
                if (waitWindow > 0L) {
                    withTimeoutOrNull(waitWindow) {
                        fastFailSignal.receive()
                    }
                }
            }

            // Fallback #6: OpenStreetMap Nominatim (starts promptly ~6s or sooner if mirrors fail fast)
            if (!hasWinner.get() && System.currentTimeMillis() < deadlineAtMs) {
                val nomHost = "nominatim.openstreetmap.org"
                workerScope.launch {
                    val startedMs = System.currentTimeMillis()
                    inFlightStarts[nomHost] = startedMs
                    try {
                        enforceNominatimRateLimit()
                        runCatching { Log.d(TAG, "Nominatim fallback GET -> $nominatimUrl") }
                        val budget = remainingBudgetMs()
                        val body = getUrlCancellable(
                            endpointUrl = nominatimUrl,
                            connectTimeoutMs = minOf(CONNECT_TIMEOUT_MS, budget),
                            readTimeoutMs = minOf(READ_TIMEOUT_MS, budget),
                            activeConnections = activeConnections
                        )
                        val count = validateNominatimJsonBody(body)
                        val elapsedMs = System.currentTimeMillis() - startedMs
                        inFlightStarts.remove(nomHost)
                        if (count == 0) {
                            diagnostics.add(
                                SourceAttemptDiagnostic(
                                    sourceName = nomHost,
                                    httpStatus = 200,
                                    elapsedMs = elapsedMs,
                                    errorKind = "empty result"
                                )
                            )
                        } else {
                            runCatching {
                                Log.i(TAG, "Nominatim fallback answered: $count items in ${elapsedMs}ms")
                            }
                            if (hasWinner.compareAndSet(false, true)) {
                                winner.complete(
                                    FreePlacesFetchOutcome.NominatimSuccess(
                                        body = body,
                                        elapsedMs = elapsedMs,
                                        diagnostics = synchronized(diagnostics) { diagnostics.toList() }
                                    )
                                )
                            }
                        }
                    } catch (ce: CancellationException) {
                        throw ce
                    } catch (e: Exception) {
                        inFlightStarts.remove(nomHost)
                        lastException.set(e)
                        val elapsedMs = System.currentTimeMillis() - startedMs
                        val diag = classifyFailure(nomHost, elapsedMs, e)
                        diagnostics.add(diag)
                        runCatching {
                            Log.w(TAG, "Nominatim fallback failed: ${diag.errorKind} (${elapsedMs}ms)")
                        }
                    } finally {
                        completeIfAllDone()
                    }
                }
            }
        }

        val outcome = try {
            withTimeoutOrNull(totalDeadlineMs) {
                winner.await()
            }
        } finally {
            // Immediately tear down remaining connections and cancel background workers
            workerScope.cancel()
            val conns = synchronized(activeConnections) { activeConnections.toList() }
            conns.forEach { conn -> runCatching { conn.disconnect() } }
        }

        if (outcome != null) {
            return@withContext outcome
        }

        // Global hard limit reached: record timeout for any still-in-flight sources
        val nowMs = System.currentTimeMillis()
        val finalDiags = synchronized(diagnostics) { diagnostics.toMutableList() }
        synchronized(inFlightStarts) {
            inFlightStarts.forEach { (host, startMs) ->
                if (finalDiags.none { it.sourceName == host }) {
                    finalDiags.add(
                        SourceAttemptDiagnostic(
                            sourceName = host,
                            httpStatus = null,
                            elapsedMs = (nowMs - startMs).coerceAtLeast(100L),
                            errorKind = "timeout"
                        )
                    )
                }
            }
        }
        val totalSec = String.format(Locale.US, "%.1fs", (nowMs - overallStartMs) / 1000.0)
        runCatching {
            Log.w(TAG, "Hard deadline ($totalSec) reached. Diagnostics: ${finalDiags.joinToString("; ") { it.formatLine(true) }}")
        }

        FreePlacesFetchOutcome.AllFailed(
            diagnostics = finalDiags,
            timedOutOverall = true,
            lastException = lastException.get()
        )
    }

    /**
     * Respects Nominatim's 1 request/second usage policy across concurrent/repeated calls.
     */
    private suspend fun enforceNominatimRateLimit() {
        while (true) {
            val now = System.currentTimeMillis()
            val prev = lastNominatimCallMs.get()
            val waitMs = (prev + NOMINATIM_MIN_INTERVAL_MS) - now
            if (waitMs <= 0L) {
                if (lastNominatimCallMs.compareAndSet(prev, now)) {
                    return
                }
            } else {
                delay(waitMs)
            }
        }
    }

    internal fun classifyFailure(
        sourceName: String,
        elapsedMs: Long,
        e: Exception
    ): SourceAttemptDiagnostic {
        return when (e) {
            is HttpStatusException -> SourceAttemptDiagnostic(
                sourceName = sourceName,
                httpStatus = e.statusCode,
                elapsedMs = elapsedMs,
                errorKind = "HTTP ${e.statusCode}",
                detail = e.message.orEmpty()
            )
            is SocketTimeoutException -> SourceAttemptDiagnostic(
                sourceName = sourceName,
                httpStatus = null,
                elapsedMs = elapsedMs,
                errorKind = "timeout",
                detail = e.message.orEmpty()
            )
            is UnknownHostException -> SourceAttemptDiagnostic(
                sourceName = sourceName,
                httpStatus = null,
                elapsedMs = elapsedMs,
                errorKind = "no internet",
                detail = e.message.orEmpty()
            )
            else -> {
                val msg = e.message.orEmpty()
                val kind = when {
                    msg.contains("timed out", ignoreCase = true) || msg.contains("timeout", ignoreCase = true) -> "timeout"
                    msg.contains("non-JSON", ignoreCase = true) || msg.contains("elements", ignoreCase = true) -> "invalid response"
                    else -> e.javaClass.simpleName
                }
                SourceAttemptDiagnostic(
                    sourceName = sourceName,
                    httpStatus = null,
                    elapsedMs = elapsedMs,
                    errorKind = kind,
                    detail = msg
                )
            }
        }
    }

    private suspend fun postOverpassQueryCancellable(
        endpointUrl: String,
        query: String,
        connectTimeoutMs: Int,
        readTimeoutMs: Int,
        activeConnections: MutableList<HttpURLConnection>
    ): String {
        val conn = (URL(endpointUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = connectTimeoutMs.coerceAtLeast(500)
            readTimeout = readTimeoutMs.coerceAtLeast(500)
            doOutput = true
            useCaches = false
            setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Accept-Encoding", "identity")
            setRequestProperty("User-Agent", USER_AGENT)
        }
        activeConnections.add(conn)

        val job = currentCoroutineContext()[Job]
        val cancelHandle = job?.invokeOnCompletion { cause ->
            if (cause != null) {
                runCatching { conn.disconnect() }
            }
        }

        return try {
            currentCoroutineContext().ensureActive()
            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
                writer.write("data=" + URLEncoder.encode(query, "UTF-8"))
                writer.flush()
            }
            currentCoroutineContext().ensureActive()
            val code = conn.responseCode
            if (code !in 200..299) {
                throw HttpStatusException(code)
            }
            conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            cancelHandle?.dispose()
            activeConnections.remove(conn)
            runCatching { conn.disconnect() }
        }
    }

    private suspend fun getUrlCancellable(
        endpointUrl: String,
        connectTimeoutMs: Int,
        readTimeoutMs: Int,
        activeConnections: MutableList<HttpURLConnection>
    ): String {
        val conn = (URL(endpointUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = connectTimeoutMs.coerceAtLeast(500)
            readTimeout = readTimeoutMs.coerceAtLeast(500)
            useCaches = false
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Accept-Encoding", "identity")
            setRequestProperty("User-Agent", USER_AGENT)
        }
        activeConnections.add(conn)

        val job = currentCoroutineContext()[Job]
        val cancelHandle = job?.invokeOnCompletion { cause ->
            if (cause != null) {
                runCatching { conn.disconnect() }
            }
        }

        return try {
            currentCoroutineContext().ensureActive()
            val code = conn.responseCode
            if (code !in 200..299) {
                throw HttpStatusException(code)
            }
            conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            cancelHandle?.dispose()
            activeConnections.remove(conn)
            runCatching { conn.disconnect() }
        }
    }
}
