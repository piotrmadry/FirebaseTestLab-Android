package com.appunite.firebasetestlabplugin.model

class Device(val name: String) {
    var locales: List<String> = listOf("en")
    var screenOrientations: List<ScreenOrientation> = listOf(ScreenOrientation.PORTRAIT)
    var androidApiLevels: List<Int> = listOf()
    var deviceIds: List<String> = listOf()
    var timeout: Long = 0

    var testUniversalApk: Boolean = true

    var filterAbiSplits = false
    var abiSplits: Set<String> = setOf()
}







