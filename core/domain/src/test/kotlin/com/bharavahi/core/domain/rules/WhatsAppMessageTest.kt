package com.bharavahi.core.domain.rules

import com.bharavahi.core.domain.model.EventMember
import com.bharavahi.core.domain.model.MemberDues
import com.bharavahi.core.domain.model.DueStatus
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate

class WhatsAppMessageTest {
    private fun dues(name: String, paid: Long, active: Boolean = true) = MemberDues(
        member = EventMember(name, name, null, active, paid, emptyMap()),
        totalPaid = paid,
        dueSoFar = 0,
        pending = 0,
        currentStatus = DueStatus.PAID,
        overdueCount = 0,
        eventTotal = 150_000,
    )

    @Test
    fun `ORG-32 and ORG-33 full payers first, alphabetical, aligned, disabled left out`() {
        val text = WhatsAppMessage.build(
            "Onam 2026",
            LocalDate.parse("2026-08-05"),
            listOf(dues("Joseph", 125_000), dues("Beena", 150_000), dues("Anil", 150_000), dues("Cindy", 0), dues("Old", 1, active = false)),
        )
        assertThat(text).isEqualTo(
            """
            ```
            Onam 2026 · paid so far · 5 Aug 2026
            Anil   - 1500
            Beena  - 1500
            Cindy  -    0
            Joseph - 1250
            ```
            """.trimIndent(),
        )
    }
}
