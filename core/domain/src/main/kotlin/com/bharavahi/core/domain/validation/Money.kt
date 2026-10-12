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
}
