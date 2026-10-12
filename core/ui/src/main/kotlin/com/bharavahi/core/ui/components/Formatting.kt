package com.bharavahi.core.ui.components

import android.icu.text.NumberFormat
import android.icu.util.ULocale
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val INDIA = ULocale("en_IN")
private val DATE = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)

/** Paise to "₹1,50,000" (Indian grouping), with paise shown only when there are any. */
fun formatRupees(paise: Long): String {
    val format = NumberFormat.getCurrencyInstance(INDIA)
    val digits = if (paise % 100 == 0L) 0 else 2
    format.minimumFractionDigits = digits
    format.maximumFractionDigits = digits
    return format.format(BigDecimal.valueOf(paise, 2))
}

/** "5 Jan 2026". */
fun formatDate(date: LocalDate?): String = date?.format(DATE).orEmpty()
