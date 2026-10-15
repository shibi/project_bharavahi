package com.bharavahi.core.domain.rules

import com.bharavahi.core.domain.model.Due
import com.bharavahi.core.domain.model.EventMember
import com.bharavahi.core.domain.model.Schedule
import com.bharavahi.core.domain.model.SheetCell
import com.bharavahi.core.domain.model.SheetTab
import java.time.format.TextStyle
import java.util.Locale

/**
 * ORG-37: the layout of one schedule's tab.
 *
 * ```
 * EVENT · SCHEDULE                                 (merged title)
 * NO | NAME  | DUE1 - JUNE | DUE2 - JULY | TOTAL
 * 1  | AJITH | 500         |             | =SUM
 * …
 *    |       | =SUM        | =SUM        | =SUM   (totals)
 * ```
 * Every event member is listed, disabled ones too, because their payments still count (BR-10).
 */
object ScheduleSheetBuilder {

    const val FIRST_MEMBER_ROW = 3 // 1-based, after the title and header rows

    /** One tab per schedule, in the given order, with unique titles. */
    fun buildAll(eventName: String, schedules: List<Schedule>, members: List<EventMember>): List<SheetTab> {
        val used = mutableSetOf<String>()
        return schedules.map { schedule ->
            build(eventName, schedule, members).let { it.copy(title = uniqueTitle(it.title, used)) }
        }
    }

    fun build(eventName: String, schedule: Schedule, members: List<EventMember>): SheetTab {
        val dues = schedule.dues.sortedBy { it.index }
        val firstDueCol = column(2)
        val lastDueCol = column(1 + dues.size)
        val totalCol = column(2 + dues.size)
        val sorted = members.sortedBy { it.name.lowercase() }
        val lastMemberRow = FIRST_MEMBER_ROW + sorted.size - 1

        val title = listOf(SheetCell.Text(tableTitle(eventName, schedule)))
        val header = (listOf("NO", "NAME") + dueHeaders(dues) + "TOTAL").map { SheetCell.Text(it) }
        val memberRows = sorted.mapIndexed { i, member ->
            val row = FIRST_MEMBER_ROW + i
            listOf(SheetCell.Number(i + 1), SheetCell.Text(member.name.trim().uppercase())) +
                dues.map { due -> member.paidOn(schedule.id, due.index).let { if (it == 0L) SheetCell.Empty else SheetCell.Amount(it) } } +
                sumOrZero(dues.isEmpty(), "$firstDueCol$row:$lastDueCol$row")
        }
        val totals = listOf(SheetCell.Empty, SheetCell.Empty) +
            (2..2 + dues.size).map { c ->
                val col = column(c)
                sumOrZero(sorted.isEmpty(), "$col$FIRST_MEMBER_ROW:$col$lastMemberRow")
            }
        return SheetTab(schedule.id, schedule.name.trim().ifEmpty { "Schedule" }.take(MAX_TITLE), listOf(title, header) + memberRows + listOf(totals))
    }

    /** "DUE1 - JUNE" per due, sorted by index; the year is added when the dues span more than one. Also used by the PDF (ORG-34). */
    fun dueHeaders(dues: List<Due>): List<String> {
        val sorted = dues.sortedBy { it.index }
        val spansYears = sorted.map { it.dueDate.year }.distinct().size > 1
        return sorted.map { due ->
            val month = due.dueDate.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH).uppercase()
            "DUE${due.index + 1} - $month" + if (spansYears) " ${due.dueDate.year}" else ""
        }
    }

    /** "EVENT · SCHEDULE", the tab's title row. */
    fun tableTitle(eventName: String, schedule: Schedule): String = "${eventName.trim()} · ${schedule.name.trim()}".uppercase()

    private fun sumOrZero(empty: Boolean, range: String): SheetCell =
        if (empty) SheetCell.Number(0) else SheetCell.Formula("=SUM($range)")

    /** Sheet column letters for a 0-based index: 0 → A, 25 → Z, 26 → AA. */
    fun column(index: Int): String {
        var n = index + 1
        val sb = StringBuilder()
        while (n > 0) {
            val r = (n - 1) % 26
            sb.append('A' + r)
            n = (n - 1) / 26
        }
        return sb.reverse().toString()
    }

    /** Sheets refuses two tabs with the same title, ignoring case. */
    private fun uniqueTitle(title: String, used: MutableSet<String>): String {
        var candidate = title
        var n = 2
        while (!used.add(candidate.lowercase())) {
            val suffix = " ($n)"
            candidate = title.take(MAX_TITLE - suffix.length) + suffix
            n++
        }
        return candidate
    }

    private const val MAX_TITLE = 100
}
