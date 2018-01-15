package com.appunite.firebasetestlabplugin.cloud

import com.appunite.firebasetestlabplugin.model.ResultTypes
import com.appunite.firebasetestlabplugin.utils.Constants
import com.appunite.firebasetestlabplugin.utils.asCommand
import com.appunite.firebasetestlabplugin.utils.command
import org.gradle.api.GradleException
import org.gradle.api.logging.Logger
import java.io.File


class CloudTestResultDownloader(private val resultsTypes: ResultTypes,
                                private val gCloudSdkPath: File,
                                private val gCloudDirectory: File,
                                private val gCloudBucketName: String,
                                private val logger: Logger) {

    fun getResults() {
        clearBucket()
        if (!resultsTypes.junit && !resultsTypes.logcat && !resultsTypes.video && !resultsTypes.xml) {
            return
        }
        val gCloudFullPath = "$gCloudBucketName/$gCloudDirectory"
        logger.lifecycle(Constants.DOWNLOAD_PHASE + "Downloading results from $gCloudFullPath")

        prepareDownloadDirectory()
        downloadTestResults()
    }

    private fun prepareDownloadDirectory() {
        gCloudDirectory.mkdirs()
        if (!gCloudDirectory.exists()) {
            throw GradleException("Issue when creating destination dir $gCloudDirectory")
        }
    }

    private fun downloadTestResults() {
        val excludeQuery = StringBuilder().append("-x \".*\\.txt$|.*\\.apk$")
        if (!resultsTypes.xml) {
            excludeQuery.append("|.*\\.xml$")
        }
        if (!resultsTypes.xml) {
            excludeQuery.append("|.*\\.results$")
        }
        if (!resultsTypes.logcat) {
            excludeQuery.append("|.*\\logcat$")
        }
        if (!resultsTypes.video) {
            excludeQuery.append("|.*\\.mp4$")
        }
        excludeQuery.append("|.*\\.txt$\"").toString()
        val processCreator = ProcessBuilder("""${command("gsutil", gCloudSdkPath)} -m rsync $excludeQuery -r gs://$gCloudSdkPath $gCloudDirectory""".asCommand())
        val process = processCreator.start()

        process.errorStream.bufferedReader().forEachLine { logger.lifecycle(it) }
        process.inputStream.bufferedReader().forEachLine { logger.lifecycle(it) }

        process.waitFor()
    }

    private fun clearBucket() {
        val processCreator = ProcessBuilder("""${command("gsutil", gCloudSdkPath)} rm gs://$gCloudBucketName/**""".asCommand())
        val process = processCreator.start()

        process.errorStream.bufferedReader().forEachLine { logger.lifecycle(it) }
        process.inputStream.bufferedReader().forEachLine { logger.lifecycle(it) }

        process.waitFor()
    }
}