package com.bharavahi.core.domain.rules

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate

class DueGeneratorTest {

    private fun d(text: String) = LocalDate.parse(text)

    @Test
    fun `BR-1 even split`() {
        assertThat(DueGenerator.splitAmount(300_000, 6)).containsExactly(50_000L, 50_000L, 50_000L, 50_000L, 50_000L, 50_000L)
    }

    @Test
    fun `BR-1 remainder goes on the last due`() {
        assertThat(DueGenerator.splitAmount(100_000, 3)).containsExactly(33_300L, 33_300L, 33_400L).inOrder()
    }

    @Test
    fun `BR-1 paise in the total land on the last due`() {
        assertThat(DueGenerator.splitAmount(100_050, 2)).containsExactly(50_000L, 50_050L).inOrder()
    }

    @Test
    fun `BR-1 single due takes everything`() {
        assertThat(DueGenerator.splitAmount(12_345, 1)).containsExactly(12_345L)
    }

    @Test
    fun `BR-2 twelve whole months make monthly dues on the same day`() {
        val dates = DueGenerator.dueDates(12, d("2026-01-01"), d("2027-01-01"))
        assertThat(dates.first()).isEqualTo(d("2026-01-01"))
        assertThat(dates[1]).isEqualTo(d("2026-02-01"))
        assertThat(dates.last()).isEqualTo(d("2026-12-01"))
    }

    @Test
    fun `BR-2 end date on the last day of the range also counts as whole months`() {
        val dates = DueGenerator.dueDates(12, d("2026-01-01"), d("2026-12-31"))
        assertThat(dates.map { it.dayOfMonth }.toSet()).containsExactly(1)
        assertThat(dates.last()).isEqualTo(d("2026-12-01"))
    }

    @Test
    fun `BR-2 six months in three dues step two months`() {
        val dates = DueGenerator.dueDates(3, d("2026-04-15"), d("2026-10-15"))
        assertThat(dates).containsExactly(d("2026-04-15"), d("2026-06-15"), d("2026-08-15")).inOrder()
    }

    @Test
    fun `BR-2 month end is clamped`() {
        val dates = DueGenerator.dueDates(3, d("2026-10-31"), d("2027-01-31"))
        assertThat(dates).containsExactly(d("2026-10-31"), d("2026-11-30"), d("2026-12-31")).inOrder()
    }

    @Test
    fun `BR-2 uneven range splits the days`() {
        val dates = DueGenerator.dueDates(4, d("2026-01-01"), d("2026-01-21"))
        // 20 days / 4 = every 5 days
        assertThat(dates).containsExactly(d("2026-01-01"), d("2026-01-06"), d("2026-01-11"), d("2026-01-16")).inOrder()
    }

    @Test
    fun `BR-2 months not divisible by dues fall back to days`() {
        val dates = DueGenerator.dueDates(5, d("2026-01-01"), d("2027-01-01"))
        // 365 days / 5 = 73
        assertThat(dates[1]).isEqualTo(d("2026-03-15"))
    }

    @Test
    fun `BR-3 labels`() {
        val dues = DueGenerator.generate(300_000, 6, d("2026-01-01"), d("2026-07-01"))
        assertThat(dues.map { it.label }).containsExactly(
            "Due 1 of 6, Jan 2026", "Due 2 of 6, Feb 2026", "Due 3 of 6, Mar 2026",
            "Due 4 of 6, Apr 2026", "Due 5 of 6, May 2026", "Due 6 of 6, Jun 2026",
        ).inOrder()
        assertThat(dues.map { it.index }).containsExactly(0, 1, 2, 3, 4, 5).inOrder()
        assertThat(dues.sumOf { it.amount }).isEqualTo(300_000)
    }

    @Test
    fun `single day schedule has one due on the start date`() {
        val dues = DueGenerator.generate(50_000, 1, d("2026-05-01"), d("2026-05-01"))
        assertThat(dues.single().dueDate).isEqualTo(d("2026-05-01"))
    }
}
