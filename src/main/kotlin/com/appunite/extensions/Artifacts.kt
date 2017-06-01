package com.appunite.extensions

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

interface Artifacts {
    var junit: Boolean
    var logcat: Boolean
    var video: Boolean
    var instrumentation: Boolean

    fun getArtifactPaths(): List<String>
}

@Suppress("unused")
class ArtifactsImpl : Artifacts {
    override var junit by PathBoolean("test_result_*.xml", true)
    override var logcat by PathBoolean("logcat")
    override var video by PathBoolean("video.mp4")
    override var instrumentation by PathBoolean("instrumentation.results")

    override fun getArtifactPaths() = paths.toList()

    private val paths = mutableListOf<String>()

    class PathBoolean(private var path: String,
                      private var value: Boolean = false)
        : ReadWriteProperty<ArtifactsImpl, Boolean> {
        override fun getValue(thisRef: ArtifactsImpl, property: KProperty<*>) = value

        override fun setValue(thisRef: ArtifactsImpl, property: KProperty<*>, value: Boolean) {
            this.value = value
            if (value) {
                thisRef.paths += path
            } else {
                thisRef.paths -= path
            }
        }
    }
}