package com.example.util

import android.content.ClipData
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Paint
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RawRes
import androidx.core.content.FileProvider
import com.example.R
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import kotlin.math.PI
import kotlin.math.cos

enum class ShareVideoDuration(
    val seconds: Int,
    val labelBn: String,
    val labelEn: String
) {
    SEC_15(15, "১৫ সেকেন্ড", "15 sec"),
    SEC_30(30, "৩০ সেকেন্ড", "30 sec"),
    SEC_60(60, "৬০ সেকেন্ড", "60 sec");

    fun label(isEnglish: Boolean): String = if (isEnglish) labelEn else labelBn
}

/**
 * Royalty-free bundled background music tracks for Share Card Story/Reels videos.
 *
 * Audio License & Provenance:
 * - `R.raw.bg_nasheed_calm` ("শান্ত নাশিদ টোন" / "Calm Nasheed Tone"):
 *   Original procedurally synthesized vocal-pad modal harmony (D Hijaz / Nahawand),
 *   created specifically for Nur Bangla and released under CC0 1.0 Universal (Public Domain).
 *   Pre-mastered at low background level (~22% perceived volume / -14 dBFS) as a 15s seamless loop.
 * - `R.raw.bg_islamic_soft` ("হালকা ইসলামিক টোন" / "Soft Islamic Tone"):
 *   Original procedurally synthesized gentle acoustic oud/drone modal progression,
 *   created specifically for Nur Bangla and released under CC0 1.0 Universal (Public Domain).
 *   Pre-mastered at low background level (~22% perceived volume / -14 dBFS) as a 15s seamless loop.
 * - `NO_MUSIC`: Silent video with no audio track.
 */
enum class ShareVideoMusic(
    @RawRes val rawResId: Int?,
    val labelBn: String,
    val labelEn: String
) {
    CALM_NASHEED(
        rawResId = R.raw.bg_nasheed_calm,
        labelBn = "শান্ত নাশিদ টোন",
        labelEn = "Calm Nasheed Tone"
    ),
    SOFT_ISLAMIC(
        rawResId = R.raw.bg_islamic_soft,
        labelBn = "হালকা ইসলামিক টোন",
        labelEn = "Soft Islamic Tone"
    ),
    NO_MUSIC(
        rawResId = null,
        labelBn = "কোনো মিউজিক নয়",
        labelEn = "No Music"
    );

    fun label(isEnglish: Boolean): String = if (isEnglish) labelEn else labelBn
}

enum class VideoShareDestination(
    val candidatePackages: List<String>,
    val labelBn: String,
    val labelEn: String,
    val storyAction: String? = null
) {
    FACEBOOK_ALL(
        candidatePackages = listOf("com.facebook.katana"),
        labelBn = "ভিডিও — Facebook",
        labelEn = "Video — Facebook"
    ),
    FACEBOOK_STORY(
        candidatePackages = listOf("com.facebook.katana"),
        labelBn = "ভিডিও — Facebook Story",
        labelEn = "Video — Facebook Story",
        storyAction = "com.facebook.stories.ADD_TO_STORY"
    ),
    INSTAGRAM_STORY_REELS(
        candidatePackages = listOf("com.instagram.android"),
        labelBn = "ভিডিও — Instagram Story / Reels",
        labelEn = "Video — Instagram Story / Reels",
        storyAction = "com.instagram.share.ADD_TO_STORY"
    ),
    WHATSAPP_STATUS(
        candidatePackages = listOf("com.whatsapp", "com.whatsapp.w4b"),
        labelBn = "ভিডিও — WhatsApp Status",
        labelEn = "Video — WhatsApp Status"
    ),
    TELEGRAM(
        candidatePackages = listOf("org.telegram.messenger", "org.telegram.messenger.web"),
        labelBn = "ভিডিও — Telegram",
        labelEn = "Video — Telegram"
    );

    fun label(isEnglish: Boolean): String = if (isEnglish) labelEn else labelBn
}

/**
 * Renders any Nur Bangla Share Card into a vertical 1080x1920 H.264 MP4 video with:
 * - Subtle Ken Burns slow zoom (1.00x -> 1.06x smooth ease-in-out so the frame is never frozen)
 * - Seamlessly looped low-volume (~20-25%) royalty-free AAC background music for 15 / 30 / 60s
 * - Direct video share to Facebook (Groups/Feed/Reels/Story chooser), Facebook Story,
 *   Instagram Story/Reels, WhatsApp Status, Telegram, plus zero-permission Gallery save.
 */
object ShareCardVideoGenerator {

    const val VIDEO_WIDTH = 1080
    const val VIDEO_HEIGHT = 1920
    const val VIDEO_FPS = 15
    private const val VIDEO_BITRATE = 3_500_000
    private const val MAX_ZOOM_DELTA = 0.06f

    /**
     * Smooth ease-in-out Ken Burns zoom scale in [1.0f, 1.06f] for progress in [0f, 1f].
     */
    fun kenBurnsScale(progress: Float): Float {
        val p = progress.coerceIn(0f, 1f)
        val eased = (1.0 - cos(p * PI)) * 0.5
        return 1.0f + (MAX_ZOOM_DELTA * eased.toFloat())
    }

    fun generateVideoMp4(
        context: Context,
        cardBitmap: Bitmap,
        dateKey: String,
        duration: ShareVideoDuration,
        music: ShareVideoMusic,
        onProgress: (Float) -> Unit = {}
    ): Result<File> = runCatching {
        val outDir = File(context.cacheDir, "shared_videos").apply { mkdirs() }
        outDir.listFiles()?.forEach { old ->
            if (old.isFile && System.currentTimeMillis() - old.lastModified() > 24 * 60 * 60 * 1000L) {
                old.delete()
            }
        }
        val outFile = File(outDir, "nur_bangla_${dateKey}_${duration.seconds}s_${music.name.lowercase()}.mp4")
        if (outFile.exists()) outFile.delete()

        // Ensure card bitmap matches 1080x1920 vertical video aspect cleanly
        val sourceBitmap = if (cardBitmap.width == VIDEO_WIDTH && cardBitmap.height == VIDEO_HEIGHT) {
            cardBitmap
        } else {
            Bitmap.createScaledBitmap(cardBitmap, VIDEO_WIDTH, VIDEO_HEIGHT, true)
        }

        val totalFrames = duration.seconds * VIDEO_FPS
        val frameDurationUs = 1_000_000L / VIDEO_FPS
        val targetDurationUs = duration.seconds * 1_000_000L

        var encoder: MediaCodec? = null
        var muxer: MediaMuxer? = null
        var audioExtractor: MediaExtractor? = null
        var inputSurface: android.view.Surface? = null
        var muxerStarted = false

        try {
            val videoFormat = MediaFormat.createVideoFormat(
                MediaFormat.MIMETYPE_VIDEO_AVC,
                VIDEO_WIDTH,
                VIDEO_HEIGHT
            ).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                setInteger(MediaFormat.KEY_BIT_RATE, VIDEO_BITRATE)
                setInteger(MediaFormat.KEY_FRAME_RATE, VIDEO_FPS)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2)
            }

            encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            encoder.configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            inputSurface = encoder.createInputSurface()
            encoder.start()

            muxer = MediaMuxer(outFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            // Prepare optional AAC audio track from bundled .m4a raw resource
            var audioFormat: MediaFormat? = null
            var audioSourceTrackIdx = -1
            if (music.rawResId != null) {
                val afd = context.resources.openRawResourceFd(music.rawResId)
                audioExtractor = MediaExtractor().apply {
                    setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                }
                afd.close()
                for (i in 0 until audioExtractor.trackCount) {
                    val fmt = audioExtractor.getTrackFormat(i)
                    val mime = fmt.getString(MediaFormat.KEY_MIME).orEmpty()
                    if (mime.startsWith("audio/")) {
                        audioFormat = fmt
                        audioSourceTrackIdx = i
                        audioExtractor.selectTrack(i)
                        break
                    }
                }
            }

            var videoTrackIndex = -1
            var audioTrackIndex = -1
            val bufferInfo = MediaCodec.BufferInfo()
            val drawPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            val matrix = Matrix()
            var lastVideoPtsUs = -1L

            fun drainEncoder(endOfStream: Boolean) {
                if (endOfStream) {
                    encoder.signalEndOfInputStream()
                }
                while (true) {
                    val outIdx = encoder.dequeueOutputBuffer(bufferInfo, 10_000L)
                    when {
                        outIdx == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                            if (!endOfStream) break
                        }
                        outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            check(!muxerStarted) { "Video output format changed after muxer started" }
                            videoTrackIndex = muxer.addTrack(encoder.outputFormat)
                            if (audioFormat != null) {
                                audioTrackIndex = muxer.addTrack(audioFormat)
                            }
                            muxer.start()
                            muxerStarted = true
                        }
                        outIdx >= 0 -> {
                            val encodedData = encoder.getOutputBuffer(outIdx)
                            if (encodedData != null && bufferInfo.size > 0 &&
                                (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0 &&
                                muxerStarted
                            ) {
                                encodedData.position(bufferInfo.offset)
                                encodedData.limit(bufferInfo.offset + bufferInfo.size)
                                // Ensure strictly monotonically increasing timestamps
                                val pts = if (bufferInfo.presentationTimeUs <= lastVideoPtsUs) {
                                    lastVideoPtsUs + frameDurationUs
                                } else {
                                    bufferInfo.presentationTimeUs
                                }
                                lastVideoPtsUs = pts
                                bufferInfo.presentationTimeUs = pts
                                muxer.writeSampleData(videoTrackIndex, encodedData, bufferInfo)
                            }
                            encoder.releaseOutputBuffer(outIdx, false)
                            if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                                break
                            }
                        }
                    }
                }
            }

            // Render frames with smooth Ken Burns zoom onto the hardware encoder surface
            for (frameIdx in 0 until totalFrames) {
                val progress = if (totalFrames > 1) frameIdx.toFloat() / (totalFrames - 1).toFloat() else 0f
                val zoom = kenBurnsScale(progress)
                val canvas = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    inputSurface.lockHardwareCanvas()
                } else {
                    inputSurface.lockCanvas(null)
                }
                try {
                    matrix.reset()
                    matrix.postScale(zoom, zoom, VIDEO_WIDTH / 2f, VIDEO_HEIGHT / 2f)
                    canvas.drawBitmap(sourceBitmap, matrix, drawPaint)
                } finally {
                    inputSurface.unlockCanvasAndPost(canvas)
                }
                drainEncoder(endOfStream = false)
                if (frameIdx % 10 == 0 || frameIdx == totalFrames - 1) {
                    onProgress((frameIdx + 1).toFloat() / totalFrames.toFloat() * 0.88f)
                }
            }
            drainEncoder(endOfStream = true)

            // Seamlessly loop pre-encoded low-volume AAC audio frames to fill targetDurationUs
            if (muxerStarted && audioExtractor != null && audioTrackIndex >= 0 && audioSourceTrackIdx >= 0) {
                val audioBuf = ByteBuffer.allocate(256 * 1024)
                val audioInfo = MediaCodec.BufferInfo()
                var loopOffsetUs = 0L
                var lastAudioPtsUs = -1L
                val loopFrameGapUs = 23_220L // ~1024 samples @ 44.1kHz

                while (loopOffsetUs < targetDurationUs) {
                    audioExtractor.seekTo(0L, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
                    var loopMaxSampleTimeUs = 0L
                    while (true) {
                        val sampleSize = audioExtractor.readSampleData(audioBuf, 0)
                        if (sampleSize < 0) break
                        val rawSampleUs = audioExtractor.sampleTime
                        if (rawSampleUs < 0L) break
                        val outPtsUs = loopOffsetUs + rawSampleUs
                        if (outPtsUs >= targetDurationUs) {
                            loopOffsetUs = targetDurationUs
                            break
                        }
                        if (outPtsUs > lastAudioPtsUs) {
                            audioInfo.offset = 0
                            audioInfo.size = sampleSize
                            audioInfo.presentationTimeUs = outPtsUs
                            audioInfo.flags = audioExtractor.sampleFlags
                            muxer.writeSampleData(audioTrackIndex, audioBuf, audioInfo)
                            lastAudioPtsUs = outPtsUs
                        }
                        if (rawSampleUs > loopMaxSampleTimeUs) {
                            loopMaxSampleTimeUs = rawSampleUs
                        }
                        audioExtractor.advance()
                    }
                    if (loopMaxSampleTimeUs <= 0L) break
                    loopOffsetUs += loopMaxSampleTimeUs + loopFrameGapUs
                }
            }

            onProgress(1.0f)
        } finally {
            runCatching { audioExtractor?.release() }
            runCatching { encoder?.stop() }
            runCatching { encoder?.release() }
            runCatching { inputSurface?.release() }
            if (muxerStarted) {
                runCatching { muxer?.stop() }
            }
            runCatching { muxer?.release() }
            if (sourceBitmap !== cardBitmap) {
                sourceBitmap.recycle()
            }
        }

        check(outFile.exists() && outFile.length() > 1024L) { "Generated MP4 is empty" }
        outFile
    }

    fun getVideoUri(context: Context, videoFile: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            videoFile
        )
    }

    /**
     * Saves the rendered MP4 video to Gallery (Movies/NurBangla) with zero storage permissions on API 29+.
     */
    fun saveVideoMp4ToGallery(
        context: Context,
        videoFile: File,
        dateKey: String,
        durationSeconds: Int
    ): Result<Uri> = runCatching {
        val fileName = "NurBangla_${dateKey}_${durationSeconds}s_${System.currentTimeMillis() % 10000}.mp4"
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(
                    MediaStore.Video.Media.RELATIVE_PATH,
                    "${Environment.DIRECTORY_MOVIES}/NurBangla"
                )
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
            ?: error("Could not create MediaStore video entry")
        resolver.openOutputStream(uri)?.use { out ->
            FileInputStream(videoFile).use { input ->
                input.copyTo(out)
            }
            out.flush()
        } ?: error("Could not open MediaStore video stream")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val doneValues = ContentValues().apply {
                put(MediaStore.Video.Media.IS_PENDING, 0)
            }
            resolver.update(uri, doneValues, null, null)
        }
        uri
    }

    fun getInstalledVideoDestinations(context: Context): List<VideoShareDestination> {
        return VideoShareDestination.entries.filter { dest ->
            dest.candidatePackages.any { DailyShareCardGenerator.isPackageInstalled(context, it) }
        }
    }

    fun shareVideoToDestination(
        context: Context,
        destination: VideoShareDestination?,
        videoFile: File,
        entry: DailyInspirationEntry,
        isEnglish: Boolean
    ): Result<Unit> = runCatching {
        val uri = getVideoUri(context, videoFile)
        val caption = entry.formatShareText(isEnglish)
        val pm = context.packageManager

        val targetPackage = destination?.candidatePackages?.firstOrNull {
            DailyShareCardGenerator.isPackageInstalled(context, it)
        }

        // Dedicated Story composer for Facebook Story / Instagram Story when requested
        if (destination?.storyAction != null && targetPackage != null) {
            context.grantUriPermission(targetPackage, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            val storyIntent = Intent(destination.storyAction).apply {
                setPackage(targetPackage)
                setDataAndType(uri, "video/mp4")
                putExtra("interactive_asset_uri", uri)
                putExtra("source_application", context.packageName)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (storyIntent.resolveActivity(pm) != null) {
                context.startActivity(storyIntent)
                return@runCatching
            }
        }

        // Direct package ACTION_SEND (e.g. Facebook shows Your Groups / Feed / Reels / Your Story)
        if (targetPackage != null) {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "video/mp4"
                setPackage(targetPackage)
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, caption)
                clipData = ClipData.newUri(context.contentResolver, "Nur Bangla Share Video", uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (sendIntent.resolveActivity(pm) != null) {
                context.startActivity(sendIntent)
                return@runCatching
            }
        }

        // Fallback to standard system video share chooser
        val genericIntent = Intent(Intent.ACTION_SEND).apply {
            type = "video/mp4"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, caption)
            clipData = ClipData.newUri(context.contentResolver, "Nur Bangla Share Video", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(
            genericIntent,
            if (isEnglish) "Share Video Card" else "ভিডিও কার্ড শেয়ার করুন"
        ).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(chooser)
    }
}
