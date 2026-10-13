package com.bharavahi.core.domain.rules

import com.bharavahi.core.domain.fake.eventMember
import com.bharavahi.core.domain.model.DueStatus
import com.bharavahi.core.domain.model.MemberSort
import com.bharavahi.core.domain.model.Schedule
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate

class MemberDuesRulesTest {
    private val start = LocalDate.parse("2026-01-01")
    // ₹1,500 in 3 monthly dues of ₹500: 1 Jan, 1 Feb, 1 Mar.
    private val schedule = Schedule(
        "s1", "Monthly", "", 150_000, 3, start, LocalDate.parse("2026-04-01"),
        DueGenerator.generate(150_000, 3, start, LocalDate.parse("2026-04-01")), false,
    )
    private val today = LocalDate.parse("2026-02-10")

    @Test
    fun `due so far, pending and overdue`() {
        val m = eventMember("a", paidByDue = mapOf("s1_0" to 50_000L, "s1_1" to 20_000L))
        val dues = MemberDuesRules.compute(m, listOf(schedule), today)
        assertThat(dues.totalPaid).isEqualTo(70_000)
        assertThat(dues.dueSoFar).isEqualTo(100_000)
        assertThat(dues.pending).isEqualTo(30_000)
        assertThat(dues.currentStatus).isEqualTo(DueStatus.PARTLY_PAID)
        assertThat(dues.overdueCount).isEqualTo(1) // Feb is past and only partly paid
        assertThat(dues.eventTotal).isEqualTo(150_000)
        assertThat(dues.paidInFull).isFalse()
    }

    @Test
    fun `BR-6 late joiner owes past dues at once`() {
        val dues = MemberDuesRules.compute(eventMember("late"), listOf(schedule), today)
        assertThat(dues.currentStatus).isEqualTo(DueStatus.PENDING)
        assertThat(dues.overdueCount).isEqualTo(2)
        assertThat(dues.pending).isEqualTo(100_000)
    }

    @Test
    fun `no schedules means nothing owed`() {
        val dues = MemberDuesRules.compute(eventMember("a"), emptyList(), today)
        assertThat(dues.currentStatus).isEqualTo(DueStatus.PAID)
        assertThat(dues.pending).isEqualTo(0)
    }

    @Test
    fun `filter by status drops disabled members and sort puts most behind first`() {
        val all = MemberDuesRules.computeAll(
            listOf(
                eventMember("paid", paidByDue = mapOf("s1_0" to 50_000L, "s1_1" to 50_000L)),
                eventMember("one", paidByDue = mapOf("s1_0" to 50_000L)),
                eventMember("none"),
                eventMember("off", active = false),
            ),
            listOf(schedule),
            today,
        )
        val pending = MemberDuesRules.filterAndSort(all, DueStatus.PENDING, null, MemberSort.MOST_BEHIND)
        assertThat(pending.map { it.member.memberId }).containsExactly("none", "one").inOrder()
        val behind = MemberDuesRules.filterAndSort(all, null, null, MemberSort.MOST_BEHIND)
        assertThat(behind.map { it.member.memberId }).containsExactly("none", "off", "one", "paid").inOrder()
    }
}
