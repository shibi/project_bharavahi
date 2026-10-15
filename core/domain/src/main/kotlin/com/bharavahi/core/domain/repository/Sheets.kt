package com.bharavahi.core.domain.repository

import com.bharavahi.core.domain.model.SheetTab
import com.bharavahi.core.domain.model.SheetsConnection
import com.bharavahi.core.domain.model.SheetsStatus
import kotlinx.coroutines.flow.Flow

/** `organizations/{orgId}/config/sheets`. */
interface SheetsConfigRepository {
    fun observe(orgId: String): Flow<SheetsConnection?>
    suspend fun get(orgId: String): SheetsConnection?
    suspend fun save(orgId: String, connection: SheetsConnection)
    suspend fun setStatus(orgId: String, status: SheetsStatus)
    suspend fun clear(orgId: String)
}

/** The Google account this phone has authorised for sheet backups. */
interface GoogleAccountAuth {
    /** Email of the Google account connected on this phone, or null. */
    fun connectedEmail(): String?
    fun setConnectedEmail(email: String?)

    /** A fresh access token, without asking the user. Throws NotAuthorized when they must approve again. */
    suspend fun accessToken(email: String): String
}

/** Google Sheets and Drive calls. Every call takes an access token from [GoogleAccountAuth]. */
interface SpreadsheetGateway {
    /** Email of the account the token belongs to. */
    suspend fun accountEmail(token: String): String

    /** Creates a spreadsheet and returns its id. */
    suspend fun createSpreadsheet(token: String, title: String): String

    /** False when the file is gone or this account can't open it. */
    suspend fun exists(token: String, spreadsheetId: String): Boolean

    /** Makes the managed tabs match [tabs]: adds, renames, rewrites and formats them; removes tabs of deleted schedules. */
    suspend fun syncTabs(token: String, spreadsheetId: String, tabs: List<SheetTab>)

    suspend fun deleteFile(token: String, fileId: String)
}

/** Queues sheet syncs in the background; they run when the phone is online (ORG-38). */
interface SheetSyncScheduler {
    fun enqueue(orgId: String, eventId: String)

    /** Rebuilds every event's sheet, e.g. right after connecting an account. */
    fun enqueueAll()
}
