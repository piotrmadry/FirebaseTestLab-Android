package com.appunite.firebasetestlabplugin.cloud

import com.appunite.firebasetestlabplugin.FirebaseTestLabPlugin
import com.appunite.firebasetestlabplugin.model.Device
import com.appunite.firebasetestlabplugin.model.TestResults
import com.appunite.firebasetestlabplugin.utils.*
import org.gradle.api.logging.Logger
import java.io.File

sealed class TestType {
    object Robo : TestType()
    data class Instrumentation(val testApk: File) : TestType()
}

internal class FirebaseTestLabProcessCreator(
        private val sdk: FirebaseTestLabPlugin.Sdk,
        private val gCloudBucketName: String?,
        private val gCloudDirectory: String?,
        private val logger: Logger) {

    private val resultMessageMap = mapOf(
            0 to "All test executions passed.",
            1 to "A general failure occurred. Possible causes include: a filename that does not exist, or an HTTP/network error.",
            2 to "Testing exited because unknown commands or arguments were provided.",
            10 to "One or more test cases (tested classes or class methods) within a test execution did not pass.",
            15 to "Firebase Test Lab for Android could not determine if the test matrix passed or failed because of an unexpected error.",
            18 to "The test environment for this test execution is not supported because of incompatible test dimensions. This error might occur if the selected Android API level is not supported by the selected device type.",
            19 to "The test matrix was canceled by the user.",
            20 to "A test infrastructure error occurred."
    )

    fun callFirebaseTestLab(device: Device, apk: File, testType: TestType): TestResults {
        val type = when (testType) {
            TestType.Robo -> "--type robo"
            is TestType.Instrumentation -> "--instrumentation instrumentation --test ${testType.testApk}"
        }

        val processBuilder = ProcessBuilder("""
        ${sdk.gcloud.absolutePath}
                firebase test android run
                --format json
                ${if (gCloudBucketName != null) "--results-bucket $gCloudBucketName" else ""}
                ${if (gCloudDirectory != null) "--results-dir $gCloudDirectory" else ""}
                $type
                --locales ${device.locales.joinArgs()},
                --os-version-ids ${device.androidApiLevels.joinArgs()}
                --orientations ${device.screenOrientations.map { orientation -> orientation.gcloudName }.joinArgs()}
                --device-ids ${device.deviceIds.joinArgs()}
                --app $apk
                ${if (device.timeout > 0) "--timeoutSec ${device.timeout}s" else ""}
    """.asCommand())
        val process = processBuilder.start()
        process.errorStream.bufferedReader().forEachLine { logger.lifecycle(it) }
        process.inputStream.bufferedReader().forEachLine { logger.lifecycle(it) }

        val resultCode = process.waitFor()
        return TestResults(
                isSuccessful = resultCode == 0,
                message = resultMessageMap[resultCode] ?: ""
        )
    }
}