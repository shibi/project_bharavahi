package com.bharavahi.core.domain.usecase

import com.bharavahi.core.domain.error.DomainException
import com.bharavahi.core.domain.fake.FakeEventRepository
import com.bharavahi.core.domain.fake.FakeOrganizationRepository
import com.bharavahi.core.domain.fake.FakeScheduleRepository
import com.bharavahi.core.domain.fake.eventMember
import com.bharavahi.core.domain.fake.org
import com.bharavahi.core.domain.model.Event
import com.bharavahi.core.domain.model.EventStatus
import com.bharavahi.core.domain.model.OrganizationStatus
import com.bharavahi.core.domain.model.Schedule
import com.bharavahi.core.domain.model.SheetTab
import com.bharavahi.core.domain.model.SheetsConnection
import com.bharavahi.core.domain.model.SheetsStatus
import com.bharavahi.core.domain.repository.GoogleAccountAuth
import com.bharavahi.core.domain.repository.SheetsConfigRepository
import com.bharavahi.core.domain.repository.SpreadsheetGateway
import com.bharavahi.core.domain.rules.DueGenerator
import com.bharavahi.core.domain.usecase.sheets.SyncEventSheetUseCase
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.LocalDate

class SyncEventSheetUseCaseTest {
    private val config = object : SheetsConfigRepository {
        val value = MutableStateFlow<SheetsConnection?>(SheetsConnection("club@gmail.com", "u1", SheetsStatus.CONNECTED))
        override fun observe(orgId: String) = value
        override suspend fun get(orgId: String) = value.value
        override suspend fun save(orgId: String, connection: SheetsConnection) { value.value = connection }
        override suspend fun setStatus(orgId: String, status: SheetsStatus) { value.value = value.value?.copy(status = status) }
        override suspend fun clear(orgId: String) { value.value = null }
    }
    private val account = object : GoogleAccountAuth {
        var email: String? = "club@gmail.com"
        var refuse = false
        override fun connectedEmail() = email
        override fun setConnectedEmail(email: String?) { this.email = email }
        override suspend fun accessToken(email: String): String = if (refuse) throw DomainException.NotAuthorized() else "token"
    }
    private val gateway = object : SpreadsheetGateway {
        val files = mutableSetOf<String>()
        var created = 0
        val synced = mutableListOf<Pair<String, List<SheetTab>>>()
        override suspend fun accountEmail(token: String) = "club@gmail.com"
        override suspend fun createSpreadsheet(token: String, title: String) = "sheet${++created}".also { files += it }
        override suspend fun exists(token: String, spreadsheetId: String) = spreadsheetId in files
        override suspend fun syncTabs(token: String, spreadsheetId: String, tabs: List<SheetTab>) { synced += spreadsheetId to tabs }
        override suspend fun deleteFile(token: String, fileId: String) { files -= fileId }
    }
    private val orgs = FakeOrganizationRepository().apply {
        orgs.value = mapOf("org1" to org("org1", "Green Park", OrganizationStatus.ACTIVE))
    }
    private val events = FakeEventRepository().apply {
        events.value = mapOf("e1" to Event("e1", "Onam", "", null, null, 0, EventStatus.ACTIVE, emptyList(), 0, 0, null))
        members.value = mapOf("m1" to eventMember("m1", paidByDue = mapOf("s1_0" to 50_000L)))
    }
    private val start = LocalDate.parse("2026-06-01")
    private val schedules = FakeScheduleRepository().apply {
        schedules.value = mapOf(
            "s1" to Schedule("s1", "Monthly", "", 150_000, 3, start, start.plusMonths(2), DueGenerator.generate(150_000, 3, start, start.plusMonths(2)), true),
        )
    }
    private val sync = SyncEventSheetUseCase(config, account, gateway, orgs, events, schedules)

    @Test
    fun `creates the spreadsheet once, then skips until something changes`() = runTest {
        sync("org1", "e1")
        assertThat(events.events.value.getValue("e1").sheetId).isEqualTo("sheet1")
        assertThat(gateway.synced.single().second.single().scheduleId).isEqualTo("s1")

        sync("org1", "e1")
        assertThat(gateway.synced).hasSize(1)

        events.members.value = mapOf("m1" to eventMember("m1", paidByDue = mapOf("s1_0" to 50_000L, "s1_1" to 10_000L)))
        sync("org1", "e1")
        assertThat(gateway.synced.map { it.first }).containsExactly("sheet1", "sheet1")
        assertThat(gateway.files).containsExactly("sheet1")
    }

    @Test
    fun `a spreadsheet deleted in Drive is created again`() = runTest {
        sync("org1", "e1")
        gateway.files.clear()
        sync("org1", "e1", force = true)
        assertThat(events.events.value.getValue("e1").sheetId).isEqualTo("sheet2")
        assertThat(gateway.synced.map { it.first }).containsExactly("sheet1", "sheet2").inOrder()
    }

    @Test
    fun `nothing happens without a connection or on another admin's phone`() = runTest {
        account.email = "someone@gmail.com"
        sync("org1", "e1")
        config.value.value = null
        account.email = "club@gmail.com"
        sync("org1", "e1")
        assertThat(gateway.synced).isEmpty()
        assertThat(events.events.value.getValue("e1").sheetId).isNull()
    }

    @Test
    fun `no sheet before the first schedule`() = runTest {
        schedules.schedules.value = emptyMap()
        sync("org1", "e1")
        assertThat(gateway.files).isEmpty()
    }

    @Test
    fun `revoked access asks for a reconnect`() = runTest {
        account.refuse = true
        val error = runCatching { sync("org1", "e1") }.exceptionOrNull()
        assertThat(error).isInstanceOf(DomainException.NotAuthorized::class.java)
        assertThat(config.value.value!!.status).isEqualTo(SheetsStatus.NEEDS_RECONNECT)
        account.refuse = false
        sync("org1", "e1")
        assertThat(gateway.synced).isEmpty()
    }
}
