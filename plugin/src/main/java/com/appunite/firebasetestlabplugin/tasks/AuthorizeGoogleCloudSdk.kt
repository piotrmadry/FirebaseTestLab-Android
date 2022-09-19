package com.appunite.firebasetestlabplugin.tasks

import com.appunite.firebasetestlabplugin.Constants
import com.appunite.firebasetestlabplugin.FirebaseTestLabPluginExtension
import com.appunite.firebasetestlabplugin.utils.get
import com.appunite.firebasetestlabplugin.utils.getOutputFileByFileName
import com.appunite.firebasetestlabplugin.utils.getTaskByName
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction

private const val AUTH_NOT_SET_ERROR_MESSAGE =
    "keyFile not set. Please check documentation for details."
private const val AUTH_NOT_EXIST_ERROR_MESSAGE =
    "keyFile doesn't exist. Please check documentation for details."

open class AuthorizeGoogleCloudSdk : HiddenExec() {

    private val extension: FirebaseTestLabPluginExtension =
        project.extensions.findByType(FirebaseTestLabPluginExtension::class.java).get()

    @TaskAction
    override fun exec() {

        if (extension.keyFile == null) throw GradleException(AUTH_NOT_SET_ERROR_MESSAGE)
        if (extension.keyFile?.exists() == false) throw GradleException(AUTH_NOT_EXIST_ERROR_MESSAGE)

        val gcloud = getTaskByName(Constants.ENSURE_GOOGLE_CLOUD_SDK_INSTALLED)
            .getOutputFileByFileName(Constants.GCLOUD)

        val authorizeWithGoogleCloudAccount = listOf(
            gcloud,
            "auth",
            "activate-service-account",
            "--key-file=${extension.keyFile?.absolutePath}"
        )
        commandLine = authorizeWithGoogleCloudAccount

        super.exec()
    }
}