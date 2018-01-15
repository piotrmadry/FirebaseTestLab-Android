package com.appunite.firebasetestlabplugin.cloud

import com.appunite.firebasetestlabplugin.model.Device
import com.appunite.firebasetestlabplugin.model.TestResults
import com.appunite.firebasetestlabplugin.model.TestType
import com.appunite.firebasetestlabplugin.utils.ApkSource
import com.appunite.firebasetestlabplugin.utils.Constants.Companion.RESULT_SUCCESSFUL
import com.appunite.firebasetestlabplugin.utils.asCommand
import com.appunite.firebasetestlabplugin.utils.command
import com.appunite.firebasetestlabplugin.utils.joinArgs
import org.gradle.api.logging.Logger
import java.io.File


internal class FirebaseTestLabProcessCreator(private val gCloudBucketName: String?,
                                             private val gCloudDirectory: String?,
                                             private val gCloudSdkPath: File,
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

    fun callFirebaseTestLab(testType: TestType, device: Device, apkSource: ApkSource): TestResults {

        val processBuilder = ProcessBuilder("""
        ${command("gcloud", gCloudSdkPath)}
                firebase test android run
                --format json
                --results-bucket $gCloudBucketName
                --results-dir $gCloudDirectory
                --type ${testType.toString().toLowerCase()}
                --locales ${device.locales.joinArgs()},
                --os-version-ids ${device.androidApiLevels.joinArgs()}
                --orientations ${device.screenOrientations.map { orientation -> orientation.toString().toLowerCase()}.joinArgs()}
                --device-ids ${device.deviceIds.joinArgs()}
                --app ${apkSource.apk}
                ${if (testType == TestType.INSTRUMENTATION) "--test ${apkSource.testApk}" else ""}
                ${if (device.timeout > 0) "--timeoutSec ${device.timeout}s" else ""}
    """.asCommand())

        val process = processBuilder.start()
        val gCloudFullPath = "$gCloudBucketName/$gCloudDirectory"

        process.errorStream.bufferedReader().forEachLine { logger.lifecycle(it) }
        process.inputStream.bufferedReader().forEachLine { logger.lifecycle(it) }

        val resultCode = process.waitFor()
        return TestResults(
                isSuccessful = resultCode == RESULT_SUCCESSFUL,
                resultDir = gCloudFullPath,
                message = resultMessageMap[resultCode] ?: ""
        )
    }
}