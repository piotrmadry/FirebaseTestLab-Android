package com.appunite.firebasetestlabplugin.tasks

import com.appunite.firebasetestlabplugin.utils.Constants
import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

class VerifyOSSupportedGoogleCloudSDKInstallation : DefaultTask() {
    
    init {
        group = Constants.FIREBASE_TEST_LAB
        description = "Ensure installation is running on Linux or Mac OS, because on Windows OS is not supported."
    }
    
    @TaskAction
    fun run() {
        if (Os.isFamily(Os.FAMILY_WINDOWS)) {
            throw IllegalStateException("""
                Google Cloud SDK installation is not possible, because it's not supported on WindowsOS.
                You have to install it manually: https://cloud.google.com/sdk/downloads#windows
            """.trimIndent())
        }
    }
}