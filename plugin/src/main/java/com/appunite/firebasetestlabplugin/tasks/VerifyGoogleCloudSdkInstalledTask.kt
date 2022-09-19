package com.appunite.firebasetestlabplugin.tasks

import com.appunite.firebasetestlabplugin.Constants
import com.appunite.firebasetestlabplugin.FirebaseTestLabPluginExtension
import com.appunite.firebasetestlabplugin.utils.get
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.io.File

private const val FILE_DOES_NOT_EXIST_ERROR_MESSAGE = " does not exist in the path: "

open class VerifyGoogleCloudSdkInstalledTask : DefaultTask() {

    private val extension: FirebaseTestLabPluginExtension =
        project.extensions.findByType(FirebaseTestLabPluginExtension::class.java).get()

    @TaskAction
    fun verify() {
        val googleCloudSdkInstallationLocation = extension.cloudSdkPath ?: return
        val googleCloudSdkInstallationLocationFile = File(googleCloudSdkInstallationLocation)

        val gcloud = File(googleCloudSdkInstallationLocationFile, Constants.GCLOUD)
        val gsutil = File(googleCloudSdkInstallationLocationFile, Constants.GSUTIL)

        doFirst {
            if (!gcloud.exists()) {
                throw IllegalStateException(Constants.GCLOUD + FILE_DOES_NOT_EXIST_ERROR_MESSAGE + googleCloudSdkInstallationLocationFile.absolutePath)
            }
            if (!gsutil.exists()) {
                throw IllegalStateException(Constants.GSUTIL + FILE_DOES_NOT_EXIST_ERROR_MESSAGE + googleCloudSdkInstallationLocationFile.absolutePath)
            }
        }
        outputs.files(gcloud, gsutil)
    }
}