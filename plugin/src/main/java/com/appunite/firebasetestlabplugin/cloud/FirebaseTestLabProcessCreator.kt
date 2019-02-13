package com.appunite.firebasetestlabplugin.cloud

import com.appunite.firebasetestlabplugin.FirebaseTestLabPlugin
import com.appunite.firebasetestlabplugin.model.Device
import com.appunite.firebasetestlabplugin.model.TestResults
import com.appunite.firebasetestlabplugin.utils.joinArgs
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

    fun callFirebaseTestLab(device: Device, apk: File, testType: TestType, shardIndex: Int = -1): TestResults {
        val processBuilder = createProcess(device, apk, testType)
        logger.debug(processBuilder.command().joinToString(separator = " ", transform = { "\"$it\"" }))
        val process = processBuilder.start()
        process.errorStream.bufferedReader().forEachLine { logger.lifecycle(it) }
        process.inputStream.bufferedReader().forEachLine { logger.lifecycle(it) }

        val resultCode = process.waitFor()
        return TestResults(
                isSuccessful = resultCode == 0,
                message = resultMessageMap[resultCode] ?: ""
        )
    }
    
    private fun createProcess(device: Device, apk: File, testType: TestType, shardIndex: Int = -1): ProcessBuilder {
        return ProcessBuilder(
            sequenceOf(
                sdk.gcloud.absolutePath,
                "firebase", "test", "android", "run",
                "--format=json",
                "--device-ids=${device.deviceIds.joinArgs()}",
                "--app=$apk",
                "--locales=${device.locales.joinArgs()}",
                "--os-version-ids=${device.androidApiLevels.joinArgs()}",
                "--orientations=${device.screenOrientations.map { orientation -> orientation.gcloudName }.joinArgs()}")
                .plus(when (testType) {
                    TestType.Robo -> sequenceOf("--type=robo")
                    is TestType.Instrumentation -> sequenceOf("--type=instrumentation", "--test=${testType.testApk}")
                })
                .plus(gCloudBucketName?.let { sequenceOf("--results-bucket=$it") } ?: sequenceOf())
                .plus(gCloudDirectory?.let { sequenceOf("--results-dir=$it") } ?: sequenceOf())
                .plus(if (device.isUseOrchestrator) sequenceOf("--use-orchestrator") else sequenceOf())
                .plus(if (device.environmentVariables.isNotEmpty()) sequenceOf("--environment-variables=${device.environmentVariables.joinToString(",")}${addSharding(device, shardIndex)}") else sequenceOf())
                .plus(if (device.testTargets.isNotEmpty()) sequenceOf("--test-targets=${device.testTargets.joinToString(",")}") else sequenceOf())
                .plus(device.customParamsForGCloudTool)
                .plus(device.testRunnerClass?.let { sequenceOf("--test-runner-class=$it") } ?: sequenceOf())
                .plus(if (device.timeout > 0) sequenceOf("--timeoutSec=${device.timeout}s") else sequenceOf())
                .toList()
        )
    }
    
    private fun addSharding(device: Device, shardIndex: Int): String = when {
        device.numShards <= 0 && shardIndex >= 0 -> ", numShards=${device.numShards}, shardIndex=$shardIndex"
        else -> ""
    }
}