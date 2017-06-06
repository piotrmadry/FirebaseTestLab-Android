package com.appunite

import com.appunite.model.Artifacts
import com.appunite.model.Platform
import groovy.lang.Closure
import org.gradle.api.Project

open class FirebaseTestLabPluginExtension(val project: Project) {

    var cloudSdkPath: String = ""
    var cloudBucketName: String = ""
    var resultsTestDir = ""
    var ignoreFailures: Boolean = false
    var enableVariantLessTasks = false
    val platforms = project.container(Platform::class.java)!!
    val artifacts :Artifacts = Artifacts()

    fun platforms(closure: Closure<Platform>) {
        platforms.configure(closure)
    }

    fun artifacts(configure: Artifacts.() -> Unit) {
        configure(artifacts)
    }

    fun artifacts(closure: Closure<Artifacts>) {
        project.configure(artifacts, closure)
    }
}