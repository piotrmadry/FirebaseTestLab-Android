package com.appunite.firebasetestlabplugin.tasks

import com.appunite.firebasetestlabplugin.Constants
import org.gradle.api.tasks.TaskAction
import java.io.File

private const val DEFAULT_GOOGLE_CLOUD_SDK_INSTALLATION_DIR = "CLOUDSDK_INSTALL_DIR"
private const val LOGGER_GOOGLE_CLOUD_SDK_ALREADY_INSTALLED = "Google Cloud SDK installed at: "
private const val DIR_GOOGLE_CLOUD_SDK = "google-cloud-sdk"
private const val DIR_GOOGLE_CLOUD_SDK_BIN = "bin"

open class DownloadGoogleCloudSdkTask : HiddenExec() {

    @TaskAction
    override fun exec() {
        val installationDir = resolveInstallationDir()
        project.logger.lifecycle(LOGGER_GOOGLE_CLOUD_SDK_ALREADY_INSTALLED + installationDir.toString())

        val googleCloudSdkDir = File(installationDir, DIR_GOOGLE_CLOUD_SDK)
        val googleCloudSdkBinDir = File(googleCloudSdkDir, DIR_GOOGLE_CLOUD_SDK_BIN)

        val googleCloudSdkDownloadCommand = listOf(
            "bash",
            "-c",
            "rm -r \"${googleCloudSdkDir.absolutePath}\";export CLOUDSDK_CORE_DISABLE_PROMPTS=1 && export CLOUDSDK_INSTALL_DIR=\"${installationDir.absolutePath}\" && curl https://sdk.cloud.google.com | bash"
        )

        val gcloud = File(googleCloudSdkBinDir, Constants.GCLOUD)
        val gsutil = File(googleCloudSdkBinDir, Constants.GSUTIL)

        commandLine = googleCloudSdkDownloadCommand
        outputs.files(listOf(gcloud, gsutil))
        super.exec()
    }

    private fun resolveInstallationDir(): File {
        val env = System.getenv(DEFAULT_GOOGLE_CLOUD_SDK_INSTALLATION_DIR)
        val installDir = when {
            !env.isNullOrEmpty() -> File(env)
            else -> File(project.buildDir, Constants.GCLOUD)
        }
        return installDir
    }
}