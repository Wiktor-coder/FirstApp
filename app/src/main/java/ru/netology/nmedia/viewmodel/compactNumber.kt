package ru.netology.nmedia.viewmodel

fun Number.formatNumberCompact(): String {
    val value = this.toInt()
    return when {
        value < 1_000 -> toString()
        value < 10_000 -> {
            val hundreds = value / 100
            val wholePart = hundreds / 10
            val decimalPart = hundreds % 10
            val str = if (decimalPart == 0) "$wholePart" else "$wholePart.$decimalPart"
            "$str.K"
        }

        value < 1_000_000 -> "${value / 1_000}K"
        value < 10_000_000 -> {
            val hundredThousands = value / 100_000 // 1_350_000 → 13
            val wholePart = hundredThousands / 10 // 13 → 1
            val decimalPart = hundredThousands % 10 // 13 → 3
            val str = if (decimalPart == 0) "$wholePart" else "$wholePart.$decimalPart"
            "$str.M"
        }

        else -> "${value / 1_000_000}M"
    }
}