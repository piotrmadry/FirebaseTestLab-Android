package com.appunite.model

class Artifacts {

    var junit: Boolean = true
    var logcat: Boolean = false
    var video: Boolean = false
    var instrumentation: Boolean = false

    fun getArttifactsMap(): Map<String, Boolean> {
        return mapOf(Pair("junit", junit), Pair("logcat", logcat), Pair("video", video), Pair("instrumentation", instrumentation))
    }
}
