package com.bharavahi.core.domain.rules

import com.bharavahi.core.domain.fake.eventMember
import com.bharavahi.core.domain.model.Due
import com.bharavahi.core.domain.model.Schedule
import com.bharavahi.core.domain.model.SheetCell
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate

class ScheduleSheetBuilderTest {
    /** Three monthly dues of ₹500 from [start]. */
    private fun schedule(id: String = "s1", name: String = "Monthly", start: String = "2026-06-01"): Schedule {
        val first = LocalDate.parse(start)
        val dues = (0 until 3).map { i -> Due(i, "Due ${i + 1}", first.plusMonths(i.toLong()), 50_000) }
        return Schedule(id, name, "", 150_000, 3, first, dues.last().dueDate, dues, false)
    }

    private val members = listOf(
        eventMember("b", paidByDue = mapOf("s1_0" to 50_000L, "s1_1" to 20_000L)).copy(name = "Beena"),
        eventMember("a", paidByDue = mapOf("s1_1" to 50_000L, "other_0" to 99_900L)).copy(name = "Ajith"),
        eventMember("c", active = false).copy(name = "Cindy"),
    )

    @Test
    fun `layout follows the sample sheet`() {
        val tab = ScheduleSheetBuilder.build("Christmas Celebration 2026", schedule(), members)
        val rows = tab.rows

        assertThat(rows[0]).containsExactly(SheetCell.Text("CHRISTMAS CELEBRATION 2026 · MONTHLY"))
        assertThat(rows[1].map { (it as SheetCell.Text).value })
            .containsExactly("NO", "NAME", "JUNE", "JULY", "AUGUST", "TOTAL").inOrder()
        // Sorted by name, numbered from 1, disabled members kept, other schedules' payments ignored.
        assertThat(rows[2]).containsExactly(
            SheetCell.Number(1), SheetCell.Text("AJITH"),
            SheetCell.Empty, SheetCell.Amount(50_000), SheetCell.Empty, SheetCell.Formula("=SUM(C3:E3)"),
        ).inOrder()
        assertThat(rows[3].subList(1, 4)).containsExactly(
            SheetCell.Text("BEENA"), SheetCell.Amount(50_000), SheetCell.Amount(20_000),
        ).inOrder()
        assertThat(rows[4][1]).isEqualTo(SheetCell.Text("CINDY"))
        assertThat(rows.last()).containsExactly(
            SheetCell.Empty, SheetCell.Empty,
            SheetCell.Formula("=SUM(C3:C5)"), SheetCell.Formula("=SUM(D3:D5)"),
            SheetCell.Formula("=SUM(E3:E5)"), SheetCell.Formula("=SUM(F3:F5)"),
        ).inOrder()
        assertThat(tab.columnCount).isEqualTo(6)
    }

    @Test
    fun `year is added when a schedule crosses into the next year`() {
        val header = ScheduleSheetBuilder.build("Onam", schedule(start = "2026-12-01"), members).rows[1]
        assertThat(header[2]).isEqualTo(SheetCell.Text("DECEMBER 2026"))
        assertThat(header[4]).isEqualTo(SheetCell.Text("FEBRUARY 2027"))
    }

    @Test
    fun `day is shown when two dues share a month`() {
        val weekly = (0 until 3).map { i -> Due(i, "", LocalDate.parse("2026-06-20").plusDays(i * 7L), 1) }
        assertThat(ScheduleSheetBuilder.dueHeaders(weekly)).containsExactly("20 JUN", "27 JUN", "4 JUL").inOrder()
    }

    @Test
    fun `no members gives zero totals instead of an empty range`() {
        val rows = ScheduleSheetBuilder.build("Onam", schedule(), emptyList()).rows
        assertThat(rows).hasSize(3)
        assertThat(rows.last()[2]).isEqualTo(SheetCell.Number(0))
    }

    @Test
    fun `tab titles are unique ignoring case`() {
        val tabs = ScheduleSheetBuilder.buildAll(
            "Onam",
            listOf(schedule("s1", "Monthly"), schedule("s2", "monthly"), schedule("s3", "Monthly")),
            members,
        )
        assertThat(tabs.map { it.title }).containsExactly("Monthly", "monthly (2)", "Monthly (3)").inOrder()
    }

    @Test
    fun `column letters`() {
        assertThat(listOf(0, 25, 26, 51, 52, 122).map(ScheduleSheetBuilder::column))
            .containsExactly("A", "Z", "AA", "AZ", "BA", "DS").inOrder()
    }
}
