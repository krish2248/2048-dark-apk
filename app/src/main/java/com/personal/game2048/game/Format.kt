package com.personal.game2048.game

import java.util.Locale

/** Short label for a tile exponent: 2 ... 524288, then 1M, 2M ... */
fun tileLabel(exp: Int): String = when {
    exp <= 0 -> "0"
    exp < 20 -> (1L shl exp).toString()
    exp < 30 -> "${1L shl (exp - 20)}M"
    exp < 40 -> "${1L shl (exp - 30)}G"
    else -> "2^$exp"
}

fun fmt(n: Long): String = String.format(Locale.US, "%,d", n)
fun fmt(n: Int): String = fmt(n.toLong())

/** 12:05 or 1:02:05 */
fun clock(ms: Long): String {
    val s = ms / 1000
    val h = s / 3600
    return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, s % 3600 / 60, s % 60)
    else String.format(Locale.US, "%d:%02d", s / 60, s % 60)
}

fun hoursMinutes(ms: Long): String {
    val m = ms / 60_000
    return if (m >= 60) "${m / 60}h ${m % 60}m" else "${m}m"
}

/** "EG" becomes its flag emoji via regional indicator symbols. */
fun flag(country: String?): String {
    if (country == null || country.length != 2 || !country.all { it in 'A'..'Z' }) return "🏳️"
    return buildString { country.forEach { appendCodePoint(0x1F1E6 + (it - 'A')) } }
}

fun countryName(code: String): String =
    Locale("", code).getDisplayCountry(Locale.ENGLISH).ifBlank { code }

val allCountries: List<String> by lazy {
    Locale.getISOCountries().sortedBy { countryName(it) }
}
