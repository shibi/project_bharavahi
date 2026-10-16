package com.bharavahi.org.ui.common

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.content.FileProvider
import com.bharavahi.core.domain.rules.EventPdfReport
import com.bharavahi.core.domain.rules.ReportTable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** ORG-34: writes [report] as a PDF in the app's cache and opens the share sheet with it. */
suspend fun sharePdf(context: Context, report: EventPdfReport, chooserTitle: String) {
    val file = withContext(Dispatchers.IO) {
        File(context.cacheDir, REPORTS_DIR).apply { mkdirs() }.resolve(report.fileName).also { EventPdfWriter(report).writeTo(it) }
    }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, report.title)
        // ClipData lets the chooser's preview and the receiving app read the file too.
        clipData = ClipData.newRawUri(report.fileName, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(send, chooserTitle))
}

private const val REPORTS_DIR = "reports" // matches res/xml/file_paths.xml

/**
 * Draws the report on A4 landscape pages, styled like the sheet: a green title bar per schedule, a blue
 * header and totals row, grey borders. A table that runs past the page carries on with its header repeated.
 */
private class EventPdfWriter(private val report: EventPdfReport) {
    private val doc = PdfDocument()
    private var page: PdfDocument.Page? = null
    private var pageNumber = 0
    private var y = 0f

    private val titlePaint = textPaint(18f, bold = true)
    private val subtitlePaint = textPaint(10f, color = MUTED)
    private val labelPaint = textPaint(9f, color = MUTED)
    private val valuePaint = textPaint(11f, bold = true)
    private val tableTitlePaint = textPaint(10f, bold = true, color = Color.WHITE)
    private val headerPaint = textPaint(7.5f, bold = true)
    private val cellPaint = textPaint(8.5f)
    private val totalsPaint = textPaint(8.5f, bold = true)
    private val footerPaint = textPaint(8f, color = MUTED)
    private val fill = Paint()
    private val border = Paint().apply {
        color = BORDER_GREY
        style = Paint.Style.STROKE
        strokeWidth = 0.5f
    }

    private val canvas: Canvas get() = page!!.canvas

    fun writeTo(file: File) {
        try {
            newPage()
            drawSummary()
            report.tables.forEach { drawTable(it) }
            finishPage()
            file.outputStream().use { doc.writeTo(it) }
        } finally {
            doc.close()
        }
    }

    private fun drawSummary() {
        canvas.drawText(report.title, MARGIN, y + 18f, titlePaint)
        y += 24f
        if (report.subtitle.isNotEmpty()) {
            canvas.drawText(report.subtitle, MARGIN, y + 10f, subtitlePaint)
            y += 16f
        }
        y += 6f
        val width = CONTENT_WIDTH / report.summary.size.coerceAtLeast(1)
        report.summary.forEachIndexed { i, (label, value) ->
            val x = MARGIN + i * width
            canvas.drawText(label, x, y + 9f, labelPaint)
            canvas.drawText(value, x, y + 24f, valuePaint)
        }
        y += 40f
    }

    private fun drawTable(table: ReportTable) {
        val widths = columnWidths(table.header.size)
        // Keep the title, header and at least one row together.
        if (y + GAP + TITLE_HEIGHT + HEADER_HEIGHT + ROW_HEIGHT > BOTTOM) newPage() else y += GAP
        fillRow(TITLE_HEIGHT, TITLE_GREEN)
        canvas.drawRect(MARGIN, y, MARGIN + CONTENT_WIDTH, y + TITLE_HEIGHT, border)
        drawCell(table.title, MARGIN, CONTENT_WIDTH, TITLE_HEIGHT, tableTitlePaint, Paint.Align.CENTER)
        y += TITLE_HEIGHT
        drawHeader(table.header, widths)
        table.rows.forEach { row ->
            if (y + ROW_HEIGHT > BOTTOM) {
                newPage()
                drawHeader(table.header, widths)
            }
            drawRow(row, widths, ROW_HEIGHT, cellPaint)
        }
        if (y + ROW_HEIGHT > BOTTOM) {
            newPage()
            drawHeader(table.header, widths)
        }
        fillRow(ROW_HEIGHT, TOTALS_BLUE)
        drawRow(table.totals, widths, ROW_HEIGHT, totalsPaint)
    }

    /** Due headers come as up to two lines ("SEPTEMBER\n2026") so they fit the narrow due columns. */
    private fun drawHeader(header: List<String>, widths: List<Float>) {
        fillRow(HEADER_HEIGHT, HEADER_BLUE)
        var x = MARGIN
        header.forEachIndexed { i, label ->
            canvas.drawRect(x, y, x + widths[i], y + HEADER_HEIGHT, border)
            val lines = label.split("\n", limit = 2)
            val lineHeight = headerPaint.textSize + 2f
            val top = y + (HEADER_HEIGHT - lines.size * lineHeight) / 2
            lines.forEachIndexed { l, line ->
                val text = ellipsize(line, headerPaint, widths[i] - 2 * PAD)
                canvas.drawText(text, x + widths[i] / 2, top + (l + 1) * lineHeight - 2f, headerPaint.aligned(Paint.Align.CENTER))
            }
            x += widths[i]
        }
        y += HEADER_HEIGHT
    }

    private fun drawRow(cells: List<String>, widths: List<Float>, height: Float, paint: TextPaint) {
        var x = MARGIN
        cells.forEachIndexed { i, text ->
            canvas.drawRect(x, y, x + widths[i], y + height, border)
            val align = when (i) {
                0 -> Paint.Align.CENTER
                1 -> Paint.Align.LEFT
                else -> Paint.Align.RIGHT
            }
            drawCell(text, x, widths[i], height, paint, align)
            x += widths[i]
        }
        y += height
    }

    private fun drawCell(text: String, x: Float, width: Float, height: Float, paint: TextPaint, align: Paint.Align) {
        val fitted = ellipsize(text, paint, width - 2 * PAD)
        val textX = when (align) {
            Paint.Align.LEFT -> x + PAD
            Paint.Align.CENTER -> x + width / 2
            Paint.Align.RIGHT -> x + width - PAD
        }
        val baseline = y + height / 2 - (paint.descent() + paint.ascent()) / 2
        canvas.drawText(fitted, textX, baseline, paint.aligned(align))
    }

    private fun fillRow(height: Float, color: Int) {
        fill.color = color
        canvas.drawRect(MARGIN, y, MARGIN + CONTENT_WIDTH, y + height, fill)
    }

    /** NO is narrow, NAME gets the room of two and a half amount columns, the rest share what's left. */
    private fun columnWidths(count: Int): List<Float> {
        val amountColumns = (count - 2).coerceAtLeast(0)
        val unit = (CONTENT_WIDTH - NO_WIDTH) / (NAME_WEIGHT + amountColumns)
        return listOf(NO_WIDTH, unit * NAME_WEIGHT) + List(amountColumns) { unit }
    }

    private fun ellipsize(text: String, paint: TextPaint, width: Float): String =
        TextUtils.ellipsize(text, paint, width.coerceAtLeast(0f), TextUtils.TruncateAt.END).toString()

    private fun newPage() {
        finishPage()
        pageNumber++
        page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
        y = MARGIN
    }

    private fun finishPage() {
        val current = page ?: return
        current.canvas.drawText("${report.title} · page $pageNumber", MARGIN + CONTENT_WIDTH, PAGE_HEIGHT - MARGIN / 2, footerPaint.aligned(Paint.Align.RIGHT))
        doc.finishPage(current)
        page = null
    }

    private fun textPaint(size: Float, bold: Boolean = false, color: Int = Color.BLACK) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = size
        this.color = color
        typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
    }

    private fun TextPaint.aligned(align: Paint.Align): TextPaint = apply { textAlign = align }

    private companion object {
        // A4 landscape in PostScript points.
        const val PAGE_WIDTH = 842
        const val PAGE_HEIGHT = 595
        const val MARGIN = 32f
        const val CONTENT_WIDTH = PAGE_WIDTH - 2 * MARGIN
        const val BOTTOM = PAGE_HEIGHT - MARGIN - 8f
        const val GAP = 16f
        const val TITLE_HEIGHT = 20f
        const val HEADER_HEIGHT = 24f
        const val ROW_HEIGHT = 16f
        const val PAD = 4f
        const val NO_WIDTH = 28f
        const val NAME_WEIGHT = 2.5f

        // The sheet's colours (GoogleSheetsRestGateway).
        val TITLE_GREEN = Color.rgb(0x6A, 0xA8, 0x4F)
        val HEADER_BLUE = Color.rgb(0xCF, 0xE2, 0xF3)
        val TOTALS_BLUE = Color.rgb(0xC9, 0xDA, 0xF8)
        val BORDER_GREY = Color.rgb(0xBF, 0xBF, 0xBF)
        val MUTED = Color.rgb(0x5F, 0x63, 0x68)
    }
}
