package com.sournary.simplemediaplayer.util

import java.text.SimpleDateFormat
import java.util.*

fun Long?.toTimeString(pattern: String, locale: Locale = Locale.getDefault()): String {
    if (this == null) return ""
    val date = Date(this)
    return SimpleDateFormat(pattern, locale).format(date)
}
