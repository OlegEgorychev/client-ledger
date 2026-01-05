package com.clientledger.app.util

import java.text.NumberFormat
import java.util.Locale

object MoneyUtils {
    private val numberFormat = NumberFormat.getNumberInstance(Locale("ru", "RU"))

    fun formatCents(cents: Long): String {
        val rubles = cents / 100
        val formatted = numberFormat.format(rubles)
        return "$formatted ₽"
    }

    fun rublesToCents(rubles: Double): Long {
        return (rubles * 100).toLong()
    }

    fun centsToRubles(cents: Long): Double {
        return cents / 100.0
    }
}


