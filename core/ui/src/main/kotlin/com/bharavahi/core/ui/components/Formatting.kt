package com.bharavahi.core.ui.components

import com.bharavahi.core.domain.validation.Money
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DATE = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)

/** Paise to "₹1,50,000" (Indian grouping), with paise shown only when there are any. */
fun formatRupees(paise: Long): String = Money.format(paise)

/** "5 Jan 2026". */
fun formatDate(date: LocalDate?): String = date?.format(DATE).orEmpty()
