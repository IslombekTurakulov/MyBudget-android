package ru.iuturakulov.mybudget.core

import java.text.NumberFormat
import java.util.Currency
import java.util.Locale
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.roundToLong

object CurrencyFormatter {

    /**
     * Форматирует число с разделением тысяч и двумя дробными цифрами,
     * и добавляет пробел + символ валюты из ресурсов.
     */
    fun format(amount: Double): String {
        val nf = NumberFormat.getCurrencyInstance(Locale.getDefault()).apply {
            currency = Currency.getInstance("RUB")
            maximumFractionDigits = 2
            minimumFractionDigits = 2
        }
        return nf.format(amount)
    }

    fun Float.roundTo(n: Int): Float {
        val factor = 10f.pow(n)
        return (this * factor).roundToInt() / factor
    }

    fun Double.roundTo(n: Int): Double {
        val factor = 10.0.pow(n)
        return (this * factor).roundToLong() / factor
    }
}