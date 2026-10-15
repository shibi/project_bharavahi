package com.bharavahi.core.domain.model

/** ORG-35: the Google account whose Drive holds the community's backup sheets. One per community. */
data class SheetsConnection(
    val googleEmail: String,
    /** Uid of the admin who connected it. */
    val connectedBy: String,
    val status: SheetsStatus,
)

enum class SheetsStatus {
    CONNECTED,

    /** Google refused the saved permission (revoked, password changed). An admin must connect again. */
    NEEDS_RECONNECT,
}

/**
 * One schedule's tab in the event spreadsheet (ORG-37). Row 0 is the title, row 1 the header and the
 * last row the totals; the rows between are members.
 */
data class SheetTab(
    val scheduleId: String,
    val title: String,
    val rows: List<List<SheetCell>>,
) {
    val columnCount: Int get() = rows.maxOfOrNull { it.size } ?: 0
}

sealed interface SheetCell {
    data class Text(val value: String) : SheetCell
    data class Number(val value: Int) : SheetCell

    /** Paise; written to the sheet as rupees. */
    data class Amount(val paise: Long) : SheetCell
    data class Formula(val formula: String) : SheetCell
    data object Empty : SheetCell
}
