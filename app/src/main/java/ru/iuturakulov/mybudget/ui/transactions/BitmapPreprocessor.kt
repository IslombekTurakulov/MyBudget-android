package ru.iuturakulov.mybudget.ui.transactions

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import java.text.NumberFormat
import java.util.Locale

object BitmapPreprocessor {

    /**
     * Переводит в градации серого и масштабирует до максимально допустимых размеров,
     * сохраняя пропорции.
     */
    fun preprocessBitmap(
        source: Bitmap,
        maxWidth: Int = 512,
        maxHeight: Int = 512
    ): Bitmap {
        // Сразу downsample при необходимости, чтобы не держать в памяти слишком крупную картинку
        val downsampled = downsampleBitmap(source, maxWidth, maxHeight)

        // Конвертация в Grayscale: создаём bitmap с теми же размерами, ARGB_8888
        val gray = Bitmap.createBitmap(
            downsampled.width,
            downsampled.height,
            Bitmap.Config.ARGB_8888
        )
        Canvas(gray).drawBitmap(downsampled, 0f, 0f, Paint().apply {
            colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
            isFilterBitmap = true
        })

        // если downsampled отличается от исходника — освобождаем память
        if (downsampled !== source) {
            downsampled.recycle()
        }

        // Ещё раз ресайзим с фильтром (если надо добиться ровно maxWidth×maxHeight)
        val final = resizeBitmap(gray, maxWidth, maxHeight)

        // освобождаем промежуточный grayscale
        if (final !== gray) {
            gray.recycle()
        }

        return final
    }

    private fun downsampleBitmap(
        source: Bitmap,
        maxWidth: Int,
        maxHeight: Int
    ): Bitmap {
        val (w, h) = source.width to source.height
        // Вычисляем минимальный downsample factor 2^n,
        // чтобы ни одна сторона не превышала maxWidth/maxHeight
        var inSampleSize = 1
        while (w / inSampleSize > maxWidth * 2 || h / inSampleSize > maxHeight * 2) {
            inSampleSize *= 2
        }
        return if (inSampleSize > 1) {
            Bitmap.createScaledBitmap(
                source,
                w / inSampleSize,
                h / inSampleSize,
                true
            )
        } else {
            source
        }
    }

    private fun resizeBitmap(
        bitmap: Bitmap,
        maxWidth: Int,
        maxHeight: Int
    ): Bitmap {
        val (origW, origH) = bitmap.width to bitmap.height
        val ratio = origW.toFloat() / origH
        val (newW, newH) = when {
            origW <= maxWidth && origH <= maxHeight -> origW to origH
            ratio > 1f -> maxWidth to (maxWidth / ratio).toInt()
            else -> (maxHeight * ratio).toInt() to maxHeight
        }
        // Если размеры не изменились — возвращаем оригинал
        return if (newW == origW && newH == origH) {
            bitmap
        } else {
            Bitmap.createScaledBitmap(bitmap, newW, newH, true)
        }
    }

    fun preprocessText(text: String): String {
        var t = text
        CLEANUP_REGEX.forEach { t = t.replace(it, "") }
        t = t.replace(DIGIT_O_REGEX, "0")
        return t.trim().replace("\\s+".toRegex(), " ")
    }

    fun extractTotalAmount(text: String): String? {
        for (regex in AMOUNT_PATTERNS) {
            val m = regex.find(text) ?: continue

            val rawAmount = try {
                m.groups["amount"]!!.value
            } catch (_: IllegalArgumentException) {
                m.groupValues.getOrNull(1) ?: continue
            }

            val normalized = rawAmount
                .replace("\\s".toRegex(), "")
                .replace(',', '.')

            try {
                val nf = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
                    maximumFractionDigits = 2
                }
                val value = nf.parse(normalized)?.toDouble() ?: continue
                return value.toString()
            } catch (_: Exception) {
                continue
            }
        }
        return null
    }

    private val CLEANUP_REGEX = listOf(
        Regex("[©®™•§]"),
        Regex("\\s+"),
        Regex("(?<=\\d) (?=\\d)"),
        Regex("[`´‘’′]")
    )
    private val DIGIT_O_REGEX = Regex("(?<=\\d)[oO](?=\\d)")
    private val AMOUNT_PATTERNS = listOf(
        Regex(
            """(?i)(?:итого|всего|сумма|к\s+оплате|total)\s*[:\-]?\s*(?<amount>[\d\s]+[.,]\d{2})\b"""
        ),
        // символ валюты, потом цифры
        Regex(
            """[€$₽]\s*(?<amount>[\d\s]+[.,]\d{2})\b"""
        ),
        // цифры с тысячными разделителями и единицами валюты
        Regex(
            """\b(?<amount>\d{1,3}(?:[ ,]\d{3})*[.,]\d{2})(?=\s*(?:руб|р|usd|€|\$))""",
            RegexOption.IGNORE_CASE
        ),
        // просто цифры с двумя знаками после точки/запятой
        Regex(
            """\b(?<amount>\d+[.,]\d{2})\b"""
        )
    )
}
