package com.example.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import com.example.R
import java.io.File
import java.io.FileOutputStream

enum class ShareCardFormat(
    val widthPx: Int,
    val heightPx: Int,
    val labelBn: String,
    val labelEn: String
) {
    STORY(
        widthPx = 1080,
        heightPx = 1920,
        labelBn = "স্টোরি (৯:১৬)",
        labelEn = "Story (9:16)"
    ),
    FEED(
        widthPx = 1080,
        heightPx = 1080,
        labelBn = "পোস্ট (১:১)",
        labelEn = "Post (1:1)"
    );

    fun label(isEnglish: Boolean): String = if (isEnglish) labelEn else labelBn
}

enum class MosqueSilhouetteStyle {
    CLASSIC_GRAND,
    OTTOMAN_TRIPLE,
    CRESCENT_ARCH,
    NABAWI_LANTERN
}

/**
 * 12 curated Islamic Share Card themes:
 * - Existing 4 unchanged: DARK_MOSQUE (Dark Gold), LIGHT_IVORY, EMERALD_MOSQUE, MIDNIGHT_BLUE
 * - New 8 in v1.9: SUNSET_ROSE_GOLD, TEAL_DAWN, ROYAL_PURPLE, DESERT_SAND, DEEP_FOREST,
 *   CRIMSON_DAWN, OCEAN_BLUE, AMBER_HERITAGE
 */
enum class ShareCardTheme(
    val labelBn: String,
    val labelEn: String,
    val previewBgArgb: Long,
    val previewAccentArgb: Long,
    val silhouetteStyle: MosqueSilhouetteStyle
) {
    DARK_MOSQUE(
        labelBn = "ডার্ক গোল্ড",
        labelEn = "Dark Gold",
        previewBgArgb = 0xFF0A2A1E,
        previewAccentArgb = 0xFFE8C468,
        silhouetteStyle = MosqueSilhouetteStyle.CLASSIC_GRAND
    ),
    LIGHT_IVORY(
        labelBn = "লাইট আইভরি",
        labelEn = "Light Ivory",
        previewBgArgb = 0xFFFAF4E6,
        previewAccentArgb = 0xFF5A3B1E,
        silhouetteStyle = MosqueSilhouetteStyle.CRESCENT_ARCH
    ),
    EMERALD_MOSQUE(
        labelBn = "এমেরাল্ড মসজিদ",
        labelEn = "Emerald Mosque",
        previewBgArgb = 0xFF05383B,
        previewAccentArgb = 0xFFF2CF72,
        silhouetteStyle = MosqueSilhouetteStyle.NABAWI_LANTERN
    ),
    MIDNIGHT_BLUE(
        labelBn = "মিডনাইট ব্লু",
        labelEn = "Midnight Blue",
        previewBgArgb = 0xFF091833,
        previewAccentArgb = 0xFFEBC86E,
        silhouetteStyle = MosqueSilhouetteStyle.OTTOMAN_TRIPLE
    ),
    SUNSET_ROSE_GOLD(
        labelBn = "সানসেট রোজ গোল্ড",
        labelEn = "Sunset Rose Gold",
        previewBgArgb = 0xFF3E182A,
        previewAccentArgb = 0xFFF3B7A2,
        silhouetteStyle = MosqueSilhouetteStyle.CRESCENT_ARCH
    ),
    TEAL_DAWN(
        labelBn = "টিল ডন",
        labelEn = "Teal Dawn",
        previewBgArgb = 0xFF0B3F4A,
        previewAccentArgb = 0xFF9BE7DC,
        silhouetteStyle = MosqueSilhouetteStyle.OTTOMAN_TRIPLE
    ),
    ROYAL_PURPLE(
        labelBn = "রয়্যাল পার্পল",
        labelEn = "Royal Purple",
        previewBgArgb = 0xFF251242,
        previewAccentArgb = 0xFFE8C77B,
        silhouetteStyle = MosqueSilhouetteStyle.CLASSIC_GRAND
    ),
    DESERT_SAND(
        labelBn = "ডেজার্ট স্যান্ড",
        labelEn = "Desert Sand",
        previewBgArgb = 0xFFF3E2C6,
        previewAccentArgb = 0xFF7D4E1D,
        silhouetteStyle = MosqueSilhouetteStyle.CRESCENT_ARCH
    ),
    DEEP_FOREST(
        labelBn = "ডিপ ফরেস্ট",
        labelEn = "Deep Forest",
        previewBgArgb = 0xFF0B2415,
        previewAccentArgb = 0xFFD9C074,
        silhouetteStyle = MosqueSilhouetteStyle.NABAWI_LANTERN
    ),
    CRIMSON_DAWN(
        labelBn = "ক্রিমসন ডন",
        labelEn = "Crimson Dawn",
        previewBgArgb = 0xFF42101A,
        previewAccentArgb = 0xFFF4CE7B,
        silhouetteStyle = MosqueSilhouetteStyle.OTTOMAN_TRIPLE
    ),
    OCEAN_BLUE(
        labelBn = "ওশান ব্লু",
        labelEn = "Ocean Blue",
        previewBgArgb = 0xFF082747,
        previewAccentArgb = 0xFF8CD5F7,
        silhouetteStyle = MosqueSilhouetteStyle.CLASSIC_GRAND
    ),
    AMBER_HERITAGE(
        labelBn = "অ্যাম্বার হেরিটেজ",
        labelEn = "Amber Heritage",
        previewBgArgb = 0xFF3A230D,
        previewAccentArgb = 0xFFF7C96B,
        silhouetteStyle = MosqueSilhouetteStyle.NABAWI_LANTERN
    );

    fun label(isEnglish: Boolean): String = if (isEnglish) labelEn else labelBn
}

enum class ShareCardFontSize(
    val scaleMultiplier: Float,
    val labelBn: String,
    val labelEn: String
) {
    SMALL(0.90f, "ছোট", "Small"),
    MEDIUM(1.00f, "মাঝারি", "Medium"),
    LARGE(1.12f, "বড়", "Large"),
    EXTRA_LARGE(1.28f, "আরও বড়", "Extra Large"),
    XXL(1.45f, "অতি বড় (XXL)", "XXL");

    fun label(isEnglish: Boolean): String = if (isEnglish) labelEn else labelBn
}

enum class DirectShareTarget(
    val candidatePackages: List<String>,
    val labelBn: String,
    val labelEn: String,
    val badgeColorArgb: Long,
    val storyAction: String? = null
) {
    FACEBOOK_STORY(
        candidatePackages = listOf("com.facebook.katana"),
        labelBn = "ফেসবুক স্টোরি",
        labelEn = "FB Story",
        badgeColorArgb = 0xFF1877F2,
        storyAction = "com.facebook.stories.ADD_TO_STORY"
    ),
    FACEBOOK_POST(
        candidatePackages = listOf("com.facebook.katana"),
        labelBn = "ফেসবুক পোস্ট",
        labelEn = "FB Post",
        badgeColorArgb = 0xFF1877F2
    ),
    WHATSAPP_STATUS(
        candidatePackages = listOf("com.whatsapp", "com.whatsapp.w4b"),
        labelBn = "হোয়াটসঅ্যাপ স্ট্যাটাস",
        labelEn = "WA Status",
        badgeColorArgb = 0xFF25D366
    ),
    WHATSAPP_CHAT(
        candidatePackages = listOf("com.whatsapp", "com.whatsapp.w4b"),
        labelBn = "হোয়াটসঅ্যাপ চ্যাট",
        labelEn = "WA Chat",
        badgeColorArgb = 0xFF128C7E
    ),
    TELEGRAM(
        candidatePackages = listOf("org.telegram.messenger", "org.telegram.messenger.web"),
        labelBn = "টেলিগ্রাম",
        labelEn = "Telegram",
        badgeColorArgb = 0xFF0088CC
    ),
    INSTAGRAM_STORY(
        candidatePackages = listOf("com.instagram.android"),
        labelBn = "ইনস্টাগ্রাম স্টোরি",
        labelEn = "IG Story",
        badgeColorArgb = 0xFFE1306C,
        storyAction = "com.instagram.share.ADD_TO_STORY"
    ),
    INSTAGRAM_POST(
        candidatePackages = listOf("com.instagram.android"),
        labelBn = "ইনস্টাগ্রাম পোস্ট",
        labelEn = "IG Post",
        badgeColorArgb = 0xFFC13584
    ),
    MESSENGER(
        candidatePackages = listOf("com.facebook.orca"),
        labelBn = "মেসেঞ্জার",
        labelEn = "Messenger",
        badgeColorArgb = 0xFF00B2FF
    ),
    X_TWITTER(
        candidatePackages = listOf("com.twitter.android"),
        labelBn = "এক্স (X)",
        labelEn = "X (Twitter)",
        badgeColorArgb = 0xFF14171A
    );

    fun label(isEnglish: Boolean): String = if (isEnglish) labelEn else labelBn
}

data class RenderedShareCard(
    val bitmap: Bitmap,
    val scaleFactorUsed: Float,
    val isClipped: Boolean,
    val hadithBodyPx: Float = 0f,
    val ayahArabicPx: Float = 0f,
    val ayahTranslationPx: Float = 0f
)

/**
 * Generates high-resolution, zero-clipping PNG share cards (1080x1920 Story/Status and
 * 1080x1080 Feed/Post) with 12 rich Islamic themes, 5 font sizes (Small to XXL),
 * ornate gold border, 4 vector mosque silhouette variants, special-day badge,
 * direct app share targets, Facebook account-switch helper, and zero-permission Gallery save.
 *
 * NOTE: Zero QR codes anywhere per user request.
 */
object DailyShareCardGenerator {

    private data class CardPalette(
        val bgTop: Int,
        val bgBottom: Int,
        val panelFill: Int,
        val panelStroke: Int,
        val ornateGold: Int,
        val silhouetteFill: Int,
        val badgeBg: Int,
        val badgeText: Int,
        val titleText: Int,
        val arabicText: Int,
        val bodyText: Int,
        val sourceText: Int,
        val dividerColor: Int
    )

    private fun paletteFor(theme: ShareCardTheme): CardPalette = when (theme) {
        ShareCardTheme.DARK_MOSQUE -> CardPalette(
            bgTop = Color.parseColor("#072219"),
            bgBottom = Color.parseColor("#0F3D2D"),
            panelFill = Color.parseColor("#123629"),
            panelStroke = Color.parseColor("#8A6E35"),
            ornateGold = Color.parseColor("#E8C468"),
            silhouetteFill = Color.parseColor("#33E8C468"),
            badgeBg = Color.parseColor("#1D4E3D"),
            badgeText = Color.parseColor("#FFE494"),
            titleText = Color.parseColor("#FFFFFF"),
            arabicText = Color.parseColor("#FFFDF5"),
            bodyText = Color.parseColor("#F5F8F6"),
            sourceText = Color.parseColor("#D4E7DD"),
            dividerColor = Color.parseColor("#54856E")
        )
        ShareCardTheme.LIGHT_IVORY -> CardPalette(
            bgTop = Color.parseColor("#FBF5E8"),
            bgBottom = Color.parseColor("#EFE2C6"),
            panelFill = Color.parseColor("#FFFDF8"),
            panelStroke = Color.parseColor("#C29E56"),
            ornateGold = Color.parseColor("#9C7327"),
            silhouetteFill = Color.parseColor("#2B8B6520"),
            badgeBg = Color.parseColor("#F3E5C5"),
            badgeText = Color.parseColor("#4A2F14"),
            titleText = Color.parseColor("#2B1B12"),
            arabicText = Color.parseColor("#25160D"),
            bodyText = Color.parseColor("#2B1B12"),
            sourceText = Color.parseColor("#5F442E"),
            dividerColor = Color.parseColor("#CFAF6E")
        )
        ShareCardTheme.EMERALD_MOSQUE -> CardPalette(
            bgTop = Color.parseColor("#043437"),
            bgBottom = Color.parseColor("#074F52"),
            panelFill = Color.parseColor("#084144"),
            panelStroke = Color.parseColor("#C9A54A"),
            ornateGold = Color.parseColor("#F2CF72"),
            silhouetteFill = Color.parseColor("#38F2CF72"),
            badgeBg = Color.parseColor("#0C5B5E"),
            badgeText = Color.parseColor("#FFEA9F"),
            titleText = Color.parseColor("#FFFFFF"),
            arabicText = Color.parseColor("#FFFFFF"),
            bodyText = Color.parseColor("#F2FAFA"),
            sourceText = Color.parseColor("#C8E7E8"),
            dividerColor = Color.parseColor("#2F7E80")
        )
        ShareCardTheme.MIDNIGHT_BLUE -> CardPalette(
            bgTop = Color.parseColor("#07142B"),
            bgBottom = Color.parseColor("#10274D"),
            panelFill = Color.parseColor("#112344"),
            panelStroke = Color.parseColor("#9E8040"),
            ornateGold = Color.parseColor("#EBC86E"),
            silhouetteFill = Color.parseColor("#33EBC86E"),
            badgeBg = Color.parseColor("#1C3663"),
            badgeText = Color.parseColor("#FFE494"),
            titleText = Color.parseColor("#FFFFFF"),
            arabicText = Color.parseColor("#FFFDF7"),
            bodyText = Color.parseColor("#F1F5FC"),
            sourceText = Color.parseColor("#C5D5EE"),
            dividerColor = Color.parseColor("#355282")
        )
        ShareCardTheme.SUNSET_ROSE_GOLD -> CardPalette(
            bgTop = Color.parseColor("#351324"),
            bgBottom = Color.parseColor("#5E2339"),
            panelFill = Color.parseColor("#471A2D"),
            panelStroke = Color.parseColor("#D49583"),
            ornateGold = Color.parseColor("#F6C0AE"),
            silhouetteFill = Color.parseColor("#38F6C0AE"),
            badgeBg = Color.parseColor("#682841"),
            badgeText = Color.parseColor("#FFE1D5"),
            titleText = Color.parseColor("#FFFFFF"),
            arabicText = Color.parseColor("#FFF9F6"),
            bodyText = Color.parseColor("#FDF1EE"),
            sourceText = Color.parseColor("#F0CEC4"),
            dividerColor = Color.parseColor("#8A475C")
        )
        ShareCardTheme.TEAL_DAWN -> CardPalette(
            bgTop = Color.parseColor("#08333D"),
            bgBottom = Color.parseColor("#115966"),
            panelFill = Color.parseColor("#0D4652"),
            panelStroke = Color.parseColor("#73C9BE"),
            ornateGold = Color.parseColor("#A5EFE4"),
            silhouetteFill = Color.parseColor("#36A5EFE4"),
            badgeBg = Color.parseColor("#166370"),
            badgeText = Color.parseColor("#DCFFF9"),
            titleText = Color.parseColor("#FFFFFF"),
            arabicText = Color.parseColor("#FFFFFF"),
            bodyText = Color.parseColor("#EFFBFB"),
            sourceText = Color.parseColor("#C4E9EA"),
            dividerColor = Color.parseColor("#3B8C94")
        )
        ShareCardTheme.ROYAL_PURPLE -> CardPalette(
            bgTop = Color.parseColor("#1E0D38"),
            bgBottom = Color.parseColor("#391D63"),
            panelFill = Color.parseColor("#2B154D"),
            panelStroke = Color.parseColor("#C4A05A"),
            ornateGold = Color.parseColor("#F0CF82"),
            silhouetteFill = Color.parseColor("#36F0CF82"),
            badgeBg = Color.parseColor("#442473"),
            badgeText = Color.parseColor("#FFE8AC"),
            titleText = Color.parseColor("#FFFFFF"),
            arabicText = Color.parseColor("#FFFDF7"),
            bodyText = Color.parseColor("#F7F3FC"),
            sourceText = Color.parseColor("#DACDEC"),
            dividerColor = Color.parseColor("#6D4A9E")
        )
        ShareCardTheme.DESERT_SAND -> CardPalette(
            bgTop = Color.parseColor("#F5E6CD"),
            bgBottom = Color.parseColor("#E6CCA4"),
            panelFill = Color.parseColor("#FCF5E8"),
            panelStroke = Color.parseColor("#B68544"),
            ornateGold = Color.parseColor("#87531A"),
            silhouetteFill = Color.parseColor("#2E87531A"),
            badgeBg = Color.parseColor("#EAD2AE"),
            badgeText = Color.parseColor("#47290D"),
            titleText = Color.parseColor("#2D1908"),
            arabicText = Color.parseColor("#261406"),
            bodyText = Color.parseColor("#2F1B0B"),
            sourceText = Color.parseColor("#613E1E"),
            dividerColor = Color.parseColor("#C29559")
        )
        ShareCardTheme.DEEP_FOREST -> CardPalette(
            bgTop = Color.parseColor("#081D11"),
            bgBottom = Color.parseColor("#143822"),
            panelFill = Color.parseColor("#0F2B1A"),
            panelStroke = Color.parseColor("#9B8646"),
            ornateGold = Color.parseColor("#E3CB7F"),
            silhouetteFill = Color.parseColor("#33E3CB7F"),
            badgeBg = Color.parseColor("#1B472D"),
            badgeText = Color.parseColor("#F7E6A8"),
            titleText = Color.parseColor("#FFFFFF"),
            arabicText = Color.parseColor("#FCFDF7"),
            bodyText = Color.parseColor("#F1F7F2"),
            sourceText = Color.parseColor("#CBE0CF"),
            dividerColor = Color.parseColor("#3B6E4E")
        )
        ShareCardTheme.CRIMSON_DAWN -> CardPalette(
            bgTop = Color.parseColor("#3A0D16"),
            bgBottom = Color.parseColor("#631926"),
            panelFill = Color.parseColor("#4B131E"),
            panelStroke = Color.parseColor("#D4A955"),
            ornateGold = Color.parseColor("#F7D482"),
            silhouetteFill = Color.parseColor("#36F7D482"),
            badgeBg = Color.parseColor("#6E202E"),
            badgeText = Color.parseColor("#FFE8AC"),
            titleText = Color.parseColor("#FFFFFF"),
            arabicText = Color.parseColor("#FFFDF7"),
            bodyText = Color.parseColor("#FDF2F3"),
            sourceText = Color.parseColor("#F0CCD1"),
            dividerColor = Color.parseColor("#8C3A48")
        )
        ShareCardTheme.OCEAN_BLUE -> CardPalette(
            bgTop = Color.parseColor("#06203B"),
            bgBottom = Color.parseColor("#0E3E6B"),
            panelFill = Color.parseColor("#0B2E52"),
            panelStroke = Color.parseColor("#6FB8E0"),
            ornateGold = Color.parseColor("#9ADDFB"),
            silhouetteFill = Color.parseColor("#369ADDFB"),
            badgeBg = Color.parseColor("#14497A"),
            badgeText = Color.parseColor("#DEF4FF"),
            titleText = Color.parseColor("#FFFFFF"),
            arabicText = Color.parseColor("#FFFFFF"),
            bodyText = Color.parseColor("#F0F7FC"),
            sourceText = Color.parseColor("#C7E1F5"),
            dividerColor = Color.parseColor("#3572A6")
        )
        ShareCardTheme.AMBER_HERITAGE -> CardPalette(
            bgTop = Color.parseColor("#301D0A"),
            bgBottom = Color.parseColor("#543412"),
            panelFill = Color.parseColor("#3E260E"),
            panelStroke = Color.parseColor("#D6A249"),
            ornateGold = Color.parseColor("#F9D076"),
            silhouetteFill = Color.parseColor("#38F9D076"),
            badgeBg = Color.parseColor("#5C3915"),
            badgeText = Color.parseColor("#FFE8AE"),
            titleText = Color.parseColor("#FFFFFF"),
            arabicText = Color.parseColor("#FFFBF0"),
            bodyText = Color.parseColor("#FCF5E8"),
            sourceText = Color.parseColor("#EAD4AE"),
            dividerColor = Color.parseColor("#875D27")
        )
    }

    private fun loadTypefaceSafely(context: Context, fontRes: Int, fallback: Typeface): Typeface {
        return try {
            ResourcesCompat.getFont(context, fontRes) ?: fallback
        } catch (_: Exception) {
            fallback
        }
    }

    fun renderDailyCard(
        context: Context,
        entry: DailyInspirationEntry,
        isEnglish: Boolean,
        format: ShareCardFormat,
        theme: ShareCardTheme,
        fontSize: ShareCardFontSize = ShareCardFontSize.MEDIUM
    ): RenderedShareCard {
        val width = format.widthPx
        val height = format.heightPx
        val palette = paletteFor(theme)

        val banglaTypeface = loadTypefaceSafely(context, R.font.hind_siliguri, Typeface.SANS_SERIF)
        val banglaBold = Typeface.create(banglaTypeface, Typeface.BOLD)
        val arabicTypeface = loadTypefaceSafely(context, R.font.amiri, Typeface.SERIF)

        val isStory = format == ShareCardFormat.STORY
        val specialLabel = entry.specialDayLabel(isEnglish)
        val outerPad = if (isStory) 58f else 44f
        val innerPad = if (isStory) 38f else 28f
        val specialExtra = if (specialLabel != null) (if (isStory) 48f else 34f) else 0f
        val headerHeight = (if (isStory) 240f else 148f) + specialExtra
        val footerHeight = if (isStory) 92f else 64f
        val availableBodyHeight = (height - outerPad * 2f - headerHeight - footerHeight).coerceAtLeast(280f)
        val contentWidth = (width - outerPad * 2f - innerPad * 2f).toInt().coerceAtLeast(300)

        val hadithHeader = if (isEnglish) "HADITH OF THE DAY" else "আজকের হাদিস"
        val ayahHeader = if (isEnglish) "DAILY AYAH" else "আজকের আয়াত"
        val hadithBody = entry.hadith.meaning(isEnglish)
        val hadithSource = "— ${entry.hadith.source(isEnglish)}"
        val ayahBody = entry.ayah.meaning(isEnglish)
        val ayahSource = "— ${entry.ayah.source(isEnglish)}"

        val userMul = fontSize.scaleMultiplier
        val baseHadithArabicSize = (if (isStory) 44f else 30f) * userMul
        val baseHadithBodySize = (if (isStory) 38f else 26f) * userMul
        val baseAyahArabicSize = (if (isStory) 50f else 33f) * userMul
        val baseAyahBodySize = (if (isStory) 35f else 25f) * userMul
        val baseSourceSize = (if (isStory) 26f else 18f) * userMul
        val baseBadgeSize = (if (isStory) 25f else 18f) * userMul

        // Auto-shrink ONLY enough to fit so EVERY theme x font size x format NEVER clips
        var chosenScale = 1.0f
        var chosenLayouts: MeasuredBodyLayouts? = null
        val candidateScales = floatArrayOf(
            1.00f, 0.95f, 0.90f, 0.85f, 0.80f, 0.75f,
            0.70f, 0.65f, 0.60f, 0.56f, 0.52f, 0.48f,
            0.44f, 0.40f, 0.36f, 0.32f, 0.28f
        )
        for (scale in candidateScales) {
            val measured = measureBodyLayouts(
                contentWidth = contentWidth,
                isStory = isStory,
                scale = scale,
                baseHadithArabicSize = baseHadithArabicSize,
                baseHadithBodySize = baseHadithBodySize,
                baseAyahArabicSize = baseAyahArabicSize,
                baseAyahBodySize = baseAyahBodySize,
                baseSourceSize = baseSourceSize,
                baseBadgeSize = baseBadgeSize,
                hadithHeader = hadithHeader,
                hadithArabic = entry.hadith.arabic,
                hadithBody = hadithBody,
                hadithSource = hadithSource,
                ayahHeader = ayahHeader,
                ayahArabic = entry.ayah.arabic,
                ayahBody = ayahBody,
                ayahSource = ayahSource,
                banglaTypeface = banglaTypeface,
                banglaBold = banglaBold,
                arabicTypeface = arabicTypeface,
                palette = palette
            )
            chosenScale = scale
            chosenLayouts = measured
            if (measured.totalHeight <= availableBodyHeight) {
                break
            }
        }

        val layouts = chosenLayouts!!
        val isClipped = layouts.totalHeight > availableBodyHeight + 2f

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 1. Background rich vertical gradient
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, 0f, height.toFloat(),
                palette.bgTop, palette.bgBottom,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // 2. Ornate double decorative border + corner flourishes
        drawOrnateGoldBorder(canvas, width.toFloat(), height.toFloat(), palette.ornateGold)

        // 3. Vector Mosque Silhouette variant at top header
        val mosqueBaseY = outerPad + (if (isStory) 114f else 70f)
        drawMosqueSilhouette(
            canvas = canvas,
            silhouetteStyle = theme.silhouetteStyle,
            centerX = width / 2f,
            baseY = mosqueBaseY,
            scale = if (isStory) 1.0f else 0.64f,
            fillColor = palette.silhouetteFill,
            strokeColor = palette.ornateGold
        )

        // 4. Header: Brand name + Gregorian & Hijri date + optional Special Day pill
        val brandPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.ornateGold
            typeface = banglaBold
            textSize = if (isStory) 48f else 35f
            textAlign = Paint.Align.CENTER
        }
        val subBrandPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.sourceText
            typeface = banglaTypeface
            textSize = if (isStory) 26f else 19f
            textAlign = Paint.Align.CENTER
        }
        val brandTitle = if (isEnglish) "নূর বাংলা  •  Nur Bangla" else "নূর বাংলা"
        val dateLine = "${entry.gregorianDate(isEnglish)}  •  ${entry.hijriDate(isEnglish)}"
        val titleY = mosqueBaseY + (if (isStory) 50f else 34f)
        canvas.drawText(brandTitle, width / 2f, titleY, brandPaint)
        canvas.drawText(dateLine, width / 2f, titleY + (if (isStory) 40f else 28f), subBrandPaint)

        if (specialLabel != null) {
            val spPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = palette.badgeText
                typeface = banglaBold
                textSize = if (isStory) 24f else 18f
                textAlign = Paint.Align.CENTER
            }
            val textW = spPaint.measureText(specialLabel)
            val pillTop = titleY + (if (isStory) 54f else 36f)
            val pillH = if (isStory) 38f else 28f
            val pillRect = RectF(
                width / 2f - textW / 2f - 24f,
                pillTop,
                width / 2f + textW / 2f + 24f,
                pillTop + pillH
            )
            val spBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = palette.badgeBg
                style = Paint.Style.FILL
            }
            val spBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = palette.ornateGold
                style = Paint.Style.STROKE
                strokeWidth = 2f
            }
            canvas.drawRoundRect(pillRect, 18f, 18f, spBgPaint)
            canvas.drawRoundRect(pillRect, 18f, 18f, spBorder)
            canvas.drawText(specialLabel, width / 2f, pillTop + pillH * 0.72f, spPaint)
        }

        // Ornamental header divider with central diamond
        val headerDividerY = outerPad + headerHeight - 14f
        drawOrnamentalDivider(canvas, outerPad + 12f, width - outerPad - 12f, headerDividerY, palette.ornateGold)

        // 5. Vertically center Hadith & Ayah panels inside available region
        val bodyTopBase = outerPad + headerHeight
        val extraSpace = (availableBodyHeight - layouts.totalHeight).coerceAtLeast(0f)
        var cursorY = bodyTopBase + (extraSpace * 0.44f)

        val cardFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.panelFill
            style = Paint.Style.FILL
        }
        val cardStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.panelStroke
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        val badgeFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.badgeBg
            style = Paint.Style.FILL
        }

        // Draw Hadith Panel
        val hadithCardRect = RectF(
            outerPad,
            cursorY,
            width - outerPad,
            cursorY + layouts.hadithCardHeight
        )
        canvas.drawRoundRect(hadithCardRect, 28f, 28f, cardFillPaint)
        canvas.drawRoundRect(hadithCardRect, 28f, 28f, cardStrokePaint)

        var y = cursorY + layouts.cardVerticalPad
        drawBadgePill(canvas, outerPad + innerPad, y, layouts.hadithBadgeLayout, badgeFillPaint, palette.ornateGold)
        y += layouts.hadithBadgeLayout.height + layouts.badgeBottomGap + 14f
        drawLayoutAt(canvas, layouts.hadithArabicLayout, outerPad + innerPad, y)
        y += layouts.hadithArabicLayout.height + layouts.arabicToBodyGap
        drawLayoutAt(canvas, layouts.hadithBodyLayout, outerPad + innerPad, y)
        y += layouts.hadithBodyLayout.height + layouts.bodyToSourceGap
        drawLayoutAt(canvas, layouts.hadithSourceLayout, outerPad + innerPad, y)

        // Decorative separator diamond between Hadith and Ayah cards
        val midGapY = cursorY + layouts.hadithCardHeight + (layouts.betweenCardsGap / 2f)
        drawOrnamentalDivider(canvas, outerPad + 90f, width - outerPad - 90f, midGapY, palette.ornateGold)

        cursorY += layouts.hadithCardHeight + layouts.betweenCardsGap

        // Draw Ayah Panel
        val ayahCardRect = RectF(
            outerPad,
            cursorY,
            width - outerPad,
            cursorY + layouts.ayahCardHeight
        )
        canvas.drawRoundRect(ayahCardRect, 28f, 28f, cardFillPaint)
        canvas.drawRoundRect(ayahCardRect, 28f, 28f, cardStrokePaint)

        y = cursorY + layouts.cardVerticalPad
        drawBadgePill(canvas, outerPad + innerPad, y, layouts.ayahBadgeLayout, badgeFillPaint, palette.ornateGold)
        y += layouts.ayahBadgeLayout.height + layouts.badgeBottomGap + 14f
        drawLayoutAt(canvas, layouts.ayahArabicLayout, outerPad + innerPad, y)
        y += layouts.ayahArabicLayout.height + layouts.arabicToBodyGap
        drawLayoutAt(canvas, layouts.ayahBodyLayout, outerPad + innerPad, y)
        y += layouts.ayahBodyLayout.height + layouts.bodyToSourceGap
        drawLayoutAt(canvas, layouts.ayahSourceLayout, outerPad + innerPad, y)

        // 6. Footer branding bar (text only — NO QR code)
        val footerY = height - outerPad - (if (isStory) 16f else 10f)
        val footerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.ornateGold
            typeface = banglaBold
            textSize = if (isStory) 28f else 21f
            textAlign = Paint.Align.CENTER
        }
        val footerLabel = if (isEnglish) {
            "নূর বাংলা • ডাউনলোড করুন  (Nur Bangla)"
        } else {
            "নূর বাংলা • ডাউনলোড করুন"
        }
        canvas.drawText(footerLabel, width / 2f, footerY, footerPaint)

        return RenderedShareCard(
            bitmap = bitmap,
            scaleFactorUsed = chosenScale,
            isClipped = isClipped,
            hadithBodyPx = layouts.hadithBodySizePx,
            ayahArabicPx = layouts.ayahArabicSizePx,
            ayahTranslationPx = layouts.ayahBodySizePx
        )
    }

    private fun drawOrnateGoldBorder(canvas: Canvas, width: Float, height: Float, goldColor: Int) {
        val outerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 4.5f
            color = goldColor
        }
        val innerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2f
            color = goldColor
            alpha = 180
        }
        val cornerFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = goldColor
        }

        val outerRect = RectF(24f, 24f, width - 24f, height - 24f)
        val innerRect = RectF(36f, 36f, width - 36f, height - 36f)
        canvas.drawRoundRect(outerRect, 34f, 34f, outerPaint)
        canvas.drawRoundRect(innerRect, 26f, 26f, innerPaint)

        val corners = listOf(
            Pair(50f, 50f),
            Pair(width - 50f, 50f),
            Pair(50f, height - 50f),
            Pair(width - 50f, height - 50f)
        )
        for ((cx, cy) in corners) {
            val diamond = Path().apply {
                moveTo(cx, cy - 11f)
                lineTo(cx + 11f, cy)
                lineTo(cx, cy + 11f)
                lineTo(cx - 11f, cy)
                close()
            }
            canvas.drawPath(diamond, cornerFill)
        }
    }

    private fun drawMosqueSilhouette(
        canvas: Canvas,
        silhouetteStyle: MosqueSilhouetteStyle,
        centerX: Float,
        baseY: Float,
        scale: Float,
        fillColor: Int,
        strokeColor: Int
    ) {
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.style = Paint.Style.FILL
            this.color = fillColor
        }
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.style = Paint.Style.STROKE
            this.strokeWidth = 2.6f
            this.color = strokeColor
            this.strokeCap = Paint.Cap.ROUND
            this.strokeJoin = Paint.Join.ROUND
        }
        val s = scale

        when (silhouetteStyle) {
            MosqueSilhouetteStyle.CLASSIC_GRAND -> {
                val path = Path().apply {
                    moveTo(centerX - 180f * s, baseY)
                    lineTo(centerX - 180f * s, baseY - 22f * s)
                    lineTo(centerX - 160f * s, baseY - 22f * s)
                    lineTo(centerX - 160f * s, baseY - 82f * s)
                    lineTo(centerX - 152f * s, baseY - 102f * s)
                    lineTo(centerX - 144f * s, baseY - 82f * s)
                    lineTo(centerX - 144f * s, baseY - 26f * s)
                    lineTo(centerX - 118f * s, baseY - 26f * s)
                    quadTo(centerX - 92f * s, baseY - 68f * s, centerX - 66f * s, baseY - 26f * s)
                    cubicTo(centerX - 58f * s, baseY - 78f * s, centerX - 20f * s, baseY - 94f * s, centerX, baseY - 114f * s)
                    cubicTo(centerX + 20f * s, baseY - 94f * s, centerX + 58f * s, baseY - 78f * s, centerX + 66f * s, baseY - 26f * s)
                    quadTo(centerX + 92f * s, baseY - 68f * s, centerX + 118f * s, baseY - 26f * s)
                    lineTo(centerX + 144f * s, baseY - 26f * s)
                    lineTo(centerX + 144f * s, baseY - 82f * s)
                    lineTo(centerX + 152f * s, baseY - 102f * s)
                    lineTo(centerX + 160f * s, baseY - 82f * s)
                    lineTo(centerX + 160f * s, baseY - 22f * s)
                    lineTo(centerX + 180f * s, baseY - 22f * s)
                    lineTo(centerX + 180f * s, baseY)
                    close()
                }
                canvas.drawPath(path, fillPaint)
                canvas.drawPath(path, strokePaint)
                drawCrescentFinial(canvas, centerX, baseY - 114f * s, s, strokePaint)
            }

            MosqueSilhouetteStyle.OTTOMAN_TRIPLE -> {
                val path = Path().apply {
                    moveTo(centerX - 196f * s, baseY)
                    lineTo(centerX - 196f * s, baseY - 18f * s)
                    // Tall pencil left minaret
                    lineTo(centerX - 178f * s, baseY - 18f * s)
                    lineTo(centerX - 178f * s, baseY - 94f * s)
                    lineTo(centerX - 172f * s, baseY - 122f * s)
                    lineTo(centerX - 166f * s, baseY - 94f * s)
                    lineTo(centerX - 166f * s, baseY - 28f * s)
                    // Stepped left semi-dome
                    quadTo(centerX - 115f * s, baseY - 76f * s, centerX - 68f * s, baseY - 46f * s)
                    // Broad Ottoman central dome
                    cubicTo(centerX - 52f * s, baseY - 104f * s, centerX + 52f * s, baseY - 104f * s, centerX + 68f * s, baseY - 46f * s)
                    // Stepped right semi-dome
                    quadTo(centerX + 115f * s, baseY - 76f * s, centerX + 166f * s, baseY - 28f * s)
                    // Tall pencil right minaret
                    lineTo(centerX + 166f * s, baseY - 94f * s)
                    lineTo(centerX + 172f * s, baseY - 122f * s)
                    lineTo(centerX + 178f * s, baseY - 94f * s)
                    lineTo(centerX + 178f * s, baseY - 18f * s)
                    lineTo(centerX + 196f * s, baseY - 18f * s)
                    lineTo(centerX + 196f * s, baseY)
                    close()
                }
                canvas.drawPath(path, fillPaint)
                canvas.drawPath(path, strokePaint)
                drawCrescentFinial(canvas, centerX, baseY - 96f * s, s, strokePaint)
            }

            MosqueSilhouetteStyle.CRESCENT_ARCH -> {
                val path = Path().apply {
                    moveTo(centerX - 170f * s, baseY)
                    lineTo(centerX - 170f * s, baseY - 36f * s)
                    // Left arch pavilion
                    quadTo(centerX - 128f * s, baseY - 64f * s, centerX - 86f * s, baseY - 36f * s)
                    // Pointed Moroccan/Andalusian central dome
                    lineTo(centerX - 58f * s, baseY - 36f * s)
                    quadTo(centerX - 42f * s, baseY - 96f * s, centerX, baseY - 110f * s)
                    quadTo(centerX + 42f * s, baseY - 96f * s, centerX + 58f * s, baseY - 36f * s)
                    lineTo(centerX + 86f * s, baseY - 36f * s)
                    // Right arch pavilion
                    quadTo(centerX + 128f * s, baseY - 64f * s, centerX + 170f * s, baseY - 36f * s)
                    lineTo(centerX + 170f * s, baseY)
                    close()
                }
                canvas.drawPath(path, fillPaint)
                canvas.drawPath(path, strokePaint)
                drawCrescentFinial(canvas, centerX, baseY - 110f * s, s, strokePaint)
            }

            MosqueSilhouetteStyle.NABAWI_LANTERN -> {
                val path = Path().apply {
                    moveTo(centerX - 188f * s, baseY)
                    lineTo(centerX - 188f * s, baseY - 26f * s)
                    // Left twin slender towers
                    lineTo(centerX - 166f * s, baseY - 26f * s)
                    lineTo(centerX - 166f * s, baseY - 88f * s)
                    lineTo(centerX - 158f * s, baseY - 108f * s)
                    lineTo(centerX - 150f * s, baseY - 88f * s)
                    lineTo(centerX - 150f * s, baseY - 32f * s)
                    // Colonnade roofline
                    lineTo(centerX - 72f * s, baseY - 32f * s)
                    // Iconic tall Green Dome silhouette
                    cubicTo(centerX - 66f * s, baseY - 82f * s, centerX - 24f * s, baseY - 106f * s, centerX, baseY - 118f * s)
                    cubicTo(centerX + 24f * s, baseY - 106f * s, centerX + 66f * s, baseY - 82f * s, centerX + 72f * s, baseY - 32f * s)
                    lineTo(centerX + 150f * s, baseY - 32f * s)
                    lineTo(centerX + 150f * s, baseY - 88f * s)
                    lineTo(centerX + 158f * s, baseY - 108f * s)
                    lineTo(centerX + 166f * s, baseY - 88f * s)
                    lineTo(centerX + 166f * s, baseY - 26f * s)
                    lineTo(centerX + 188f * s, baseY - 26f * s)
                    lineTo(centerX + 188f * s, baseY)
                    close()
                }
                canvas.drawPath(path, fillPaint)
                canvas.drawPath(path, strokePaint)
                drawCrescentFinial(canvas, centerX, baseY - 118f * s, s, strokePaint)
            }
        }

        // Subtle arched central doorway
        val archPath = Path().apply {
            moveTo(centerX - 20f * s, baseY)
            lineTo(centerX - 20f * s, baseY - 22f * s)
            quadTo(centerX, baseY - 44f * s, centerX + 20f * s, baseY - 22f * s)
            lineTo(centerX + 20f * s, baseY)
        }
        canvas.drawPath(archPath, strokePaint)
    }

    private fun drawCrescentFinial(
        canvas: Canvas,
        centerX: Float,
        apexY: Float,
        s: Float,
        strokePaint: Paint
    ) {
        canvas.drawLine(centerX, apexY, centerX, apexY - 11f * s, strokePaint)
        val crescentRect = RectF(
            centerX - 9f * s,
            apexY - 25f * s,
            centerX + 9f * s,
            apexY - 7f * s
        )
        canvas.drawArc(crescentRect, 35f, 250f, false, strokePaint)
    }

    private fun drawOrnamentalDivider(
        canvas: Canvas,
        startX: Float,
        endX: Float,
        y: Float,
        goldColor: Int
    ) {
        val midX = (startX + endX) / 2f
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = goldColor
            strokeWidth = 2.2f
            alpha = 190
        }
        val diamondPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = goldColor
            style = Paint.Style.FILL
        }
        canvas.drawLine(startX, y, midX - 20f, y, linePaint)
        canvas.drawLine(midX + 20f, y, endX, y, linePaint)
        val diamond = Path().apply {
            moveTo(midX, y - 8f)
            lineTo(midX + 10f, y)
            lineTo(midX, y + 8f)
            lineTo(midX - 10f, y)
            close()
        }
        canvas.drawPath(diamond, diamondPaint)
    }

    private data class MeasuredBodyLayouts(
        val cardVerticalPad: Float,
        val badgeBottomGap: Float,
        val arabicToBodyGap: Float,
        val bodyToSourceGap: Float,
        val betweenCardsGap: Float,
        val hadithBodySizePx: Float,
        val ayahArabicSizePx: Float,
        val ayahBodySizePx: Float,
        val hadithBadgeLayout: StaticLayout,
        val hadithArabicLayout: StaticLayout,
        val hadithBodyLayout: StaticLayout,
        val hadithSourceLayout: StaticLayout,
        val ayahBadgeLayout: StaticLayout,
        val ayahArabicLayout: StaticLayout,
        val ayahBodyLayout: StaticLayout,
        val ayahSourceLayout: StaticLayout,
        val hadithCardHeight: Float,
        val ayahCardHeight: Float,
        val totalHeight: Float
    )

    private fun measureBodyLayouts(
        contentWidth: Int,
        isStory: Boolean,
        scale: Float,
        baseHadithArabicSize: Float,
        baseHadithBodySize: Float,
        baseAyahArabicSize: Float,
        baseAyahBodySize: Float,
        baseSourceSize: Float,
        baseBadgeSize: Float,
        hadithHeader: String,
        hadithArabic: String,
        hadithBody: String,
        hadithSource: String,
        ayahHeader: String,
        ayahArabic: String,
        ayahBody: String,
        ayahSource: String,
        banglaTypeface: Typeface,
        banglaBold: Typeface,
        arabicTypeface: Typeface,
        palette: CardPalette
    ): MeasuredBodyLayouts {
        val badgeSize = (baseBadgeSize * scale).coerceAtLeast(12f)
        val hadithArabicSize = (baseHadithArabicSize * scale).coerceAtLeast(14f)
        val hadithBodySize = (baseHadithBodySize * scale).coerceAtLeast(13f)
        val ayahArabicSize = (baseAyahArabicSize * scale).coerceAtLeast(15f)
        val ayahBodySize = (baseAyahBodySize * scale).coerceAtLeast(13f)
        val sourceSize = (baseSourceSize * scale).coerceAtLeast(11f)

        val badgePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.badgeText
            typeface = banglaBold
            textSize = badgeSize
        }
        val hadithArabicPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.arabicText
            typeface = arabicTypeface
            textSize = hadithArabicSize
        }
        val hadithBodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.bodyText
            typeface = banglaTypeface
            textSize = hadithBodySize
        }
        val ayahArabicPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.arabicText
            typeface = arabicTypeface
            textSize = ayahArabicSize
        }
        val ayahBodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.bodyText
            typeface = banglaTypeface
            textSize = ayahBodySize
        }
        val sourcePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.sourceText
            typeface = banglaBold
            textSize = sourceSize
        }

        val lineMultAr = if (scale < 0.60f) 1.15f else 1.25f
        val lineMultBody = if (scale < 0.60f) 1.14f else 1.22f

        val hadithBadgeLayout = buildStaticLayout(hadithHeader, badgePaint, contentWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f)
        val hadithArabicLayout = buildStaticLayout(hadithArabic, hadithArabicPaint, contentWidth, Layout.Alignment.ALIGN_CENTER, lineMultAr)
        val hadithBodyLayout = buildStaticLayout(hadithBody, hadithBodyPaint, contentWidth, Layout.Alignment.ALIGN_NORMAL, lineMultBody)
        val hadithSourceLayout = buildStaticLayout(hadithSource, sourcePaint, contentWidth, Layout.Alignment.ALIGN_OPPOSITE, 1.08f)

        val ayahBadgeLayout = buildStaticLayout(ayahHeader, badgePaint, contentWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f)
        val ayahArabicLayout = buildStaticLayout(ayahArabic, ayahArabicPaint, contentWidth, Layout.Alignment.ALIGN_CENTER, lineMultAr)
        val ayahBodyLayout = buildStaticLayout(ayahBody, ayahBodyPaint, contentWidth, Layout.Alignment.ALIGN_NORMAL, lineMultBody)
        val ayahSourceLayout = buildStaticLayout(ayahSource, sourcePaint, contentWidth, Layout.Alignment.ALIGN_OPPOSITE, 1.08f)

        val spacingScale = scale.coerceIn(0.42f, 1.0f)
        val cardVerticalPad = (if (isStory) 30f else 18f) * spacingScale
        val badgeBottomGap = (if (isStory) 18f else 9f) * spacingScale
        val arabicToBodyGap = (if (isStory) 18f else 9f) * spacingScale
        val bodyToSourceGap = (if (isStory) 14f else 8f) * spacingScale
        val betweenCardsGap = (if (isStory) 40f else 20f) * spacingScale

        val hadithCardHeight = cardVerticalPad * 2f +
            hadithBadgeLayout.height + 14f + badgeBottomGap +
            hadithArabicLayout.height + arabicToBodyGap +
            hadithBodyLayout.height + bodyToSourceGap +
            hadithSourceLayout.height

        val ayahCardHeight = cardVerticalPad * 2f +
            ayahBadgeLayout.height + 14f + badgeBottomGap +
            ayahArabicLayout.height + arabicToBodyGap +
            ayahBodyLayout.height + bodyToSourceGap +
            ayahSourceLayout.height

        return MeasuredBodyLayouts(
            cardVerticalPad = cardVerticalPad,
            badgeBottomGap = badgeBottomGap,
            arabicToBodyGap = arabicToBodyGap,
            bodyToSourceGap = bodyToSourceGap,
            betweenCardsGap = betweenCardsGap,
            hadithBodySizePx = hadithBodySize,
            ayahArabicSizePx = ayahArabicSize,
            ayahBodySizePx = ayahBodySize,
            hadithBadgeLayout = hadithBadgeLayout,
            hadithArabicLayout = hadithArabicLayout,
            hadithBodyLayout = hadithBodyLayout,
            hadithSourceLayout = hadithSourceLayout,
            ayahBadgeLayout = ayahBadgeLayout,
            ayahArabicLayout = ayahArabicLayout,
            ayahBodyLayout = ayahBodyLayout,
            ayahSourceLayout = ayahSourceLayout,
            hadithCardHeight = hadithCardHeight,
            ayahCardHeight = ayahCardHeight,
            totalHeight = hadithCardHeight + betweenCardsGap + ayahCardHeight
        )
    }

    private fun buildStaticLayout(
        text: String,
        paint: TextPaint,
        widthPx: Int,
        alignment: Layout.Alignment,
        lineSpacingMult: Float
    ): StaticLayout {
        return StaticLayout.Builder.obtain(text, 0, text.length, paint, widthPx)
            .setAlignment(alignment)
            .setLineSpacing(0f, lineSpacingMult)
            .setIncludePad(false)
            .build()
    }

    private fun drawBadgePill(
        canvas: Canvas,
        x: Float,
        y: Float,
        layout: StaticLayout,
        pillPaint: Paint,
        borderGold: Int
    ) {
        val textWidth = if (layout.lineCount > 0) layout.getLineWidth(0) else 120f
        val pillRect = RectF(
            x,
            y,
            x + textWidth + 34f,
            y + layout.height + 16f
        )
        canvas.drawRoundRect(pillRect, 14f, 14f, pillPaint)
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.6f
            color = borderGold
            alpha = 140
        }
        canvas.drawRoundRect(pillRect, 14f, 14f, borderPaint)
        drawLayoutAt(canvas, layout, x + 17f, y + 8f)
    }

    private fun drawLayoutAt(canvas: Canvas, layout: StaticLayout, x: Float, y: Float) {
        canvas.save()
        canvas.translate(x, y)
        layout.draw(canvas)
        canvas.restore()
    }

    fun isPackageInstalled(context: Context, packageName: String): Boolean {
        return try {
            val pm = context.packageManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(packageName, 0)
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    fun getInstalledShareTargets(context: Context): List<DirectShareTarget> {
        return DirectShareTarget.entries.filter { target ->
            resolveInstalledPackage(context, target) != null
        }
    }

    fun resolveInstalledPackage(context: Context, target: DirectShareTarget): String? {
        return target.candidatePackages.firstOrNull { pkg -> isPackageInstalled(context, pkg) }
    }

    fun saveCardToCacheAndGetUri(
        context: Context,
        bitmap: Bitmap,
        dateKey: String,
        format: ShareCardFormat
    ): Uri {
        val dir = File(context.cacheDir, "shared_cards").apply { mkdirs() }
        dir.listFiles()?.forEach { old ->
            if (old.isFile && System.currentTimeMillis() - old.lastModified() > 24 * 60 * 60 * 1000L) {
                old.delete()
            }
        }
        val file = File(dir, "nur_bangla_${dateKey}_${format.name.lowercase()}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            out.flush()
        }
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    /**
     * Saves the rendered PNG card directly to the device Gallery (Pictures/NurBangla)
     * via MediaStore without requiring any storage permissions on Android 10+ (API 29+).
     */
    fun saveCardPngToGallery(
        context: Context,
        bitmap: Bitmap,
        dateKey: String,
        format: ShareCardFormat
    ): Result<Uri> {
        return runCatching {
            val fileName = "NurBangla_${dateKey}_${format.name.lowercase()}_${System.currentTimeMillis() % 10000}.png"
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(
                        MediaStore.Images.Media.RELATIVE_PATH,
                        "${Environment.DIRECTORY_PICTURES}/NurBangla"
                    )
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: error("Could not create MediaStore entry for share card")
            resolver.openOutputStream(uri)?.use { stream ->
                val ok = bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                stream.flush()
                if (!ok) error("PNG compression failed")
            } ?: error("Could not open MediaStore output stream")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val doneValues = ContentValues().apply {
                    put(MediaStore.Images.Media.IS_PENDING, 0)
                }
                resolver.update(uri, doneValues, null, null)
            }
            uri
        }
    }

    /**
     * Saves PNG card to Gallery AND copies the full bilingual caption (including APP_DOWNLOAD_URL)
     * to clipboard so the user can post manually from any Facebook account, page, or group.
     */
    fun saveCardAndCopyCaption(
        context: Context,
        bitmap: Bitmap,
        entry: DailyInspirationEntry,
        isEnglish: Boolean,
        format: ShareCardFormat
    ): Result<Uri> {
        val saveRes = saveCardPngToGallery(context, bitmap, entry.dateKey, format)
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(
            ClipData.newPlainText("Nur Bangla Share Caption", entry.formatShareText(isEnglish))
        )
        return saveRes
    }

    /**
     * Opens the Facebook app (com.facebook.katana) so the user can switch accounts inside Facebook,
     * falling back to https://www.facebook.com in the browser if the Facebook app is not installed.
     */
    fun openFacebookAppToSwitchAccount(context: Context): Boolean {
        val pm = context.packageManager
        val launchIntent = pm.getLaunchIntentForPackage("com.facebook.katana")?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (launchIntent != null) {
            return runCatching {
                context.startActivity(launchIntent)
                true
            }.getOrDefault(false)
        }
        return runCatching {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.facebook.com")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webIntent)
            true
        }.getOrDefault(false)
    }

    fun shareCardImage(
        context: Context,
        entry: DailyInspirationEntry,
        isEnglish: Boolean,
        format: ShareCardFormat,
        theme: ShareCardTheme,
        fontSize: ShareCardFontSize = ShareCardFontSize.MEDIUM
    ): Result<Unit> {
        return runCatching {
            val rendered = renderDailyCard(context, entry, isEnglish, format, theme, fontSize)
            val uri = saveCardToCacheAndGetUri(context, rendered.bitmap, entry.dateKey, format)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, entry.formatShareText(isEnglish))
                clipData = ClipData.newUri(context.contentResolver, "Nur Bangla Share Card", uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(
                shareIntent,
                if (isEnglish) "Share Daily Hadith & Ayah Card" else "দৈনিক হাদিস ও আয়াত কার্ড শেয়ার করুন"
            ).apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(chooser)
        }
    }

    fun shareCardToDirectTarget(
        context: Context,
        target: DirectShareTarget,
        entry: DailyInspirationEntry,
        isEnglish: Boolean,
        format: ShareCardFormat,
        theme: ShareCardTheme,
        fontSize: ShareCardFontSize = ShareCardFontSize.MEDIUM
    ): Result<Unit> {
        return runCatching {
            val targetPackage = resolveInstalledPackage(context, target)
                ?: error("${target.labelEn} is not installed")
            val effectiveFormat = if (target.storyAction != null) ShareCardFormat.STORY else format
            val rendered = renderDailyCard(context, entry, isEnglish, effectiveFormat, theme, fontSize)
            val uri = saveCardToCacheAndGetUri(context, rendered.bitmap, entry.dateKey, effectiveFormat)

            val pm = context.packageManager

            if (target.storyAction != null) {
                context.grantUriPermission(targetPackage, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                val storyIntent = Intent(target.storyAction).apply {
                    setPackage(targetPackage)
                    setDataAndType(uri, "image/png")
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

            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                setPackage(targetPackage)
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, entry.formatShareText(isEnglish))
                clipData = ClipData.newUri(context.contentResolver, "Nur Bangla Share Card", uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (sendIntent.resolveActivity(pm) != null) {
                context.startActivity(sendIntent)
            } else {
                shareCardImage(context, entry, isEnglish, effectiveFormat, theme, fontSize).getOrThrow()
            }
        }
    }

    fun sharePlainText(
        context: Context,
        entry: DailyInspirationEntry,
        isEnglish: Boolean
    ): Result<Unit> {
        return runCatching {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, entry.formatShareText(isEnglish))
            }
            val chooser = Intent.createChooser(
                sendIntent,
                if (isEnglish) "Share Daily Hadith & Ayah Text" else "হাদিস ও আয়াত টেক্সট শেয়ার করুন"
            )
            context.startActivity(chooser)
        }
    }
}
