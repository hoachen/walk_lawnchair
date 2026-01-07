package com.ur.apps.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateUtils {

    fun formatMillis(millis: Long, pattern: String = "yyyy-MM-dd HH:mm:ss"): String {
        return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(millis))
    }

    fun getCurrentDateTime(): String {
        return formatMillis(System.currentTimeMillis())
    }
    // 预定义一些常用格式
    fun toDateOnly(millis: Long): String = formatMillis(millis, "yyyy-MM-dd")

    fun toTimeOnly(millis: Long): String = formatMillis(millis, "HH:mm:ss")

    fun toChineseDate(millis: Long): String = formatMillis(millis, "yyyy MMM dd")
}