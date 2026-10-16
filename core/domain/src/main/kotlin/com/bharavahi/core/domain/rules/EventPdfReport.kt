package com.bharavahi.core.domain.rules

import com.bharavahi.core.domain.model.Event
import com.bharavahi.core.domain.model.EventMember
import com.bharavahi.core.domain.model.Schedule
import com.bharavahi.core.domain.usecase.event.ObserveExpectedTotalUseCase
import com.bharavahi.core.domain.validation.Money
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** ORG-34: what the event PDF shows, already formatted. The app draws it; amounts are text. */
data class EventPdfReport(
    val fileName: String,
    val title: String,
    val subtitle: String,
    val summary: List<Pair<String, String>>,
    val tables: List<ReportTable>,
)

/** One schedule, laid out like its sheet tab (ORG-37): header, one row per member, then totals. */
data class ReportTable(
    val title: String,
    val header: List<String>,
    val rows: List<List<String>>,
    val totals: List<String>,
)

object EventPdfReportBuilder {

    fun build(
        communityName: String,
        event: Event,
        schedules: List<Schedule>,
        members: List<EventMember>,
        today: LocalDate,
    ): EventPdfReport = EventPdfReport(
        fileName = "${event.name.fileSafe()}-dues.pdf",
        title = event.name.trim(),
        subtitle = listOf(communityName.trim(), today.format(DATE)).filter { it.isNotEmpty() }.joinToString(" · "),
        summary = listOf(
            "Opening balance" to Money.format(event.openingBalance),
            "Schedule total" to Money.format(ObserveExpectedTotalUseCase.expectedTotal(members, schedules)),
            "Collected" to Money.format(event.totalCollected),
            "Expenses" to Money.format(event.totalExpenses),
            "Balance" to Money.format(event.balance),
        ),
        tables = schedules.flatMap { tables(event.name, it, members) },
    )

    /** Most due columns one table holds; past this the amounts no longer fit an A4 landscape row. */
    const val MAX_DUES_PER_TABLE = 10

    /**
     * Same rows as the sheet: every member (disabled ones too, BR-10), sorted by name; a blank cell is unpaid.
     * A schedule with more than [MAX_DUES_PER_TABLE] dues is split into evenly sized parts, each repeating
     * NO and NAME with its own totals row; the member's TOTAL column goes on the last part.
     */
    private fun tables(eventName: String, schedule: Schedule, members: List<EventMember>): List<ReportTable> {
        val dues = schedule.dues.sortedBy { it.index }
        val sorted = members.sortedBy { it.name.lowercase() }
        val paid = sorted.map { member -> dues.map { member.paidOn(schedule.id, it.index) } }
        val headers = ScheduleSheetBuilder.dueHeaders(dues, yearSeparator = "\n") // two header lines in the PDF
        val title = ScheduleSheetBuilder.tableTitle(eventName, schedule)
        val parts = ((dues.size + MAX_DUES_PER_TABLE - 1) / MAX_DUES_PER_TABLE).coerceAtLeast(1)
        val chunks = if (parts == 1) listOf(dues.indices.toList()) else dues.indices.chunked((dues.size + parts - 1) / parts)
        return chunks.mapIndexed { part, cols ->
            val last = part == chunks.lastIndex
            ReportTable(
                title = if (chunks.size == 1) title else "$title · DUES ${cols.first() + 1}–${cols.last() + 1} OF ${dues.size}",
                header = listOf("NO", "NAME") + cols.map { headers[it] } + if (last) listOf("TOTAL") else emptyList(),
                rows = sorted.mapIndexed { i, member ->
                    listOf((i + 1).toString(), member.name.trim().uppercase()) +
                        cols.map { d -> paid[i][d].let { if (it == 0L) "" else Money.format(it) } } +
                        if (last) listOf(Money.format(paid[i].sum())) else emptyList()
                },
                totals = listOf("", "TOTAL") +
                    cols.map { d -> Money.format(paid.sumOf { it[d] }) } +
                    if (last) listOf(Money.format(paid.sumOf { it.sum() })) else emptyList(),
            )
        }
    }

    private fun String.fileSafe(): String = replace(Regex("[^A-Za-z0-9]+"), "-").trim('-').ifEmpty { "event" }

    private val DATE = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)}
