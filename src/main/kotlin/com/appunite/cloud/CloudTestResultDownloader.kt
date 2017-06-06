package com.appunite.cloud

import com.appunite.utils.Constants
import com.appunite.utils.command
import com.appunite.utils.startCommand
import org.gradle.api.GradleException
import org.gradle.api.logging.Logger
import java.io.File


class CloudTestResultDownloader(val artifacts: Map<String, Boolean>,
                                val destinationDir: File,
                                val cloudSdkPath: File,
                                val cloudBucketName: String,
                                val resultsTestDir: String,
                                val logger: Logger) {
    fun fetchArtifacts() {
        if (artifacts.isEmpty()) {
            return
        }
        logger.lifecycle(Constants.DOWNLOAD_PHASE + "Downloading results from $resultsTestDir")

        val destinationPath = "$destinationDir/$resultsTestDir"
        val sourcePath = "$cloudBucketName/$resultsTestDir"

        prepareDestination(destinationPath)
        downloadResource(sourcePath, destinationPath)
    }

    private fun prepareDestination(destPath: String) {
        val destination = File(destPath)
        destination.mkdirs()
        if (!destination.exists()) {
            throw GradleException("Issue when tried to create destination dir $destination")
        }
    }

    private fun downloadResource(source: String, destination: String): Boolean {
        //TODO: Remove -n and leave comment about first and last files to exclude
        val excludeFiles = "-x \".*\\.txt$|.*\\.mp4$|.*\\.apk$|.*\\.results$|.*\\logcat$|.*\\.txt$\""
        logger.lifecycle("${command("gsutil", cloudSdkPath)} -m rsync $excludeFiles -r gs://$source $destination")
        return "${command("gsutil", cloudSdkPath)} -m rsync $excludeFiles -r -n gs://$source $destination"
                .startCommand()
                .apply {
                    inputStream.bufferedReader().forEachLine { logger.lifecycle(it) }
                    errorStream.bufferedReader().forEachLine { logger.error("Download resources: " + it) }
                }
                .waitFor() == 0
    }
}