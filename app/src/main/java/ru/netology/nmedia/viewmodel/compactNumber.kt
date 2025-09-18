package ru.netology.nmedia.viewmodel

fun Int.formatNumberCompact(): String {
    return when {
        this < 1_000 -> toString()
        this < 10_000 -> {
            val hundreds = this / 100
            val wholePart = hundreds / 10
            val decimalPart = hundreds % 10
            val str = if (decimalPart == 0) "$wholePart" else "$wholePart.$decimalPart"
            "$str.K"
        }

        this < 1_000_000 -> "${this / 1_000}K"
        this < 10_000_000 -> {
            val hundredThousands = this / 100_000 // 1_350_000 → 13
            val wholePart = hundredThousands / 10 // 13 → 1
            val decimalPart = hundredThousands % 10 // 13 → 3
            val str = if (decimalPart == 0) "$wholePart" else "$wholePart.$decimalPart"
            "$str.M"
        }

        else -> "${this / 1_000_000}M"
    }
}