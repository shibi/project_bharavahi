package com.bharavahi.core.domain.validation

/** Money is stored as whole paise (Long). Admins type rupees. */
object Money {
    private val RUPEES = Regex("^[0-9]+(\\.[0-9]{1,2})?$")

    /** "1,500" → 150000, "12.5" → 1250. Returns null for anything that isn't a plain amount. */
    fun parseRupees(text: String): Long? {
        val cleaned = text.trim().replace(",", "").replace("₹", "").trim()
        if (!RUPEES.matches(cleaned)) return null
        val parts = cleaned.split('.')
        val rupees = parts[0].toLongOrNull() ?: return null
        val paise = parts.getOrNull(1)?.padEnd(2, '0')?.toLong() ?: 0L
        if (rupees > Long.MAX_VALUE / 100 - 1) return null
        return rupees * 100 + paise
    }

    /** 150000 → "1500", 1250 → "12.50". For prefilling text fields. */
    fun toInput(paise: Long): String =
        if (paise % 100 == 0L) (paise / 100).toString() else "%d.%02d".format(paise / 100, paise % 100)

    /** 15000000 → "₹1,50,000", 1250 → "₹12.50". Indian digit grouping, paise only when present. */
    fun format(paise: Long): String {
        val sign = if (paise < 0) "-" else ""
        val abs = kotlin.math.abs(paise)
        val rupees = groupIndian((abs / 100).toString())
        val fraction = if (abs % 100 == 0L) "" else ".%02d".format(abs % 100)
        return "$sign₹$rupees$fraction"
    }

    /** Whole rupees, rounded down, no symbol or grouping: for the WhatsApp list (ORG-32). */
    fun wholeRupees(paise: Long): String = (paise / 100).toString()

    private fun groupIndian(digits: String): String {
        if (digits.length <= 3) return digits
        val last3 = digits.takeLast(3)
        val rest = digits.dropLast(3)
        return rest.reversed().chunked(2).joinToString(",").reversed() + "," + last3
    }
}
