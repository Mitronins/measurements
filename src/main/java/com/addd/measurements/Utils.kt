package com.addd.measurements

import com.google.gson.Gson
import java.util.*

val gson = Gson()
val STATUS_CURRENT = 0
val STATUS_REJECT = 1
val STATUS_CLOSE = 2
val CHECK = "check"
val APP_LIST_TODAY_CURRENT = "listTodayCurrent"
val APP_LIST_TOMORROW_CURRENT = "listTomorrowCurrent"
val APP_LIST_TODAY_CLOSED = "listTodayClosed"
val APP_LIST_TOMORROW_CLOSED = "listTomorrowClosed"
val APP_LIST_TODAY_REJECTED = "listTodayRejected"
val APP_LIST_TOMORROW_REJECTED = "listTomorrowRejected"
val APP_USER_INFO = "userInfo"
fun getTodayDate(): String {
    val calendar = Calendar.getInstance()
    return String.format("%d-%02d-%02d", calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH))
}

fun getTomorrowDate(): String {
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.DAY_OF_MONTH, 1)
    return String.format("%d-%02d-%02d", calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH))
}