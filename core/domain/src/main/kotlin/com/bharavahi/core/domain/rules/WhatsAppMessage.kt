package com.bharavahi.core.domain.rules

import com.bharavahi.core.domain.model.MemberDues
import com.bharavahi.core.domain.validation.Money
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** ORG-32 and ORG-33: the "Name - amount" list admins forward to the community's WhatsApp group. */
object WhatsAppMessage {

    private val DATE = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)
    private const val FENCE = "```"

    fun build(eventName: String, date: LocalDate, dues: List<MemberDues>): String {
        val active = dues.filter { it.member.active }
        val byName = compareBy<MemberDues> { it.member.name.lowercase() }
        val ordered = active.filter { it.paidInFull }.sortedWith(byName) +
            active.filterNot { it.paidInFull }.sortedWith(byName)
        val nameWidth = ordered.maxOfOrNull { it.member.name.length } ?: 0
        val amountWidth = ordered.maxOfOrNull { Money.wholeRupees(it.totalPaid).length } ?: 0
        val lines = ordered.map {
            it.member.name.padEnd(nameWidth) + " - " + Money.wholeRupees(it.totalPaid).padStart(amountWidth)
        }
        return buildString {
            appendLine(FENCE)
            appendLine("$eventName · paid so far · ${DATE.format(date)}")
            lines.forEach { appendLine(it) }
            append(FENCE)
        }
    }
}
