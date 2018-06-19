package com.example.tuha.tokboxclient

import com.google.gson.Gson

fun String.toZoomData(): ZoomData {
    return Gson().fromJson(this, ZoomData::class.java)
}

fun String.toTranslateData(): TranslateData {
    return Gson().fromJson(this, TranslateData::class.java)
}