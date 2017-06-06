package com.appunite.model

class Artifacts {

    var junit: Boolean = true
    var logcat: Boolean = false
    var video: Boolean = false
    var instrumentation: Boolean = false

    fun getArtifactsMap(): Map<ArtifactType, Boolean> {
        return mapOf(
                Pair(ArtifactType.JUNIT, junit),
                Pair(ArtifactType.LOGCAT, logcat),
                Pair(ArtifactType.VIDEO, video),
                Pair(ArtifactType.XML, instrumentation))
    }
}

enum class ArtifactType {
    JUNIT, LOGCAT, VIDEO, XML
}

