package com.bharavahi.core.domain.rules

import com.bharavahi.core.domain.model.DueStatus
import com.bharavahi.core.domain.model.EmailStatus
import com.bharavahi.core.domain.model.Event
import com.bharavahi.core.domain.model.EventMember
import com.bharavahi.core.domain.model.EventRole
import com.bharavahi.core.domain.model.EventStatus
import com.bharavahi.core.domain.model.MemberDues
import com.bharavahi.core.domain.model.Payment
import com.bharavahi.core.domain.model.PaymentMethod
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PaymentReportTest {
    private val event = Event(
        "e1", "Onam 2026", "", null, null, 10_000, EventStatus.ACTIVE, listOf(EventRole("r1", "President")),
        50_000, 5_000, null,
    )
    private val payment = Payment("p1", "m1", "s1", 0, 50_000, PaymentMethod.ONLINE, "", null, "u1", null, null, EmailStatus.PENDING, false)
    private fun row(name: String, roleId: String?) = MemberDues(
        EventMember(name, name, roleId, true, 50_000, emptyMap()), 50_000, 50_000, 0, DueStatus.PAID, 0, 150_000,
    )

    @Test
    fun `ORG-30 subject, sorted rows and safe CSV`() {
        val report = PaymentReportBuilder.build(
            PaymentReportInput(
                communityName = "Green Park",
                event = event,
                payment = payment,
                memberName = "Anil",
                scheduleName = "Monthly",
                dueLabel = "Due 1 of 3, Jan 2026",
                recordedByName = "Shibin",
                recordedAt = "1 Jan 2026 10:00",
                members = listOf(row("=Zed", null), row("Anil <b>", "r1")),
            ),
        )
        assertThat(report.subject).isEqualTo("Green Park · Onam 2026 · ₹500 from Anil (Due 1 of 3, Jan 2026)")
        assertThat(report.html).contains("Anil &lt;b&gt;")
        assertThat(report.html).contains("₹550") // balance 100 + 500 - 50
        val lines = report.csv.trim().lines()
        // "=" sorts before letters; the formula guard keeps it inert in spreadsheets.
        assertThat(lines[1]).startsWith("\"'=Zed\"")
        assertThat(lines[2]).startsWith("\"Anil <b>\",\"₹500\"")
        assertThat(lines[0]).doesNotContain("Role")
        assertThat(report.html).doesNotContain("President")
        assertThat(report.csvFileName).isEqualTo("Onam-2026-members.csv")
    }
}
