package com.appunite.firebasetestlabplugin.tasks

import com.appunite.firebasetestlabplugin.utils.Constants
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.TaskAction
import java.io.File

class InstallGoogleCloudSDKTask : Exec() {
    
    init {
        group = Constants.FIREBASE_TEST_LAB
        description = "Install Google Cloud SDK."
    }
    
    @TaskAction
    fun run() {
        val installDir = resolveInstallationDir()
        val googleCloudSDKDir = File(installDir, "google-cloud-sdk")
        val binDir = File(googleCloudSDKDir, "bin")
        
        doFirst {
            commandLine = listOf(
                "bash",
                "-c",
                "rm -r \"${googleCloudSDKDir.absolutePath}\";export CLOUDSDK_CORE_DISABLE_PROMPTS=1 && export CLOUDSDK_INSTALL_DIR=\"${installDir.absolutePath}\" && curl https://sdk.cloud.google.com | bash"
            )
        }
        
        doLast{
            val gcloud = File(binDir, Constants.GCLOUD).exists()
            if (!gcloud) throw IllegalStateException("Google Cloud SDK installation failed.")
    
            val gsutil = File(binDir, Constants.GSUTIL).exists()
            if (!gsutil) throw IllegalStateException("Google Cloud SDK installation failed.")
            outputs.files(gcloud, gsutil)
        }
        
    }
    
    private fun resolveInstallationDir(): File {
        val customInstallLocation = System.getenv("CLOUDSDK_INSTALL_DIR")
        return when {
            !customInstallLocation.isNullOrEmpty() -> File(customInstallLocation)
            else -> File(project.buildDir, "gcloud")
        }
    }
}