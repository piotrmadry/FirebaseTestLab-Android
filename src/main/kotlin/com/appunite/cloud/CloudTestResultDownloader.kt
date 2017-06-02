package com.appunite.cloud

import com.appunite.extensions.TestResults
import com.appunite.utils.command
import com.appunite.utils.startCommand
import org.gradle.api.GradleException
import org.gradle.api.logging.Logger
import java.io.File


class CloudTestResultDownloader(val artifacts: List<String>,
                                val destinationDir: File,
                                val cloudSdkPath: File,
                                val cloudBucketName: String,
                                val testResults: TestResults,
                                val logger: Logger) {
    init {
        downloadResults()
    }

    private fun downloadResults() {
        if (artifacts.isEmpty()) {
            logger.lifecycle("Artifacts downloading properties not configured")
            return
        }

        getResultDirs().forEach { resultsDir ->
            logger.debug("Downloading results from $resultsDir")
            val destination = "$destinationDir/${resultsDir.split("/").last()}"
            prepareDestination(destination)
            artifacts.forEach { resource ->
                downloadResource("$resultsDir$resource", destination)
            }
        }
        logger.lifecycle("gcloud: Artifacts downloaded")
    }

    private fun prepareDestination(destPath: String) {
        val destination = File(destPath)
        logger.debug("gcloud: Preparing destination dir $destination")
        if (destination.exists()) {
            logger.debug("gcloud: Destination $destination is exists, delete recursively")
            if (!destination.isDirectory) {
                throw GradleException("Destination path $destination is not directory")
            }
            destination.deleteRecursively()
        }
        destination.mkdirs()
        if (!destination.exists()) {
            throw GradleException("Cannot create destination dir $destination")
        }
    }

    private fun downloadResource(source: String, destination: String): Boolean {
        return "${command("gsutil", cloudSdkPath)} -m cp $source $destination"
                .startCommand()
                .apply {
                    inputStream.bufferedReader().forEachLine { logger.lifecycle(it) }
                    errorStream.bufferedReader().forEachLine { logger.lifecycle(it) }
                }
                .waitFor() == 0
    }

    private fun getResultDirs() =
            "${command("gsutil", cloudSdkPath)} ls gs://$cloudBucketName/${testResults.resultDir}"
                    .startCommand()
                    .apply {
                        errorStream.bufferedReader().forEachLine { logger.error(it) }
                    }
                    .inputStream
                    .bufferedReader()
                    .readLines()
                    .filter { it.endsWith("/") }
}