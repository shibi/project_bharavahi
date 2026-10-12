package com.bharavahi.core.domain.rules

import com.bharavahi.core.domain.fake.eventMember
import com.bharavahi.core.domain.model.DueStatus
import com.bharavahi.core.domain.model.Schedule
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate

class DueStatusRulesTest {

    private val start = LocalDate.parse("2026-01-01")
    private val dues = DueGenerator.generate(150_000, 3, start, LocalDate.parse("2026-04-01"))
    private val schedule = Schedule("s1", "Monthly", "", 150_000, 3, start, LocalDate.parse("2026-04-01"), dues, false)

    @Test
    fun `status from amount paid`() {
        assertThat(DueStatusRules.statusOf(50_000, 50_000)).isEqualTo(DueStatus.PAID)
        assertThat(DueStatusRules.statusOf(60_000, 50_000)).isEqualTo(DueStatus.PAID)
        assertThat(DueStatusRules.statusOf(1, 50_000)).isEqualTo(DueStatus.PARTLY_PAID)
        assertThat(DueStatusRules.statusOf(0, 50_000)).isEqualTo(DueStatus.PENDING)
    }

    @Test
    fun `current due is the latest one already due`() {
        assertThat(DueStatusRules.currentDue(dues, LocalDate.parse("2026-02-15"))?.index).isEqualTo(1)
        assertThat(DueStatusRules.currentDue(dues, LocalDate.parse("2026-03-01"))?.index).isEqualTo(2)
    }

    @Test
    fun `before the first due date the current due is the first`() {
        assertThat(DueStatusRules.currentDue(dues, LocalDate.parse("2025-12-01"))?.index).isEqualTo(0)
    }

    @Test
    fun `summary counts active members only`() {
        val members = listOf(
            eventMember("a", paidByDue = mapOf("s1_0" to 50_000L)),
            eventMember("b", paidByDue = mapOf("s1_0" to 20_000L)),
            eventMember("c"),
            eventMember("d", active = false),
        )
        val first = DueStatusRules.summarize(schedule, members).first()
        assertThat(listOf(first.paid, first.partlyPaid, first.pending)).containsExactly(1, 1, 1).inOrder()
    }
}
