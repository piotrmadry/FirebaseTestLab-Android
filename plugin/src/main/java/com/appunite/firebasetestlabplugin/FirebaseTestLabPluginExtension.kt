package com.appunite.firebasetestlabplugin

import com.appunite.firebasetestlabplugin.model.Device
import com.appunite.firebasetestlabplugin.model.ResultTypes
import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.api.Project
import java.io.File

open class FirebaseTestLabPluginExtension(private val project: Project) {

    var keyFile: File? = null
    var googleProjectId: String? = null
    var cloudSdkPath: String? = null

    var cloudBucketName: String? = null
    var cloudDirectoryName: String? = null

    var clearDirectoryBeforeRun = false
    var ignoreFailures: Boolean = false

    internal val devices = project.container(Device::class.java)
    val resultsTypes: ResultTypes = ResultTypes()

    fun createDevice(name: String, action: Device.() -> Unit): Device = devices.create(name, action)
    fun devices(action: NamedDomainObjectContainer<Device>.() -> Unit) {
        devices.action()
    }

    fun resultTypes(configure: ResultTypes.() -> Unit) {
        configure(resultsTypes)
    }

    fun resultTypes(closure: Closure<ResultTypes>) {
        project.configure(resultsTypes, closure)
    }
}
