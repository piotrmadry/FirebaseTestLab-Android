package com.appunite.firebasetestlabplugin

import com.appunite.firebasetestlabplugin.model.Device
import com.appunite.firebasetestlabplugin.model.ResultTypes
import groovy.lang.Closure
import org.gradle.api.Project

open class FirebaseTestLabPluginExtension(private val project: Project) {

    companion object {
        private val DEFAULT_RESULTS_PATH = "app/build/androidTestResults"
        private val DEFAULT_DIRECTORY_NAME = "androidTestResults"
    }

    var cloudSdkPath: String? = null
    var cloudBucketName: String? = null
    var cloudDirectoryName: String = DEFAULT_DIRECTORY_NAME
    var clearDirectoryBeforeRun = false
    var ignoreFailures: Boolean = false

    val devices = project.container(Device::class.java)
    val resultsTypes: ResultTypes = ResultTypes()

    fun devices(closure: Closure<Device>) {
        devices.configure(closure)
    }

    fun resultTypes(configure: ResultTypes.() -> Unit) {
        configure(resultsTypes)
    }

    fun resultTypes(closure: Closure<ResultTypes>) {
        project.configure(resultsTypes, closure)
    }
}