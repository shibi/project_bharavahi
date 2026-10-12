package com.bharavahi.core.domain.rules

import com.bharavahi.core.domain.model.Due
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/** BR-1 to BR-3: turns a schedule's total and date range into its list of dues. */
object DueGenerator {

    private val LABEL_MONTH = DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH)

    fun generate(amountPerMember: Long, numberOfDues: Int, startDate: LocalDate, endDate: LocalDate): List<Due> {
        require(numberOfDues >= 1) { "numberOfDues must be at least 1" }
        require(!endDate.isBefore(startDate)) { "endDate is before startDate" }
        val amounts = splitAmount(amountPerMember, numberOfDues)
        val dates = dueDates(numberOfDues, startDate, endDate)
        return List(numberOfDues) { i ->
            Due(index = i, label = label(i, numberOfDues, dates[i]), dueDate = dates[i], amount = amounts[i])
        }
    }

    /** BR-1: whole rupees per due, the leftover goes on the last due. Amounts are in paise. */
    fun splitAmount(total: Long, n: Int): List<Long> {
        val perDue = (total / n / PAISE_PER_RUPEE) * PAISE_PER_RUPEE
        return List(n) { i -> if (i == n - 1) total - perDue * (n - 1) else perDue }
    }

    /**
     * BR-2: monthly steps when the range is a whole number of months that divides evenly by [n],
     * otherwise the day span split evenly. The end date counts both ways people pick it:
     * 1 Jan to 1 Jan next year, or 1 Jan to 31 Dec, are both treated as 12 whole months.
     */
    fun dueDates(n: Int, start: LocalDate, end: LocalDate): List<LocalDate> {
        val months = wholeMonths(start, end)
        if (months != null && months > 0 && months % n == 0) {
            val step = (months / n).toLong()
            return List(n) { i -> start.plusMonths(i * step) }
        }
        val days = ChronoUnit.DAYS.between(start, end)
        return List(n) { i -> start.plusDays(i * days / n) }
    }

    /** BR-3. */
    fun label(index: Int, n: Int, dueDate: LocalDate): String = "Due ${index + 1} of $n, ${LABEL_MONTH.format(dueDate)}"

    private fun wholeMonths(start: LocalDate, end: LocalDate): Int? {
        for (candidate in listOf(end, end.plusDays(1))) {
            val months = ChronoUnit.MONTHS.between(start, candidate)
            if (start.plusMonths(months) == candidate) return months.toInt()
        }
        return null
    }

    const val PAISE_PER_RUPEE = 100L
}
