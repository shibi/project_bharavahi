package com.bharavahi.core.domain.rules

import com.bharavahi.core.domain.model.Event
import com.bharavahi.core.domain.model.MemberDues
import com.bharavahi.core.domain.model.Payment
import com.bharavahi.core.domain.model.PaymentMethod
import com.bharavahi.core.domain.model.DueStatus
import com.bharavahi.core.domain.validation.Money

/** ORG-30: the backup email sent to every active admin after a payment is marked. */
data class PaymentReport(val subject: String, val html: String, val csvFileName: String, val csv: String)

/** Everything the report needs, already looked up. */
data class PaymentReportInput(
    val communityName: String,
    val event: Event,
    val payment: Payment,
    val memberName: String,
    val scheduleName: String,
    val dueLabel: String,
    val recordedByName: String,
    val recordedAt: String,
    val members: List<MemberDues>,
)

object PaymentReportBuilder {

    fun build(input: PaymentReportInput): PaymentReport = with(input) {
        val amount = Money.format(payment.amount)
        val subject = "$communityName · ${event.name} · $amount from $memberName ($dueLabel)"
        val rows = members.sortedBy { it.member.name.lowercase() }
        PaymentReport(
            subject = subject,
            html = html(input, rows),
            csvFileName = "${event.name.fileSafe()}-members.csv",
            csv = csv(event, rows),
        )
    }

    private fun html(input: PaymentReportInput, rows: List<MemberDues>): String = with(input) {
        buildString {
            append("<html><body style=\"font-family:sans-serif\">")
            append("<h2>").append(esc(if (payment.isReversal) "Payment reversed" else "Payment recorded")).append("</h2>")
            append("<table cellpadding=\"4\">")
            detail("Member", memberName)
            detail("Schedule", scheduleName)
            detail("Due", dueLabel)
            detail("Amount", Money.format(payment.amount))
            detail("Method", if (payment.method == PaymentMethod.ONLINE) "Online" else "Offline")
            if (payment.note.isNotBlank()) detail("Note", payment.note)
            payment.paidOn?.let { detail("Paid on", it.toString()) }
            detail("Recorded by", recordedByName)
            detail("Recorded at", recordedAt)
            append("</table>")

            append("<h3>").append(esc(event.name)).append(" summary</h3><table cellpadding=\"4\">")
            detail("Opening balance", Money.format(event.openingBalance))
            detail("Collected", Money.format(event.totalCollected))
            detail("Expenses", Money.format(event.totalExpenses))
            detail("Balance", Money.format(event.balance))
            append("</table>")

            append("<h3>Members</h3>")
            append("<table border=\"1\" cellpadding=\"4\" cellspacing=\"0\" style=\"border-collapse:collapse\">")
            append("<tr>")
            HEADERS.forEach { append("<th align=\"left\">").append(esc(it)).append("</th>") }
            append("</tr>")
            rows.forEach { row ->
                append("<tr>")
                cells(event, row).forEach { append("<td>").append(esc(it)).append("</td>") }
                append("</tr>")
            }
            append("</table>")
            append("<p style=\"color:#666\">Sent by Bharavahi as a backup of your community's payment records.</p>")
            append("</body></html>")
        }
    }

    private fun csv(event: Event, rows: List<MemberDues>): String = buildString {
        appendLine(HEADERS.joinToString(",") { csvCell(it) })
        rows.forEach { row -> appendLine(cells(event, row).joinToString(",") { csvCell(it) }) }
    }

    private fun cells(event: Event, row: MemberDues): List<String> = listOf(
        row.member.name + if (row.member.active) "" else " (disabled)",
        event.roleName(row.member.roleId).orEmpty(),
        Money.format(row.totalPaid),
        Money.format(row.dueSoFar),
        Money.format(row.pending),
        statusLabel(row.currentStatus),
    )

    fun statusLabel(status: DueStatus): String = when (status) {
        DueStatus.PAID -> "Paid"
        DueStatus.PARTLY_PAID -> "Partly paid"
        DueStatus.PENDING -> "Pending"
    }

    private fun StringBuilder.detail(label: String, value: String) {
        append("<tr><td><b>").append(esc(label)).append("</b></td><td>").append(esc(value)).append("</td></tr>")
    }

    private fun esc(text: String): String = text
        .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

    /** Quotes every cell, and neutralises a leading formula character so spreadsheets don't run it. */
    private fun csvCell(text: String): String {
        val safe = if (text.firstOrNull() in setOf('=', '+', '@')) "'$text" else text
        return "\"" + safe.replace("\"", "\"\"") + "\""
    }

    private fun String.fileSafe(): String = replace(Regex("[^A-Za-z0-9]+"), "-").trim('-').ifEmpty { "event" }

    private val HEADERS = listOf("Member", "Role", "Paid so far", "Due so far", "Pending", "Current due status")
}
