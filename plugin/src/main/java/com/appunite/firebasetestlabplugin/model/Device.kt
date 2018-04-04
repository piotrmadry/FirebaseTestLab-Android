package com.appunite.firebasetestlabplugin.model

class Device(val name: String) {
    var locales: List<String> = listOf("en")
    var screenOrientations: List<ScreenOrientation> = listOf(ScreenOrientation.PORTRAIT)
    var androidApiLevels: List<Int> = listOf()
    var deviceIds: List<String> = listOf()
    var timeout: Long = 0

    var testUniversalApk: Boolean = true

    fun filterAbis(vararg filter: String) {
        abisSplits = setOf(*filter)
        doAbiSplits = true
    }
    fun filterAbis(filter: Set<String>) {
        abisSplits = filter
        doAbiSplits = true
    }
    internal var doAbiSplits = false
    internal var abisSplits: Set<String> = setOf()
}







