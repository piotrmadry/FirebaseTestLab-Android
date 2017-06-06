package com.appunite.model


enum class Orientation {
    landscape,
    portrait
}

class Platform(val name: String) {
    var locales: List<String> = listOf("en")
    var orientations: List<Orientation> = listOf(Orientation.portrait)
    var androidApiLevels: List<Int> = emptyList()
    var deviceIds: List<String> = emptyList()
    var timeoutSec: Long = 0
}


