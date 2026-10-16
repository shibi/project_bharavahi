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
    fun `title matches the sheet tab and due headers drop the DUE prefix`() {
        val table = report.tables.single()
        val tab = ScheduleSheetBuilder.build(event.name, schedule, members)
        assertThat(table.title).isEqualTo((tab.rows[0][0] as SheetCell.Text).value)
        assertThat(table.header).containsExactly("NO", "NAME", "JUNE", "JULY", "AUGUST", "TOTAL").inOrder()
    }

    @Test
    fun `due headers put the year on a second line when dues span years`() {
        val spanning = (0 until 2).map { i -> Due(i, "", LocalDate.parse("2026-12-01").plusMonths(i.toLong()), 1) }
        val wide = schedule.copy(dues = spanning, numberOfDues = spanning.size)
        val header = EventPdfReportBuilder.build("", event, listOf(wide), members, first).tables.single().header
        assertThat(header.subList(2, 4)).containsExactly("DECEMBER\n2026", "JANUARY\n2027").inOrder()
    }

    @Test
    fun `many dues are split into even parts with the total on the last`() {
        val many = (0 until 25).map { i -> Due(i, "", first.plusMonths(i.toLong()), 1_000) }
        val wide = schedule.copy(dues = many, numberOfDues = many.size)
        val member = eventMember("a", paidByDue = mapOf("s1_0" to 1_000L, "s1_24" to 2_000L)).copy(name = "Ajith")
        val tables = EventPdfReportBuilder.build("", event, listOf(wide), listOf(member), first).tables

        assertThat(tables.map { it.header.size - 2 }).containsExactly(9, 9, 8).inOrder() // 9 + 9 + 7 dues, last adds TOTAL
        assertThat(tables[0].title).endsWith("· DUES 1–9 OF 25")
        assertThat(tables[2].title).endsWith("· DUES 19–25 OF 25")
        assertThat(tables[0].rows.single().take(3)).containsExactly("1", "AJITH", "₹10").inOrder()
        assertThat(tables[0].header.last()).isNotEqualTo("TOTAL")
        assertThat(tables[2].rows.single().takeLast(2)).containsExactly("₹20", "₹30").inOrder()
        assertThat(tables[2].totals.last()).isEqualTo("₹30")
        tables.forEach { assertThat(it.rows.single().size).isEqualTo(it.header.size) }
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
        assertThat(report.summary.map { it.first })
            .containsExactly("Opening balance", "Schedule total", "Collected", "Expenses", "Balance").inOrder()
        // Two active members × ₹1,500; the disabled member owes nothing.
        assertThat(report.summary[1].second).isEqualTo("₹3,000")
        assertThat(report.summary.last()).isEqualTo("Balance" to "₹1,000")
        assertThat(report.fileName).isEqualTo("Christmas-2026-dues.pdf")
    }
}
