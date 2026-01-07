package com.clientledger.app.util

import java.text.NumberFormat
import java.util.Locale

object MoneyUtils {
    private val numberFormat = NumberFormat.getNumberInstance(Locale("ru", "RU"))
    private val percentFormat = NumberFormat.getNumberInstance(Locale("ru", "RU")).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }

    fun formatCents(cents: Long): String {
        val rubles = cents / 100
        val formatted = numberFormat.format(rubles)
        return "$formatted ₽"
    }
    
    fun formatCentsWithSpaces(cents: Long): String {
        val rubles = cents / 100
        val formatted = numberFormat.format(rubles)
        return "$formatted ₽"
    }
    
    fun formatPercent(value: Double): String {
        return "${percentFormat.format(value)}%"
    }
    
    fun formatDelta(cents: Long): String {
        val sign = if (cents >= 0) "+" else ""
        return "$sign${formatCents(cents)}"
    }
    
    fun formatDeltaWithPercent(cents: Long, percent: Double?): String {
        val delta = formatDelta(cents)
        return if (percent != null) {
            "$delta (${formatPercent(percent)})"
        } else {
            "$delta (—)"
        }
    }

    fun rublesToCents(rubles: Double): Long {
        return (rubles * 100).toLong()
    }

    fun centsToRubles(cents: Long): Double {
        return cents / 100.0
    }
}


