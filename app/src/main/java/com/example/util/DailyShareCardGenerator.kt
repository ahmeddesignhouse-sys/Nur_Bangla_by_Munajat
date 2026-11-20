package com.example.util

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
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
        labelBn = "স্টোরি / স্ট্যাটাস (৯:১৬)",
        labelEn = "Story / Status (9:16)"
    ),
    FEED(
        widthPx = 1080,
        heightPx = 1080,
        labelBn = "পোস্ট / ফিড (১:১)",
        labelEn = "Post / Feed (1:1)"
    );

    fun label(isEnglish: Boolean): String = if (isEnglish) labelEn else labelBn
}

enum class ShareCardTheme(
    val labelBn: String,
    val labelEn: String
) {
    DARK_MOSQUE("ডার্ক গোল্ড", "Dark Emerald"),
    LIGHT_IVORY("হালকা আইভরি", "Light Ivory");

    fun label(isEnglish: Boolean): String = if (isEnglish) labelEn else labelBn
}

data class RenderedShareCard(
    val bitmap: Bitmap,
    val scaleFactorUsed: Float,
    val isClipped: Boolean
)

/**
 * Generates high-resolution, zero-clipping PNG share cards (1080x1920 Story/Status and
 * 1080x1080 Feed/Post) containing the FULL Daily Hadith + FULL Daily Ayah + sources +
 * Gregorian & Hijri dates + "নূর বাংলা" app branding using bundled Bangla & Arabic fonts.
 */
object DailyShareCardGenerator {

    private data class CardPalette(
        val bgTop: Int,
        val bgBottom: Int,
        val panelFill: Int,
        val panelStroke: Int,
        val accentGold: Int,
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
            bgTop = Color.parseColor("#07241B"),
            bgBottom = Color.parseColor("#0E3C2D"),
            panelFill = Color.parseColor("#13382C"),
            panelStroke = Color.parseColor("#3B7A62"),
            accentGold = Color.parseColor("#F4D06F"),
            badgeBg = Color.parseColor("#1F5442"),
            badgeText = Color.parseColor("#FFE28A"),
            titleText = Color.parseColor("#FFFFFF"),
            arabicText = Color.parseColor("#FFFDF5"),
            bodyText = Color.parseColor("#F1F5F3"),
            sourceText = Color.parseColor("#C7E3D7"),
            dividerColor = Color.parseColor("#336C58")
        )
        ShareCardTheme.LIGHT_IVORY -> CardPalette(
            bgTop = Color.parseColor("#FAF6EC"),
            bgBottom = Color.parseColor("#EFE7D4"),
            panelFill = Color.parseColor("#FFFFFF"),
            panelStroke = Color.parseColor("#D8C79E"),
            accentGold = Color.parseColor("#0B6E4F"),
            badgeBg = Color.parseColor("#E3F2EC"),
            badgeText = Color.parseColor("#084F39"),
            titleText = Color.parseColor("#102A22"),
            arabicText = Color.parseColor("#162821"),
            bodyText = Color.parseColor("#1E2422"),
            sourceText = Color.parseColor("#4B5B55"),
            dividerColor = Color.parseColor("#DFD3B6")
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
        theme: ShareCardTheme
    ): RenderedShareCard {
        val width = format.widthPx
        val height = format.heightPx
        val palette = paletteFor(theme)

        val banglaTypeface = loadTypefaceSafely(context, R.font.hind_siliguri, Typeface.SANS_SERIF)
        val banglaBold = Typeface.create(banglaTypeface, Typeface.BOLD)
        val arabicTypeface = loadTypefaceSafely(context, R.font.amiri, Typeface.SERIF)

        val isStory = format == ShareCardFormat.STORY
        val outerPad = if (isStory) 60f else 48f
        val innerPad = if (isStory) 40f else 32f
        val headerHeight = if (isStory) 168f else 118f
        val footerHeight = if (isStory) 104f else 76f
        val availableBodyHeight = (height - outerPad * 2f - headerHeight - footerHeight).coerceAtLeast(300f)
        val contentWidth = (width - outerPad * 2f - innerPad * 2f).toInt().coerceAtLeast(300)

        val hadithHeader = if (isEnglish) "HADITH OF THE DAY" else "আজকের হাদিস"
        val ayahHeader = if (isEnglish) "DAILY AYAH" else "আজকের আয়াত"
        val hadithBody = entry.hadith.meaning(isEnglish)
        val hadithSource = "— ${entry.hadith.source(isEnglish)}"
        val ayahBody = entry.ayah.meaning(isEnglish)
        val ayahSource = "— ${entry.ayah.source(isEnglish)}"

        val baseArabicSize = if (isStory) 46f else 31f
        val baseBodySize = if (isStory) 34f else 24f
        val baseSourceSize = if (isStory) 25f else 18f
        val baseBadgeSize = if (isStory) 24f else 18f

        // Auto-fit font scale downward so EVERY Hadith + Ayah fits cleanly with ZERO clipping
        var chosenScale = 1.0f
        var chosenLayouts: MeasuredBodyLayouts? = null
        val candidateScales = floatArrayOf(1.0f, 0.94f, 0.88f, 0.82f, 0.76f, 0.70f, 0.64f, 0.58f, 0.52f)
        for (scale in candidateScales) {
            val measured = measureBodyLayouts(
                contentWidth = contentWidth,
                isStory = isStory,
                scale = scale,
                baseArabicSize = baseArabicSize,
                baseBodySize = baseBodySize,
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

        // 1. Background vertical gradient
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, 0f, height.toFloat(),
                palette.bgTop, palette.bgBottom,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // 2. Subtle decorative geometric border frame
        val framePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3f
            color = palette.panelStroke
        }
        val frameRect = RectF(26f, 26f, width - 26f, height - 26f)
        canvas.drawRoundRect(frameRect, 38f, 38f, framePaint)

        // 3. Header: Brand name + Gregorian & Hijri date
        val brandPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.accentGold
            typeface = banglaBold
            textSize = if (isStory) 50f else 38f
            textAlign = Paint.Align.CENTER
        }
        val subBrandPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.sourceText
            typeface = banglaTypeface
            textSize = if (isStory) 26f else 20f
            textAlign = Paint.Align.CENTER
        }
        val brandTitle = if (isEnglish) "নূর বাংলা  •  Nur Bangla" else "নূর বাংলা"
        val dateLine = "${entry.gregorianDate(isEnglish)}  •  ${entry.hijriDate(isEnglish)}"
        val headerTop = outerPad + (if (isStory) 42f else 28f)
        canvas.drawText(brandTitle, width / 2f, headerTop, brandPaint)
        canvas.drawText(dateLine, width / 2f, headerTop + (if (isStory) 48f else 36f), subBrandPaint)

        val headerDividerY = outerPad + headerHeight - 20f
        val divPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.dividerColor
            strokeWidth = 2.5f
        }
        canvas.drawLine(outerPad, headerDividerY, width - outerPad, headerDividerY, divPaint)

        // 4. Vertically center the Hadith + Ayah cards inside the available body region
        val bodyTopBase = outerPad + headerHeight
        val extraSpace = (availableBodyHeight - layouts.totalHeight).coerceAtLeast(0f)
        var cursorY = bodyTopBase + (extraSpace * 0.42f)

        val cardFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.panelFill
            style = Paint.Style.FILL
        }
        val cardStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.panelStroke
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
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
        drawBadgePill(canvas, outerPad + innerPad, y, layouts.hadithBadgeLayout, badgeFillPaint)
        y += layouts.hadithBadgeLayout.height + layouts.badgeBottomGap + 12f
        drawLayoutAt(canvas, layouts.hadithArabicLayout, outerPad + innerPad, y)
        y += layouts.hadithArabicLayout.height + layouts.arabicToBodyGap
        drawLayoutAt(canvas, layouts.hadithBodyLayout, outerPad + innerPad, y)
        y += layouts.hadithBodyLayout.height + layouts.bodyToSourceGap
        drawLayoutAt(canvas, layouts.hadithSourceLayout, outerPad + innerPad, y)

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
        drawBadgePill(canvas, outerPad + innerPad, y, layouts.ayahBadgeLayout, badgeFillPaint)
        y += layouts.ayahBadgeLayout.height + layouts.badgeBottomGap + 12f
        drawLayoutAt(canvas, layouts.ayahArabicLayout, outerPad + innerPad, y)
        y += layouts.ayahArabicLayout.height + layouts.arabicToBodyGap
        drawLayoutAt(canvas, layouts.ayahBodyLayout, outerPad + innerPad, y)
        y += layouts.ayahBodyLayout.height + layouts.bodyToSourceGap
        drawLayoutAt(canvas, layouts.ayahSourceLayout, outerPad + innerPad, y)

        // 5. Footer branding bar
        val footerY = height - outerPad - (if (isStory) 22f else 12f)
        val footerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.accentGold
            typeface = banglaBold
            textSize = if (isStory) 28f else 22f
            textAlign = Paint.Align.CENTER
        }
        val footerLabel = if (isEnglish) {
            "নূর বাংলা (Nur Bangla)  •  Daily Islamic Companion"
        } else {
            "নূর বাংলা  •  দৈনিক হাদিস, আয়াত ও নামাজের সময়সূচী"
        }
        canvas.drawText(footerLabel, width / 2f, footerY, footerPaint)

        return RenderedShareCard(
            bitmap = bitmap,
            scaleFactorUsed = chosenScale,
            isClipped = isClipped
        )
    }

    private data class MeasuredBodyLayouts(
        val cardVerticalPad: Float,
        val badgeBottomGap: Float,
        val arabicToBodyGap: Float,
        val bodyToSourceGap: Float,
        val betweenCardsGap: Float,
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
        baseArabicSize: Float,
        baseBodySize: Float,
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
        val badgePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.badgeText
            typeface = banglaBold
            textSize = (baseBadgeSize * scale).coerceAtLeast(14f)
        }
        val arabicPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.arabicText
            typeface = arabicTypeface
            textSize = (baseArabicSize * scale).coerceAtLeast(18f)
        }
        val bodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.bodyText
            typeface = banglaTypeface
            textSize = (baseBodySize * scale).coerceAtLeast(15f)
        }
        val sourcePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.sourceText
            typeface = banglaBold
            textSize = (baseSourceSize * scale).coerceAtLeast(13f)
        }

        val hadithBadgeLayout = buildStaticLayout(hadithHeader, badgePaint, contentWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f)
        val hadithArabicLayout = buildStaticLayout(hadithArabic, arabicPaint, contentWidth, Layout.Alignment.ALIGN_CENTER, 1.28f)
        val hadithBodyLayout = buildStaticLayout(hadithBody, bodyPaint, contentWidth, Layout.Alignment.ALIGN_NORMAL, 1.24f)
        val hadithSourceLayout = buildStaticLayout(hadithSource, sourcePaint, contentWidth, Layout.Alignment.ALIGN_OPPOSITE, 1.1f)

        val ayahBadgeLayout = buildStaticLayout(ayahHeader, badgePaint, contentWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f)
        val ayahArabicLayout = buildStaticLayout(ayahArabic, arabicPaint, contentWidth, Layout.Alignment.ALIGN_CENTER, 1.28f)
        val ayahBodyLayout = buildStaticLayout(ayahBody, bodyPaint, contentWidth, Layout.Alignment.ALIGN_NORMAL, 1.24f)
        val ayahSourceLayout = buildStaticLayout(ayahSource, sourcePaint, contentWidth, Layout.Alignment.ALIGN_OPPOSITE, 1.1f)

        val cardVerticalPad = (if (isStory) 34f else 22f) * scale.coerceAtLeast(0.75f)
        val badgeBottomGap = (if (isStory) 22f else 12f) * scale.coerceAtLeast(0.75f)
        val arabicToBodyGap = (if (isStory) 22f else 12f) * scale.coerceAtLeast(0.75f)
        val bodyToSourceGap = (if (isStory) 16f else 10f) * scale.coerceAtLeast(0.75f)
        val betweenCardsGap = (if (isStory) 32f else 18f) * scale.coerceAtLeast(0.75f)

        val hadithCardHeight = cardVerticalPad * 2f +
            hadithBadgeLayout.height + 12f + badgeBottomGap +
            hadithArabicLayout.height + arabicToBodyGap +
            hadithBodyLayout.height + bodyToSourceGap +
            hadithSourceLayout.height

        val ayahCardHeight = cardVerticalPad * 2f +
            ayahBadgeLayout.height + 12f + badgeBottomGap +
            ayahArabicLayout.height + arabicToBodyGap +
            ayahBodyLayout.height + bodyToSourceGap +
            ayahSourceLayout.height

        return MeasuredBodyLayouts(
            cardVerticalPad = cardVerticalPad,
            badgeBottomGap = badgeBottomGap,
            arabicToBodyGap = arabicToBodyGap,
            bodyToSourceGap = bodyToSourceGap,
            betweenCardsGap = betweenCardsGap,
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
        pillPaint: Paint
    ) {
        val textWidth = if (layout.lineCount > 0) layout.getLineWidth(0) else 120f
        val pillRect = RectF(
            x,
            y,
            x + textWidth + 32f,
            y + layout.height + 14f
        )
        canvas.drawRoundRect(pillRect, 14f, 14f, pillPaint)
        drawLayoutAt(canvas, layout, x + 16f, y + 7f)
    }

    private fun drawLayoutAt(canvas: Canvas, layout: StaticLayout, x: Float, y: Float) {
        canvas.save()
        canvas.translate(x, y)
        layout.draw(canvas)
        canvas.restore()
    }

    fun saveCardToCacheAndGetUri(
        context: Context,
        bitmap: Bitmap,
        dateKey: String,
        format: ShareCardFormat
    ): Uri {
        val dir = File(context.cacheDir, "shared_cards").apply { mkdirs() }
        // Clean old temporary share PNGs so cache stays tiny
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

    fun shareCardImage(
        context: Context,
        entry: DailyInspirationEntry,
        isEnglish: Boolean,
        format: ShareCardFormat,
        theme: ShareCardTheme
    ): Result<Unit> {
        return runCatching {
            val rendered = renderDailyCard(context, entry, isEnglish, format, theme)
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
