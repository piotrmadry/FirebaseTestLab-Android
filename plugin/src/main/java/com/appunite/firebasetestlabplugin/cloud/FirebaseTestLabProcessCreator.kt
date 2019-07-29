package com.appunite.firebasetestlabplugin.cloud

import com.appunite.firebasetestlabplugin.FirebaseTestLabPlugin
import com.appunite.firebasetestlabplugin.model.Device
import com.appunite.firebasetestlabplugin.model.TestResults
import com.appunite.firebasetestlabplugin.utils.joinArgs
import java.io.File
import java.io.Serializable

sealed class TestType : Serializable {
    object Robo : TestType()
    data class Instrumentation(val testApk: File) : TestType()
}

data class ProcessData(
    val sdk: FirebaseTestLabPlugin.Sdk,
    val gCloudBucketName: String?,
    val gCloudDirectory: String?,
    val device: Device,
    val apk: File,
    val testType: TestType,
    val shardIndex: Int = -1
) : Serializable

object FirebaseTestLabProcessCreator {
    private var execute: (ProcessData) -> Process = { processData -> createProcess(processData).start() }
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

    fun setExecutor(executor: (ProcessData) -> Process) {
        execute = executor
    }

    fun callFirebaseTestLab(processData: ProcessData): TestResults {
        val process: Process = execute(processData)
        val resultCode: Int = process.let {
            it.errorStream.bufferedReader().forEachLine { errorInfo -> println(errorInfo) }
            it.inputStream.bufferedReader().forEachLine { info -> println(info) }
            process.waitFor()
        }
        return TestResults(
            isSuccessful = resultCode == 0,
            message = resultMessageMap.getOrElse(resultCode) { "Unknown error with code: $resultCode" }
        )
    }
    
    private fun createProcess(processData: ProcessData): ProcessBuilder {
        val device: Device = processData.device
        return ProcessBuilder(
            sequenceOf(
                processData.sdk.gcloud.absolutePath,
                "firebase", "test", "android", "run",
                "--format=json",
                "--device-ids=${device.deviceIds.joinArgs()}",
                "--app=${processData.apk}",
                "--locales=${device.locales.joinArgs()}",
                "--os-version-ids=${device.androidApiLevels.joinArgs()}",
                "--orientations=${device.screenOrientations.map { orientation -> orientation.gcloudName }.joinArgs()}")
                .plus(when (processData.testType) {
                    TestType.Robo -> sequenceOf("--type=robo")
                    is TestType.Instrumentation -> sequenceOf("--type=instrumentation", "--test=${processData.testType.testApk}")
                })
                .plus(processData.gCloudBucketName?.let { sequenceOf("--results-bucket=$it") } ?: sequenceOf())
                .plus(processData.gCloudDirectory?.let { sequenceOf("--results-dir=$it") } ?: sequenceOf())
                .plus(if (device.isUseOrchestrator) sequenceOf("--use-orchestrator") else sequenceOf())
                .plus(setupEnvironmentVariables(device, processData.shardIndex))
                .plus(if (device.testTargets.isNotEmpty()) sequenceOf("--test-targets=${device.testTargets.joinToString(",")}") else sequenceOf())
                .plus(device.customParamsForGCloudTool)
                .plus(device.testRunnerClass?.let { sequenceOf("--test-runner-class=$it") } ?: sequenceOf())
                .plus(if (device.timeout > 0) sequenceOf("--timeout=${device.timeout}s") else sequenceOf())
                .toList()
        )
    }
    
    private fun setupEnvironmentVariables(device: Device, shardIndex: Int): Sequence<String> =
        if (device.environmentVariables.isNotEmpty() || device.numShards > 0)
            sequenceOf(StringBuilder()
                .append("--environment-variables=")
                .append(if (device.environmentVariables.isNotEmpty()) device.environmentVariables.joinToString(",") else "")
                .append(if (device.environmentVariables.isNotEmpty() && device.numShards > 0) "," else "")
                .append(if (device.numShards > 0) "numShards=${device.numShards},shardIndex=$shardIndex" else "")
                .toString())
        else sequenceOf()
}
