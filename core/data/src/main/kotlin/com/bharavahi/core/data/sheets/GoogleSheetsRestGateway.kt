package com.bharavahi.core.data.sheets

import com.bharavahi.core.domain.error.DomainException
import com.bharavahi.core.domain.model.SheetCell
import com.bharavahi.core.domain.model.SheetTab
import com.bharavahi.core.domain.repository.SpreadsheetGateway
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.io.IOException
import java.math.BigDecimal
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.inject.Inject
import kotlin.random.Random

/**
 * Google Sheets v4 and Drive v3 over plain HTTPS. The app only has the `drive.file` scope, so it sees
 * nothing in Drive except the spreadsheets it created.
 */
internal class GoogleSheetsRestGateway @Inject constructor() : SpreadsheetGateway {

    override suspend fun accountEmail(token: String): String =
        call("GET", "$DRIVE/about?fields=user(emailAddress)", token)
            .getValue("user").jsonObject.getValue("emailAddress").jsonPrimitive.content

    override suspend fun createSpreadsheet(token: String, title: String): String {
        val body = buildJsonObject {
            putJsonObject("properties") { put("title", title) }
            // A new spreadsheet needs one tab; syncTabs removes it once a schedule's tab exists.
            putJsonArray("sheets") { add(buildJsonObject { putJsonObject("properties") { put("title", PLACEHOLDER) } }) }
        }
        return call("POST", SHEETS, token, body).getValue("spreadsheetId").jsonPrimitive.content
    }

    override suspend fun exists(token: String, spreadsheetId: String): Boolean =
        try {
            call("GET", "$SHEETS/$spreadsheetId?fields=spreadsheetId", token)
            true
        } catch (e: DomainException.NotFound) {
            false
        }

    override suspend fun deleteFile(token: String, fileId: String) {
        call("DELETE", "$DRIVE/files/$fileId", token)
    }

    override suspend fun syncTabs(token: String, spreadsheetId: String, tabs: List<SheetTab>) {
        val fields = "sheets(properties(sheetId,title),developerMetadata(metadataKey,metadataValue))"
        val existing = call("GET", "$SHEETS/$spreadsheetId?fields=${URLEncoder.encode(fields, "UTF-8")}", token)
            .array("sheets").map { it.jsonObject.toExistingTab() }
        val managed = existing.filter { it.scheduleId != null }.associateBy { it.scheduleId }
        val unmanaged = existing.filter { it.scheduleId == null }
        val placeholders = unmanaged.filter { it.title == PLACEHOLDER }
        // Titles of the admin's own tabs can't be reused; ours get a suffix instead.
        val takenTitles = unmanaged.filter { it.title != PLACEHOLDER }.map { it.title.lowercase() }.toSet()
        val usedIds = existing.map { it.sheetId }.toMutableSet()
        val keep = tabs.map { it.scheduleId }.toSet()
        val stale = managed.values.filter { it.scheduleId !in keep }

        val structure = mutableListOf<JsonObject>()
        // Every managed tab first gets a temporary title, so renames can't collide with each other.
        val sheetIds = tabs.map { tab ->
            val found = managed[tab.scheduleId]
            if (found != null) {
                structure += updateTitle(found.sheetId, "~${found.sheetId}")
                found.sheetId
            } else {
                newSheetId(usedIds).also { id ->
                    structure += request("addSheet") { putJsonObject("properties") { put("sheetId", id); put("title", "~$id") } }
                    structure += request("createDeveloperMetadata") {
                        putJsonObject("developerMetadata") {
                            put("metadataKey", METADATA_KEY)
                            put("metadataValue", tab.scheduleId)
                            put("visibility", "DOCUMENT")
                            putJsonObject("location") { put("sheetId", id) }
                        }
                    }
                }
            }
        }
        // A spreadsheet must keep one tab: the placeholder stays while there are no schedules.
        if (tabs.isEmpty() && placeholders.isEmpty() && unmanaged.isEmpty() && stale.isNotEmpty()) {
            structure += request("addSheet") { putJsonObject("properties") { put("title", PLACEHOLDER) } }
        }
        stale.forEach { structure += request("deleteSheet") { put("sheetId", it.sheetId) } }
        if (tabs.isNotEmpty()) placeholders.forEach { structure += request("deleteSheet") { put("sheetId", it.sheetId) } }
        val titles = tabs.map { if (it.title.lowercase() in takenTitles) "${it.title} (app)" else it.title }
        tabs.forEachIndexed { i, tab -> structure += layout(sheetIds[i], i, titles[i], tab) }
        if (structure.isNotEmpty()) {
            call("POST", "$SHEETS/$spreadsheetId:batchUpdate", token, buildJsonObject { put("requests", JsonArray(structure)) })
        }
        if (tabs.isEmpty()) return

        val data = tabs.mapIndexed { i, tab ->
            buildJsonObject {
                put("range", "'${titles[i].replace("'", "''")}'!A1")
                put("values", JsonArray(tab.rows.map { row -> JsonArray(row.padded(tab.columnCount).map { it.toJson() }) }))
            }
        }
        call(
            "POST",
            "$SHEETS/$spreadsheetId/values:batchUpdate",
            token,
            buildJsonObject {
                put("valueInputOption", "USER_ENTERED")
                put("data", JsonArray(data))
            },
        )
    }

    /** Size, title, order and the sample sheet's look: green title bar, blue header and totals, borders. */
    private fun layout(sheetId: Int, index: Int, title: String, tab: SheetTab): List<JsonObject> {
        val rows = tab.rows.size
        val cols = tab.columnCount
        val all = range(sheetId, 0, rows, 0, cols)
        return listOf(
            request("updateSheetProperties") {
                putJsonObject("properties") {
                    put("sheetId", sheetId)
                    put("title", title)
                    put("index", index)
                    putJsonObject("gridProperties") {
                        put("rowCount", rows)
                        put("columnCount", cols)
                        put("frozenRowCount", 2)
                    }
                }
                put("fields", "title,index,gridProperties(rowCount,columnCount,frozenRowCount)")
            },
            request("unmergeCells") { put("range", all) },
            request("mergeCells") {
                put("range", range(sheetId, 0, 1, 0, cols))
                put("mergeType", "MERGE_ALL")
            },
            // Resets formatting left from an earlier sync whose rows were laid out differently.
            repeatCell(all, "userEnteredFormat") {
                put("horizontalAlignment", "CENTER")
                put("verticalAlignment", "MIDDLE")
            },
            repeatCell(range(sheetId, 0, 1, 0, cols), "userEnteredFormat(backgroundColor,textFormat)") {
                put("backgroundColor", TITLE_GREEN)
                putJsonObject("textFormat") {
                    put("bold", true)
                    put("foregroundColor", WHITE)
                }
            },
            repeatCell(range(sheetId, 1, 2, 0, cols), "userEnteredFormat(backgroundColor,textFormat)") {
                put("backgroundColor", HEADER_BLUE)
                putJsonObject("textFormat") { put("bold", true) }
            },
            repeatCell(range(sheetId, rows - 1, rows, 0, cols), "userEnteredFormat(backgroundColor,textFormat)") {
                put("backgroundColor", TOTALS_BLUE)
                putJsonObject("textFormat") { put("bold", true) }
            },
            repeatCell(range(sheetId, 2, rows, 2, cols), "userEnteredFormat.numberFormat") {
                putJsonObject("numberFormat") {
                    put("type", "NUMBER")
                    put("pattern", "#,##0")
                }
            },
            request("updateBorders") {
                put("range", all)
                val line = buildJsonObject {
                    put("style", "SOLID")
                    put("color", BORDER_GREY)
                }
                listOf("top", "bottom", "left", "right", "innerHorizontal", "innerVertical").forEach { put(it, line) }
            },
            request("autoResizeDimensions") {
                putJsonObject("dimensions") {
                    put("sheetId", sheetId)
                    put("dimension", "COLUMNS")
                    put("startIndex", 0)
                    put("endIndex", cols)
                }
            },
        )
    }

    private fun updateTitle(sheetId: Int, title: String) = request("updateSheetProperties") {
        putJsonObject("properties") {
            put("sheetId", sheetId)
            put("title", title)
        }
        put("fields", "title")
    }

    private fun repeatCell(range: JsonObject, fields: String, format: JsonObjectBuilder.() -> Unit) = request("repeatCell") {
        put("range", range)
        putJsonObject("cell") { putJsonObject("userEnteredFormat", format) }
        put("fields", fields)
    }

    private fun range(sheetId: Int, rowStart: Int, rowEnd: Int, colStart: Int, colEnd: Int) = buildJsonObject {
        put("sheetId", sheetId)
        put("startRowIndex", rowStart)
        put("endRowIndex", rowEnd)
        put("startColumnIndex", colStart)
        put("endColumnIndex", colEnd)
    }

    private fun request(name: String, body: JsonObjectBuilder.() -> Unit) = buildJsonObject { putJsonObject(name, body) }

    private fun newSheetId(used: MutableSet<Int>): Int {
        var id: Int
        do id = Random.nextInt(1, Int.MAX_VALUE) while (!used.add(id))
        return id
    }

    private fun List<SheetCell>.padded(size: Int) = this + List(size - this.size) { SheetCell.Empty }

    private fun SheetCell.toJson(): JsonElement = when (this) {
        // The leading apostrophe keeps names as text, so "=…" or "1/2" are not read as formulas or dates.
        is SheetCell.Text -> JsonPrimitive("'$value")
        is SheetCell.Number -> JsonPrimitive(value)
        is SheetCell.Amount -> JsonPrimitive(BigDecimal.valueOf(paise, 2))
        is SheetCell.Formula -> JsonPrimitive(formula)
        SheetCell.Empty -> JsonPrimitive("")
    }

    private data class ExistingTab(val sheetId: Int, val title: String, val scheduleId: String?)

    private fun JsonObject.toExistingTab(): ExistingTab {
        val props = getValue("properties").jsonObject
        val scheduleId = (get("developerMetadata") as? JsonArray).orEmpty()
            .map { it.jsonObject }
            .firstOrNull { it["metadataKey"]?.jsonPrimitive?.content == METADATA_KEY }
            ?.get("metadataValue")?.jsonPrimitive?.content
        return ExistingTab(props.getValue("sheetId").jsonPrimitive.int, props["title"]?.jsonPrimitive?.content.orEmpty(), scheduleId)
    }

    private fun JsonObject.array(name: String): JsonArray = (get(name) as? JsonArray) ?: JsonArray(emptyList())

    private suspend fun call(method: String, url: String, token: String, body: JsonObject? = null): JsonObject =
        withContext(Dispatchers.IO) {
            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = TIMEOUT_MILLIS
                readTimeout = TIMEOUT_MILLIS
                setRequestProperty("Authorization", "Bearer $token")
            }
            try {
                if (body != null) {
                    connection.doOutput = true
                    connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    connection.outputStream.use { it.write(body.toString().toByteArray()) }
                }
                val code = connection.responseCode
                val text = (if (code in 200..299) connection.inputStream else connection.errorStream)
                    ?.bufferedReader()?.use { it.readText() }.orEmpty()
                when {
                    code in 200..299 -> if (text.isBlank()) JsonObject(emptyMap()) else Json.parseToJsonElement(text).jsonObject
                    code == 401 -> throw DomainException.NotAuthorized()
                    code == 429 || code >= 500 || "rateLimitExceeded" in text || "RATE_LIMIT" in text ->
                        throw DomainException.Network(IOException("HTTP $code"))
                    // With drive.file, a file this account didn't create is "forbidden": treat it as gone.
                    code == 403 || code == 404 -> throw DomainException.NotFound()
                    else -> throw DomainException.Unknown(IOException("HTTP $code: ${text.take(500)}"))
                }
            } catch (e: IOException) {
                throw DomainException.Network(e)
            } finally {
                connection.disconnect()
            }
        }

    private companion object {
        const val SHEETS = "https://sheets.googleapis.com/v4/spreadsheets"
        const val DRIVE = "https://www.googleapis.com/drive/v3"
        const val METADATA_KEY = "bharavahiScheduleId"
        const val PLACEHOLDER = "Bharavahi"
        const val TIMEOUT_MILLIS = 30_000

        fun rgb(r: Int, g: Int, b: Int) = buildJsonObject {
            put("red", r / 255.0)
            put("green", g / 255.0)
            put("blue", b / 255.0)
        }

        val TITLE_GREEN = rgb(0x6A, 0xA8, 0x4F)
        val HEADER_BLUE = rgb(0xCF, 0xE2, 0xF3)
        val TOTALS_BLUE = rgb(0xC9, 0xDA, 0xF8)
        val BORDER_GREY = rgb(0xBF, 0xBF, 0xBF)
        val WHITE = rgb(0xFF, 0xFF, 0xFF)
    }
}
