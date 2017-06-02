package com.appunite

import com.appunite.extensions.Artifacts
import com.appunite.extensions.ArtifactsImpl
import com.appunite.extensions.Platform
import groovy.lang.Closure
import org.gradle.api.Project

open class FirebaseTestLabPluginExtension(private val project: Project){

    var cloudSdkPath: String = ""
    var cloudBucketName: String = ""
    var ignoreFailures: Boolean = false
    var enableVariantLessTasks = false
    val platforms = project.container(Platform::class.java)!!
    val artifacts: Artifacts = ArtifactsImpl()

    fun downloadArtifact(configure: Artifacts.() -> Unit) {
        configure(artifacts)
    }

    fun platforms(closure: Closure<Platform>) {
        platforms.configure(closure)
    }

    fun downloadArtifact(closure: Closure<Artifacts>) {
        project.configure(artifacts, closure)
    }
}