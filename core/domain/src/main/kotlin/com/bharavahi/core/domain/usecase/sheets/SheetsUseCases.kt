package com.bharavahi.core.domain.usecase.sheets

import com.bharavahi.core.domain.error.DomainException
import com.bharavahi.core.domain.model.EventStatus
import com.bharavahi.core.domain.model.SheetsConnection
import com.bharavahi.core.domain.model.SheetsStatus
import com.bharavahi.core.domain.repository.AuthRepository
import com.bharavahi.core.domain.repository.EventRepository
import com.bharavahi.core.domain.repository.GoogleAccountAuth
import com.bharavahi.core.domain.repository.OrgContext
import com.bharavahi.core.domain.repository.OrganizationRepository
import com.bharavahi.core.domain.repository.ScheduleRepository
import com.bharavahi.core.domain.repository.SheetSyncScheduler
import com.bharavahi.core.domain.repository.SheetsConfigRepository
import com.bharavahi.core.domain.repository.SpreadsheetGateway
import com.bharavahi.core.domain.rules.ScheduleSheetBuilder
import com.bharavahi.core.domain.usecase.domainResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/** What the Profile card shows (ORG-35). */
data class SheetsState(
    val connection: SheetsConnection?,
    /** The connected account is signed in on this phone, so this phone keeps the sheets up to date. */
    val syncsFromThisPhone: Boolean,
)

class ObserveSheetsStateUseCase @Inject constructor(
    private val org: OrgContext,
    private val config: SheetsConfigRepository,
    private val account: GoogleAccountAuth,
) {
    operator fun invoke(): Flow<SheetsState> = flow {
        config.observe(org.orgId()).collect { connection ->
            emit(SheetsState(connection, connection != null && account.connectedEmail() == connection.googleEmail))
        }
    }
}

/**
 * ORG-35: called with the token Google gave after the admin approved access. Makes that account the
 * community's sheet account and rebuilds every event's sheet in it.
 */
class ConnectSheetsUseCase @Inject constructor(
    private val org: OrgContext,
    private val auth: AuthRepository,
    private val config: SheetsConfigRepository,
    private val account: GoogleAccountAuth,
    private val gateway: SpreadsheetGateway,
    private val scheduler: SheetSyncScheduler,
) {
    suspend operator fun invoke(accessToken: String): Result<String> = domainResult {
        val uid = auth.currentUser()?.uid ?: throw DomainException.NotAuthorized()
        val email = gateway.accountEmail(accessToken)
        config.save(org.orgId(), SheetsConnection(email, uid, SheetsStatus.CONNECTED))
        account.setConnectedEmail(email)
        scheduler.enqueueAll()
        email
    }
}

/** Stops sheet backups. Sheets already in Drive are kept. */
class DisconnectSheetsUseCase @Inject constructor(
    private val org: OrgContext,
    private val config: SheetsConfigRepository,
    private val account: GoogleAccountAuth,
) {
    suspend operator fun invoke(): Result<Unit> = domainResult {
        config.clear(org.orgId())
        account.setConnectedEmail(null)
    }
}

class SyncAllSheetsNowUseCase @Inject constructor(private val scheduler: SheetSyncScheduler) {
    operator fun invoke() = scheduler.enqueueAll()
}

/**
 * ORG-36 and ORG-37: rewrites an event's spreadsheet from Firestore, one tab per schedule. Runs in the
 * background job; throws on failure so the job can retry.
 *
 * Does nothing when no account is connected, or when the connected account isn't the one on this phone
 * (co-admins' phones): the connected phone picks their payments up from Firestore.
 */
class SyncEventSheetUseCase @Inject constructor(
    private val config: SheetsConfigRepository,
    private val account: GoogleAccountAuth,
    private val gateway: SpreadsheetGateway,
    private val organizations: OrganizationRepository,
    private val events: EventRepository,
    private val schedules: ScheduleRepository,
) {
    /** [force] rewrites even when nothing changed, e.g. to restore a tab someone deleted by hand. */
    suspend operator fun invoke(orgId: String, eventId: String, force: Boolean = false) {
        val email = syncingEmail(orgId) ?: return
        val event = events.observe(orgId, eventId).first() ?: return
        val scheduleList = schedules.observeAll(orgId, eventId).first()
        // "A sheet with each schedule": an event gets its spreadsheet with its first schedule.
        if (scheduleList.isEmpty() && event.sheetId == null) return
        val tabs = ScheduleSheetBuilder.buildAll(event.name, scheduleList, events.observeMembers(orgId, eventId).first())
        val hash = tabs.hashCode()
        if (!force && event.sheetId != null && event.sheetHash == hash) return

        val token = authorized(orgId) { account.accessToken(email) }
        authorized(orgId) {
            var sheetId = event.sheetId?.takeIf { gateway.exists(token, it) }
            if (sheetId == null) {
                val community = organizations.observe(orgId).first()?.name.orEmpty()
                val created = gateway.createSpreadsheet(token, listOf(community, event.name).filter { it.isNotBlank() }.joinToString(" - "))
                sheetId = events.claimSheetId(orgId, eventId, expected = event.sheetId, sheetId = created)
                if (sheetId != created) gateway.deleteFile(token, created)
            }
            gateway.syncTabs(token, sheetId, tabs)
        }
        events.setSheetSynced(orgId, eventId, hash)
    }

    private suspend fun syncingEmail(orgId: String): String? {
        val connection = config.get(orgId) ?: return null
        if (connection.status != SheetsStatus.CONNECTED) return null
        return connection.googleEmail.takeIf { it == account.connectedEmail() }
    }

    /** Google refusing the token means the admin has to connect again; say so in Profile and stop retrying. */
    private suspend fun <T> authorized(orgId: String, block: suspend () -> T): T =
        try {
            block()
        } catch (e: DomainException.NotAuthorized) {
            config.setStatus(orgId, SheetsStatus.NEEDS_RECONNECT)
            throw e
        }
}

/** ORG-38: syncs the signed-in admin's events. [force] also covers events that aren't active. */
class SyncAllSheetsUseCase @Inject constructor(
    private val org: OrgContext,
    private val events: EventRepository,
    private val syncEvent: SyncEventSheetUseCase,
) {
    suspend operator fun invoke(force: Boolean) {
        val orgId = org.orgId()
        events.observeAll(orgId).first()
            .filter { if (force) it.status != EventStatus.ARCHIVED else it.status == EventStatus.ACTIVE }
            .forEach { syncEvent(orgId, it.id, force) }
    }
}
