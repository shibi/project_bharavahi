package com.bharavahi.core.domain.rules

import com.bharavahi.core.domain.fake.eventMember
import com.bharavahi.core.domain.model.Due
import com.bharavahi.core.domain.model.Event
import com.bharavahi.core.domain.model.EventStatus
import com.bharavahi.core.domain.model.Schedule
import com.bharavahi.core.domain.model.SheetCell
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate

class EventPdfReportTest {
    private val first = LocalDate.parse("2026-06-01")
    private val dues = (0 until 3).map { i -> Due(i, "Due ${i + 1}", first.plusMonths(i.toLong()), 50_000) }
    private val schedule = Schedule("s1", "Monthly", "", 150_000, 3, first, dues.last().dueDate, dues, false)
    private val event = Event(
        id = "e1", name = "Christmas 2026", description = "", startDate = null, endDate = null,
        openingBalance = 10_000, status = EventStatus.ACTIVE, roles = emptyList(),
        totalCollected = 120_000, totalExpenses = 30_000, createdAtMillis = null,
    )
    private val members = listOf(
        eventMember("b", paidByDue = mapOf("s1_0" to 50_000L, "s1_1" to 20_000L)).copy(name = "Beena"),
        eventMember("a", paidByDue = mapOf("s1_1" to 50_000L, "other_0" to 99_900L)).copy(name = "Ajith"),
        eventMember("c", active = false).copy(name = "Cindy"),
    )

    private val report = EventPdfReportBuilder.build("St. Mary's", event, listOf(schedule), members, LocalDate.parse("2026-10-15"))

    @Test
    fun `header and title match the sheet tab`() {
        val table = report.tables.single()
        val tab = ScheduleSheetBuilder.build(event.name, schedule, members)
        assertThat(table.title).isEqualTo((tab.rows[0][0] as SheetCell.Text).value)
        assertThat(table.header).isEqualTo(tab.rows[1].map { (it as SheetCell.Text).value })
    }

    @Test
    fun `rows are sorted members with blank unpaid dues and their total`() {
        val rows = report.tables.single().rows
        assertThat(rows.map { it[1] }).containsExactly("AJITH", "BEENA", "CINDY").inOrder()
        assertThat(rows[0]).containsExactly("1", "AJITH", "", "₹500", "", "₹500").inOrder()
        assertThat(rows[1]).containsExactly("2", "BEENA", "₹500", "₹200", "", "₹700").inOrder()
        assertThat(rows[2]).containsExactly("3", "CINDY", "", "", "", "₹0").inOrder()
    }

    @Test
    fun `totals add up each due and the whole schedule`() {
        assertThat(report.tables.single().totals).containsExactly("", "TOTAL", "₹500", "₹700", "₹0", "₹1,200").inOrder()
    }

    @Test
    fun `summary and file name`() {
        assertThat(report.subtitle).isEqualTo("St. Mary's · 15 Oct 2026")
        assertThat(report.summary.last()).isEqualTo("Balance" to "₹1,000")
        assertThat(report.fileName).isEqualTo("Christmas-2026-dues.pdf")
    }
}
