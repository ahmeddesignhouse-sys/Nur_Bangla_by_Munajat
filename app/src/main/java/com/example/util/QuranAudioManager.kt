package com.example.util

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.data.model.Surah
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.Locale

enum class QuranReciter(
    val id: String,
    val everyAyahFolder: String,
    val nameBn: String,
    val nameEn: String,
    val styleBn: String,
    val styleEn: String
) {
    ALAFASY(
        id = "alafasy",
        everyAyahFolder = "Alafasy_128kbps",
        nameBn = "মিশারী রশিদ আল-আফাসী",
        nameEn = "Mishary Rashid Alafasy",
        styleBn = "মুরাত্তাল • ১২৮ কেবিপিএস",
        styleEn = "Murattal • 128 kbps"
    ),
    ABDUL_BASIT(
        id = "abdul_basit",
        everyAyahFolder = "Abdul_Basit_Murattal_192kbps",
        nameBn = "আব্দুল বাসিত আব্দুস সামাদ",
        nameEn = "Abdul Basit (Murattal)",
        styleBn = "মুরাত্তাল • ১৯২ কেবিপিএস",
        styleEn = "Murattal • 192 kbps"
    ),
    SUDAIS(
        id = "sudais",
        everyAyahFolder = "Abdurrahmaan_As-Sudais_192kbps",
        nameBn = "আব্দুর রহমান আস-সুদাইস",
        nameEn = "Abdur-Rahman As-Sudais",
        styleBn = "হারামাইন • ১৯২ কেবিপিএস",
        styleEn = "Haramain • 192 kbps"
    ),
    HUSARY(
        id = "husary",
        everyAyahFolder = "Husary_128kbps",
        nameBn = "মাহমুদ খলিল আল-হুসারী",
        nameEn = "Mahmoud Khalil Al-Husary",
        styleBn = "তাজবিদ • ১২৮ কেবিপিএস",
        styleEn = "Tajweed • 128 kbps"
    );

    fun displayName(isEnglish: Boolean): String = if (isEnglish) nameEn else nameBn
    fun style(isEnglish: Boolean): String = if (isEnglish) styleEn else styleBn

    companion object {
        const val AUDIO_SOURCE_BASE_URL = "https://everyayah.com/data"

        fun fromId(id: String?): QuranReciter {
            return entries.find { it.id == id } ?: ALAFASY
        }

        fun buildAyahUrl(reciter: QuranReciter, surahNumber: Int, ayahNumber: Int): String {
            val s = String.format(Locale.US, "%03d", surahNumber.coerceIn(1, 114))
            val a = String.format(Locale.US, "%03d", ayahNumber.coerceIn(1, 286))
            return "$AUDIO_SOURCE_BASE_URL/${reciter.everyAyahFolder}/$s$a.mp3"
        }
    }
}

data class QuranAudioState(
    val surahNumber: Int? = null,
    val surahNameBn: String = "",
    val surahNameEn: String = "",
    val ayahNumber: Int? = null,
    val ayahIndex: Int = -1,
    val totalAyahsInSurah: Int = 0,
    val isPlaying: Boolean = false,
    val isLoading: Boolean = false,
    val isOfflineError: Boolean = false,
    val errorMessageBn: String? = null,
    val errorMessageEn: String? = null,
    val reciter: QuranReciter = QuranReciter.ALAFASY
) {
    val isActive: Boolean
        get() = surahNumber != null && ayahNumber != null

    val canGoPrevious: Boolean
        get() = isActive && ayahIndex > 0

    val canGoNext: Boolean
        get() = isActive && ayahIndex in 0 until (totalAyahsInSurah - 1)

    fun surahTitle(isEnglish: Boolean): String = if (isEnglish) surahNameEn else surahNameBn
    fun errorMessage(isEnglish: Boolean): String? = if (isEnglish) errorMessageEn else errorMessageBn
}

class QuranAudioController(
    private val context: Context,
    private val onAyahHeard: (surahNumber: Int, ayahNumber: Int) -> Unit
) {
    private val appContext = context.applicationContext
    private val _audioState = MutableStateFlow(QuranAudioState())
    val audioState: StateFlow<QuranAudioState> = _audioState.asStateFlow()

    private var mediaPlayer: MediaPlayer? = null
    private var activeSurah: Surah? = null
    private var currentReciter: QuranReciter = QuranReciter.ALAFASY

    fun isOnline(): Boolean {
        val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ||
                caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN))
    }

    fun setReciter(reciter: QuranReciter) {
        val changed = currentReciter != reciter
        currentReciter = reciter
        _audioState.update { it.copy(reciter = reciter) }
        if (changed) {
            val surah = activeSurah
            val state = _audioState.value
            if (surah != null && state.ayahIndex in surah.ayahs.indices && (state.isPlaying || state.isLoading)) {
                playAtIndex(surah, state.ayahIndex)
            }
        }
    }

    fun playSurah(surah: Surah, reciter: QuranReciter = currentReciter) {
        currentReciter = reciter
        if (surah.ayahs.isEmpty()) return
        val state = _audioState.value
        if (state.surahNumber == surah.number && (state.isPlaying || state.isLoading)) {
            togglePlayPause()
            return
        }
        if (state.surahNumber == surah.number && state.ayahIndex in surah.ayahs.indices && !state.isOfflineError) {
            togglePlayPause()
            return
        }
        activeSurah = surah
        playAtIndex(surah, 0)
    }

    fun playAyah(surah: Surah, ayahNumber: Int, reciter: QuranReciter = currentReciter) {
        currentReciter = reciter
        val idx = surah.ayahs.indexOfFirst { it.number == ayahNumber }
        if (idx < 0) return
        val state = _audioState.value
        if (state.surahNumber == surah.number && state.ayahNumber == ayahNumber && state.isPlaying) {
            togglePlayPause()
            return
        }
        activeSurah = surah
        playAtIndex(surah, idx)
    }

    fun togglePlayPause() {
        val player = mediaPlayer
        val state = _audioState.value
        if (state.isOfflineError) {
            val surah = activeSurah
            if (surah != null && state.ayahIndex in surah.ayahs.indices) {
                playAtIndex(surah, state.ayahIndex)
            }
            return
        }
        if (player == null) {
            val surah = activeSurah
            if (surah != null && state.ayahIndex in surah.ayahs.indices) {
                playAtIndex(surah, state.ayahIndex)
            }
            return
        }
        try {
            if (player.isPlaying) {
                player.pause()
                _audioState.update { it.copy(isPlaying = false, isLoading = false) }
            } else {
                player.start()
                _audioState.update { it.copy(isPlaying = true, isLoading = false) }
            }
        } catch (_: Exception) {
            val surah = activeSurah
            if (surah != null && state.ayahIndex in surah.ayahs.indices) {
                playAtIndex(surah, state.ayahIndex)
            }
        }
    }

    fun nextAyah() {
        val surah = activeSurah ?: return
        val nextIdx = _audioState.value.ayahIndex + 1
        if (nextIdx in surah.ayahs.indices) {
            playAtIndex(surah, nextIdx)
        }
    }

    fun previousAyah() {
        val surah = activeSurah ?: return
        val prevIdx = _audioState.value.ayahIndex - 1
        if (prevIdx in surah.ayahs.indices) {
            playAtIndex(surah, prevIdx)
        }
    }

    fun stop() {
        releasePlayer()
        activeSurah = null
        _audioState.value = QuranAudioState(reciter = currentReciter)
    }

    fun release() {
        releasePlayer()
        activeSurah = null
    }

    private fun playAtIndex(surah: Surah, index: Int) {
        val ayah = surah.ayahs.getOrNull(index) ?: return
        releasePlayer()

        if (!isOnline()) {
            _audioState.value = QuranAudioState(
                surahNumber = surah.number,
                surahNameBn = surah.nameBangla,
                surahNameEn = surah.nameEnglish.ifBlank { surah.nameBangla },
                ayahNumber = ayah.number,
                ayahIndex = index,
                totalAyahsInSurah = surah.ayahs.size,
                isPlaying = false,
                isLoading = false,
                isOfflineError = true,
                errorMessageBn = "অডিও তিলাওয়াতের জন্য ইন্টারনেট সংযোগ প্রয়োজন (টেক্সট পড়া অফলাইনে চালু আছে)",
                errorMessageEn = "Internet required for audio (text reading works offline)",
                reciter = currentReciter
            )
            return
        }

        val url = QuranReciter.buildAyahUrl(currentReciter, surah.number, ayah.number)
        _audioState.value = QuranAudioState(
            surahNumber = surah.number,
            surahNameBn = surah.nameBangla,
            surahNameEn = surah.nameEnglish.ifBlank { surah.nameBangla },
            ayahNumber = ayah.number,
            ayahIndex = index,
            totalAyahsInSurah = surah.ayahs.size,
            isPlaying = false,
            isLoading = true,
            isOfflineError = false,
            errorMessageBn = null,
            errorMessageEn = null,
            reciter = currentReciter
        )

        try {
            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                setDataSource(url)
                setOnPreparedListener { mp ->
                    if (mediaPlayer === mp) {
                        mp.start()
                        _audioState.update {
                            it.copy(
                                isPlaying = true,
                                isLoading = false,
                                isOfflineError = false,
                                errorMessageBn = null,
                                errorMessageEn = null
                            )
                        }
                        onAyahHeard(surah.number, ayah.number)
                    }
                }
                setOnCompletionListener { mp ->
                    if (mediaPlayer === mp) {
                        val nextIdx = index + 1
                        if (nextIdx in surah.ayahs.indices) {
                            playAtIndex(surah, nextIdx)
                        } else {
                            releasePlayer()
                            _audioState.update {
                                it.copy(isPlaying = false, isLoading = false)
                            }
                        }
                    }
                }
                setOnErrorListener { mp, _, _ ->
                    if (mediaPlayer === mp) {
                        releasePlayer()
                        val offline = !isOnline()
                        _audioState.update {
                            it.copy(
                                isPlaying = false,
                                isLoading = false,
                                isOfflineError = offline,
                                errorMessageBn = if (offline) {
                                    "অডিও তিলাওয়াতের জন্য ইন্টারনেট সংযোগ প্রয়োজন"
                                } else {
                                    "অডিও লোড করা যায়নি। পুনরায় চেষ্টা করুন।"
                                },
                                errorMessageEn = if (offline) {
                                    "Internet required for audio"
                                } else {
                                    "Could not stream audio. Please try again."
                                }
                            )
                        }
                    }
                    true
                }
                prepareAsync()
            }
            mediaPlayer = player
        } catch (_: Exception) {
            releasePlayer()
            val offline = !isOnline()
            _audioState.update {
                it.copy(
                    isPlaying = false,
                    isLoading = false,
                    isOfflineError = offline,
                    errorMessageBn = if (offline) "অডিও তিলাওয়াতের জন্য ইন্টারনেট সংযোগ প্রয়োজন" else "অডিও চালু করতে সমস্যা হয়েছে।",
                    errorMessageEn = if (offline) "Internet required for audio" else "Unable to start audio playback."
                )
            }
        }
    }

    private fun releasePlayer() {
        try {
            mediaPlayer?.stop()
        } catch (_: Exception) {
        }
        try {
            mediaPlayer?.reset()
            mediaPlayer?.release()
        } catch (_: Exception) {
        } finally {
            mediaPlayer = null
        }
    }
}
