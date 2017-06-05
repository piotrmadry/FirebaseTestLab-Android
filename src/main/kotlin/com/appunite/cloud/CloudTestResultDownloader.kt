package com.appunite.cloud

import com.appunite.extensions.TestResults
import com.appunite.utils.Constants
import com.appunite.utils.command
import com.appunite.utils.startCommand
import org.gradle.api.GradleException
import org.gradle.api.logging.Logger
import java.io.File


class CloudTestResultDownloader(val artifacts: List<String>,
                                val destinationDir: File,
                                val cloudSdkPath: File,
                                val cloudBucketName: String,
                                val resultsTestDir: String,
                                val testResults: TestResults,
                                val logger: Logger) {
    init {
        downloadResults()
    }

    private fun downloadResults() {
        if (artifacts.isEmpty()) {
            logger.error(Constants.ARTIFACTS_NOT_CONFIGURED)
            return
        }
        logger.lifecycle("Downloading results from $resultsTestDir")
        val destination = "$destinationDir/$resultsTestDir"
        prepareDestination(destination)
        artifacts.forEach { resource ->
            logger.lifecycle("Downloading artifacts" + resource)
            downloadResource("$cloudBucketName/$resultsTestDir/*", destination)
        }
        logger.lifecycle("Artifacts downloaded")
    }

    private fun prepareDestination(destPath: String) {
        val destination = File(destPath)
        logger.lifecycle("Preparing destination dir $destination")
        if (destination.exists()) {
            logger.lifecycle("Destination $destination is exists, delete recursively")
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
        logger.lifecycle("${command("gsutil", cloudSdkPath)} -m cp -r gs://$source $destination")
        return "${command("gsutil", cloudSdkPath)} -m cp -r gs://$source $destination"
                .startCommand()
                .apply {
                    inputStream.bufferedReader().forEachLine { logger.lifecycle(it) }
                    errorStream.bufferedReader().forEachLine { logger.error("Download resources: " + it) }
                }
                .waitFor() == 0
    }

//    private fun getResultDirs() = "ui-tests"
//            "${command("gsutil", cloudSdkPath)} ls gs://$cloudBucketName/${testResults.resultDir}"
//                    .startCommand()
//                    .apply {
//                        errorStream.bufferedReader().forEachLine { logger.error("getResultDir " + it) }
//                    }
//                    .inputStream
//                    .bufferedReader()
//                    .readLines()
//                    .filter { it.endsWith("/") }

}