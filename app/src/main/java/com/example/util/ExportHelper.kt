package com.example.util

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.data.local.PrayerLogEntity
import com.example.data.model.PrayerStatus
import java.io.OutputStream
import java.nio.charset.StandardCharsets

object ExportHelper {

    fun writePrayerLogsCsv(outputStream: OutputStream, logs: List<PrayerLogEntity>) {
        // UTF-8 BOM for proper Bangla display in Microsoft Excel / Google Sheets
        outputStream.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
        val sb = StringBuilder()
        sb.appendLine("তারিখ,ফজর,যোহর,আসর,মাগরিব,এশা,নোট")
        for (log in logs.sortedByDescending { it.dateKey }) {
            val dateStr = BanglaUtils.formatDateKeyBangla(log.dateKey)
            val f = PrayerStatus.safeValueOf(log.fajrStatus).titleBn
            val d = PrayerStatus.safeValueOf(log.dhuhrStatus).titleBn
            val a = PrayerStatus.safeValueOf(log.asrStatus).titleBn
            val m = PrayerStatus.safeValueOf(log.maghribStatus).titleBn
            val i = PrayerStatus.safeValueOf(log.ishaStatus).titleBn
            val note = log.note.replace("\"", "\"\"").replace("\n", " ")
            sb.appendLine("\"$dateStr\",\"$f\",\"$d\",\"$a\",\"$m\",\"$i\",\"$note\"")
        }
        outputStream.write(sb.toString().toByteArray(StandardCharsets.UTF_8))
        outputStream.flush()
    }

    fun writePrayerLogsPdf(
        outputStream: OutputStream,
        logs: List<PrayerLogEntity>,
        userName: String
    ) {
        val pdfDocument = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val margin = 40f

        val titlePaint = Paint().apply {
            color = Color.rgb(11, 110, 79)
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val subtitlePaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 11f
            isAntiAlias = true
        }
        val headerPaint = Paint().apply {
            color = Color.BLACK
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val bodyPaint = Paint().apply {
            color = Color.rgb(35, 35, 35)
            textSize = 10.5f
            isAntiAlias = true
        }
        val linePaint = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 1f
        }

        val sorted = logs.sortedByDescending { it.dateKey }
        val rowsPerPage = 28
        val totalPages = if (sorted.isEmpty()) 1 else (sorted.size + rowsPerPage - 1) / rowsPerPage

        for (pageIndex in 0 until totalPages) {
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex + 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            var y = 52f
            canvas.drawText("নূর বাংলা — সালাত ট্র্যাকার রিপোর্ট", margin, y, titlePaint)
            y += 20f
            canvas.drawText("ব্যবহারকারী: $userName  |  পৃষ্ঠা ${BanglaUtils.toBanglaNumber(pageIndex + 1)}/${BanglaUtils.toBanglaNumber(totalPages)}", margin, y, subtitlePaint)
            y += 18f
            canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
            y += 20f

            // Table headers
            canvas.drawText("তারিখ", margin, y, headerPaint)
            canvas.drawText("ফজর", margin + 125f, y, headerPaint)
            canvas.drawText("যোহর", margin + 200f, y, headerPaint)
            canvas.drawText("আসর", margin + 275f, y, headerPaint)
            canvas.drawText("মাগরিব", margin + 350f, y, headerPaint)
            canvas.drawText("এশা", margin + 435f, y, headerPaint)
            y += 8f
            canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
            y += 18f

            if (sorted.isEmpty()) {
                canvas.drawText("কোনো নামাজের রেকর্ড পাওয়া যায়নি।", margin, y, bodyPaint)
            } else {
                val start = pageIndex * rowsPerPage
                val end = (start + rowsPerPage).coerceAtMost(sorted.size)
                for (idx in start until end) {
                    val item = sorted[idx]
                    canvas.drawText(BanglaUtils.formatDateKeyBangla(item.dateKey), margin, y, bodyPaint)
                    canvas.drawText(PrayerStatus.safeValueOf(item.fajrStatus).titleBn, margin + 125f, y, bodyPaint)
                    canvas.drawText(PrayerStatus.safeValueOf(item.dhuhrStatus).titleBn, margin + 200f, y, bodyPaint)
                    canvas.drawText(PrayerStatus.safeValueOf(item.asrStatus).titleBn, margin + 275f, y, bodyPaint)
                    canvas.drawText(PrayerStatus.safeValueOf(item.maghribStatus).titleBn, margin + 350f, y, bodyPaint)
                    canvas.drawText(PrayerStatus.safeValueOf(item.ishaStatus).titleBn, margin + 435f, y, bodyPaint)
                    y += 22f
                }
            }

            pdfDocument.finishPage(page)
        }

        pdfDocument.writeTo(outputStream)
        outputStream.flush()
        pdfDocument.close()
    }
}
