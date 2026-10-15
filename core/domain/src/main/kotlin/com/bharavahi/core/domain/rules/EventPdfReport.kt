package com.bharavahi.core.domain.rules

import com.bharavahi.core.domain.model.Event
import com.bharavahi.core.domain.model.EventMember
import com.bharavahi.core.domain.model.Schedule
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
            "Collected" to Money.format(event.totalCollected),
            "Expenses" to Money.format(event.totalExpenses),
            "Balance" to Money.format(event.balance),
        ),
        tables = schedules.map { table(event.name, it, members) },
    )

    /** Same rows as the sheet: every member (disabled ones too, BR-10), sorted by name; a blank cell is unpaid. */
    private fun table(eventName: String, schedule: Schedule, members: List<EventMember>): ReportTable {
        val dues = schedule.dues.sortedBy { it.index }
        val sorted = members.sortedBy { it.name.lowercase() }
        val paid = sorted.map { member -> dues.map { member.paidOn(schedule.id, it.index) } }
        return ReportTable(
            title = ScheduleSheetBuilder.tableTitle(eventName, schedule),
            header = listOf("NO", "NAME") + ScheduleSheetBuilder.dueHeaders(dues) + "TOTAL",
            rows = sorted.mapIndexed { i, member ->
                listOf((i + 1).toString(), member.name.trim().uppercase()) +
                    paid[i].map { if (it == 0L) "" else Money.format(it) } +
                    Money.format(paid[i].sum())
            },
            totals = listOf("", "TOTAL") +
                dues.indices.map { d -> Money.format(paid.sumOf { it[d] }) } +
                Money.format(paid.sumOf { it.sum() }),
        )
    }

    private fun String.fileSafe(): String = replace(Regex("[^A-Za-z0-9]+"), "-").trim('-').ifEmpty { "event" }

    private val DATE = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)
}
