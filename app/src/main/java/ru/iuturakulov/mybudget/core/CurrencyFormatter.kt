package ru.iuturakulov.mybudget.core

import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

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
}