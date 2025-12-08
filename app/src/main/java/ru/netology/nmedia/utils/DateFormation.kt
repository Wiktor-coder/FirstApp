package ru.netology.nmedia.utils

import java.text.SimpleDateFormat
import java.util.*

fun Long.toFormattedDate(): String {
    return SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())
        .format(Date(this))
}