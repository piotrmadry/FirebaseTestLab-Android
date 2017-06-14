package com.appunite.cloud

import com.appunite.model.ArtifactType
import com.appunite.utils.Constants
import com.appunite.utils.asCommand
import com.appunite.utils.command
import org.gradle.api.GradleException
import org.gradle.api.logging.Logger
import java.io.File


class CloudTestResultDownloader(val artifacts: Map<ArtifactType, Boolean>,
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

        val sourcePath = "$cloudBucketName/$resultsTestDir"

        prepareDestination(destinationDir)
        downloadResource(sourcePath, destinationDir, artifacts)
        clearBucket(cloudSdkPath, cloudBucketName)
    }

    private fun prepareDestination(destination: File) {
        destination.mkdirs()
        if (!destination.exists()) {
            throw GradleException("Issue when creating destination dir $destination")
        }
    }

    private fun downloadResource(source: String, destination: File, artifacts: Map<ArtifactType, Boolean>) {
        //TODO: Remove -n and leave comment about first and last files to exclude
        val startQuery = "-x \".*\\.txt$|.*\\.apk$"
        val endQuery = "|.*\\.txt$\""
        val excludeQuery = StringBuilder().append(startQuery)
        artifacts.keys.forEach { key ->
            when (key){
                ArtifactType.VIDEO -> excludeQuery.append("|.*\\.mp4$")
                ArtifactType.XML -> excludeQuery.append("|.*\\.results$")
                ArtifactType.LOGCAT -> excludeQuery.append("|.*\\logcat$")
                ArtifactType.JUNIT -> excludeQuery.append("|.*\\.xml$")
            }
        }
        excludeQuery.append(endQuery).toString()
        val processCreator = ProcessBuilder("""${command("gsutil", cloudSdkPath)} -m rsync $excludeQuery -r gs://$source $destination""".asCommand())
        val process = processCreator.start()

        process.errorStream.bufferedReader().forEachLine { logger.lifecycle(it) }
        process.inputStream.bufferedReader().forEachLine { logger.lifecycle(it) }

        process.waitFor()

//                .startCommand()
//                .apply {
//                    inputStream.bufferedReader().forEachLine { logger.lifecycle(it) }
//                    errorStream.bufferedReader().forEachLine { logger.error("Download resources: " + it) }
//                }
//                .waitFor() == 0
    }

    private fun clearBucket(cloudSdkPath: File, bucketName: String){
        val processCreator = ProcessBuilder("""${command("gsutil", cloudSdkPath)} rm gs://$bucketName/**""".asCommand())
        val process = processCreator.start()

        process.errorStream.bufferedReader().forEachLine { logger.lifecycle(it) }
        process.inputStream.bufferedReader().forEachLine { logger.lifecycle(it) }

        process.waitFor()
    }
}