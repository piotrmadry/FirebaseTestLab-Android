package com.appunite.firebasetestlabplugin

import com.android.build.VariantOutput
import com.android.build.gradle.AppExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.TestedExtension
import com.android.build.gradle.api.ApkVariant
import com.android.build.gradle.api.ApplicationVariant
import com.android.build.gradle.api.BaseVariantOutput
import com.android.build.gradle.api.ApkVariantOutput
import com.android.build.gradle.api.TestVariant
import com.appunite.firebasetestlabplugin.cloud.CloudTestResultDownloader
import com.appunite.firebasetestlabplugin.cloud.FirebaseTestLabProcessCreator
import com.appunite.firebasetestlabplugin.cloud.ProcessData
import com.appunite.firebasetestlabplugin.cloud.TestType
import com.appunite.firebasetestlabplugin.model.Device
import com.appunite.firebasetestlabplugin.model.TestResults
import com.appunite.firebasetestlabplugin.tasks.InstrumentationShardingTask
import com.appunite.firebasetestlabplugin.utils.Constants
import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.Exec
import org.gradle.kotlin.dsl.closureOf
import org.gradle.kotlin.dsl.register
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.Serializable


class FirebaseTestLabPlugin : Plugin<Project> {

    open class HiddenExec : Exec() {
        init {
            standardOutput = ByteArrayOutputStream()
            errorOutput = standardOutput
            isIgnoreExitValue = true

            doLast {
                execResult?.let {
                    if (it.exitValue != 0) {
                        println(standardOutput.toString())
                        throw GradleException("exec failed; see output above")
                    }
                }
            }
        }
    }

    companion object {
        private const val GRADLE_METHOD_NAME = "firebaseTestLab"
        private const val ANDROID = "android"
        private const val ensureGCloudSdk = "firebaseTestLabEnsureGCloudSdk"
        private const val taskAuth = "firebaseTestLabAuth"
        private const val taskSetup = "firebaseTestLabSetup"
        private const val taskSetProject = "firebaseTestLabSetProject"
        private const val taskPrefixDownload = "firebaseTestLabDownload"
        private const val taskPrefixExecute = "firebaseTestLabExecute"
        private const val BLANK_APK_RESOURCE_PATH = "/blank.apk"

        private val BLANK_APK_RESOURCE = FirebaseTestLabPlugin::class.java.getResource(BLANK_APK_RESOURCE_PATH)
    }

    private lateinit var project: Project

    /**
     * Create extension used to configure testing properties, platforms..
     * After that @param[setup] check for required fields validity
     * and throw @param[GradleException] if needed
     */
    override fun apply(project: Project) {
        this.project = project
        project.extensions.create(
                GRADLE_METHOD_NAME,
                FirebaseTestLabPluginExtension::class.java,
                project)

        project.afterEvaluate {
            logger.lifecycle("*************** Firebase Test Lab Plugin ***************")
            setup()
            logger.lifecycle("********************************************************")
        }
    }

    data class Sdk(val gcloud: File, val gsutil: File): Serializable

    private fun createDownloadSdkTask(project: Project, cloudSdkPath: String?): Sdk =
            if (cloudSdkPath != null) {
                val sdkPath = File(cloudSdkPath)
                val gcloud = File(sdkPath, Constants.GCLOUD)
                val gsutil = File(sdkPath, Constants.GSUTIL)

                project.tasks.register<Task>(ensureGCloudSdk) {
                    group = Constants.FIREBASE_TEST_LAB
                    description = "Check if google cloud sdk is installed"

                    doFirst {
                        if (!gcloud.exists()) {
                            throw IllegalStateException("gcloud does not exist in path ${sdkPath.absoluteFile}, but downloading is not supported on Windows")
                        }
                        if (!gsutil.exists()) {
                            throw IllegalStateException("gsutil does not exist in path ${sdkPath.absoluteFile}, but downloading is not supported on Windows")
                        }
                    }
                }
                Sdk(gcloud, gsutil)
            } else {
                val env = System.getenv("CLOUDSDK_INSTALL_DIR")
                val installDir = when {
                    !env.isNullOrEmpty() -> File(env)
                    else -> File(project.buildDir, "gcloud")
                }

                project.logger.lifecycle("Google Cloud SDK installed at: $installDir")
                val cloudSdkDir = File(installDir, "google-cloud-sdk")
                val sdkPath = File(cloudSdkDir, "bin")

                val gcloud = File(sdkPath, Constants.GCLOUD)
                val gsutil = File(sdkPath, Constants.GSUTIL)

                project.tasks.register<HiddenExec>(ensureGCloudSdk) {
                    group = Constants.FIREBASE_TEST_LAB
                    description = "Install google cloud SDK if necessary"

                    outputs.files(gcloud, gsutil)
                    doFirst {
                        if (Os.isFamily(Os.FAMILY_WINDOWS)) {
                            throw IllegalStateException("Fetching gcloud and gsutil is not supported on Windows. " +
                                    "You need to install it manually. Look for instructions: https://cloud.google.com/sdk/downloads#windows ." +
                                    "Than you need to set:\n " +
                                    "firebaseTestLab {\n" +
                                    "  cloudSdkPath = \"Xyz\"\n" +
                                    "}\n")
                        }
                    }
                    commandLine = listOf("bash", "-c", "rm -r \"${cloudSdkDir.absolutePath}\";export CLOUDSDK_CORE_DISABLE_PROMPTS=1 && export CLOUDSDK_INSTALL_DIR=\"${installDir.absolutePath}\" && curl https://sdk.cloud.google.com | bash")
                    doLast {
                        if (!gcloud.exists()) throw IllegalStateException("Installation failed")
                        if (!gsutil.exists()) throw IllegalStateException("Installation failed")
                    }
                }
                Sdk(gcloud, gsutil)
            }
    
    sealed class ExtensionType(open val testVariant: TestVariant) {
        data class Library(override val testVariant: TestVariant) : ExtensionType(testVariant)
        data class Application(override val testVariant: TestVariant, val appVariant: ApplicationVariant) : ExtensionType(testVariant)
    }

    private fun setup() {
        project.extensions.findByType(FirebaseTestLabPluginExtension::class.java)?.apply {
            val devices = devices.toList()

            val sdk = createDownloadSdkTask(project, cloudSdkPath)

            project.tasks.register<HiddenExec>(taskAuth) {
                group = Constants.FIREBASE_TEST_LAB
                description = "Authorize google cloud sdk"

                dependsOn(ensureGCloudSdk)
                val keyFile = keyFile
                doFirst {
                    if (keyFile == null) {
                        throw GradleException("You need to set firebaseTestLab.keyFile = file(\"key-file.json\") before run")
                    } else if (!keyFile.exists()) {
                        throw GradleException("Key file (${keyFile.absolutePath} does not exists")
                    }
                }
                commandLine = listOf(sdk.gcloud.absolutePath, "auth", "activate-service-account", "--key-file=${keyFile?.absolutePath}")
            }
            project.tasks.register<HiddenExec>(taskSetProject) {
                group = Constants.FIREBASE_TEST_LAB
                description = "Configure google cloud sdk project"

                dependsOn(ensureGCloudSdk)
                doFirst {
                    if (googleProjectId == null) {
                        throw GradleException("You need to set firebaseTestLab.googleProjectId before run")
                    }
                }
                commandLine = listOf(sdk.gcloud.absolutePath, "config", "set", "project", "$googleProjectId")
            }
            project.tasks.register<Task>(taskSetup) {
                group = Constants.FIREBASE_TEST_LAB
                description = "Setup and configure google cloud sdk"

                dependsOn(taskSetProject)
                dependsOn(taskAuth)
            }


            val downloader: CloudTestResultDownloader? = if (cloudBucketName != null && cloudDirectoryName != null) {
                CloudTestResultDownloader(
                        sdk,
                        resultsTypes,
                        File(cloudDirectoryName!!),
                        File(project.buildDir, cloudDirectoryName!!),
                        cloudBucketName!!,
                        project.logger
                )
            } else {
                null
            }

            if (clearDirectoryBeforeRun && downloader == null) {
                throw IllegalStateException("If you want to clear directory before run you need to setup cloudBucketName and cloudDirectoryName")
            }

            val androidExtension: Any? = project.extensions.findByName(ANDROID)

            (androidExtension as TestedExtension).apply {
                testVariants.toList().forEach { testVariant ->
    
                    val extensionType: ExtensionType = when (androidExtension) {
                        is LibraryExtension -> ExtensionType.Library(testVariant)
                        is AppExtension -> ExtensionType.Application(testVariant, androidExtension.applicationVariants.toList().firstOrNull { it.buildType == testVariant.buildType && it.flavorName == testVariant.flavorName }!!)
                        else -> throw IllegalStateException("Only application and library modules are supported")
                    }
                    
                    createGroupedTestLabTask(devices, extensionType, ignoreFailures, downloader, sdk, cloudBucketName, cloudDirectoryName)
                }
            }
        }
    }

    data class DeviceAppMap(val device: Device, val apk: BaseVariantOutput)

    data class Test(val device: Device, val apk: BaseVariantOutput, val testApk: BaseVariantOutput): Serializable

    private fun createGroupedTestLabTask(
        devices: List<Device>,
        extension: ExtensionType,
        ignoreFailures: Boolean,
        downloader: CloudTestResultDownloader?,
        sdk: Sdk,
        cloudBucketName: String?,
        cloudDirectoryName: String?
    ) {
        val blankApk = createBlankApkForLibrary(project)
        val variantName = extension.testVariant.testedVariant?.name?.capitalize() ?: ""

        val cleanTask = "firebaseTestLabClean${variantName.capitalize()}"

        val variantSuffix = variantName.capitalize()
        val runTestsTask = taskPrefixExecute + variantSuffix
        val runTestsTaskInstrumentation = "${runTestsTask}Instrumentation"
        val runTestsTaskRobo = "${runTestsTask}Robo"

        if (downloader != null) {
            project.task(cleanTask, closureOf<Task> {
                group = Constants.FIREBASE_TEST_LAB
                description = "Clean test lab artifacts on google storage"
                dependsOn(taskSetup)
                doLast {
                    downloader.clearResultsDir()
                }
            })
        }

        val appVersions = combineAll(devices, extension.testVariant.testedVariant.outputs, ::DeviceAppMap)
                .filter {
                    val hasAbiSplits = it.apk.filterTypes.contains(VariantOutput.ABI)
                    if (hasAbiSplits) {
                        if (it.device.filterAbiSplits) {
                            val abi = it.apk.filters.first { it.filterType == VariantOutput.ABI }.identifier
                            it.device.abiSplits.contains(abi)
                        } else {
                            true
                        }
                    } else {
                        it.device.testUniversalApk
                    }
                }
        
        /* Not applicable for library module */
        
        val roboTasks = if (extension is ExtensionType.Library) emptyList() else {
            appVersions
                .map { test ->
                    val devicePart = test.device.name.capitalize()
                    val apkPart = dashToCamelCase(test.apk.name).capitalize()
                    val taskName = "$runTestsTaskRobo$devicePart$apkPart"
                    project.task(taskName, closureOf<Task> {
                        inputs.files(resolveUnderTestApk(extension, test.apk, blankApk))
                        group = Constants.FIREBASE_TEST_LAB
                        description = "Run Robo test for ${test.device.name} device on $variantName/${test.apk.name} in Firebase Test Lab"
                        if (downloader != null) {
                            mustRunAfter(cleanTask)
                        }
                        dependsOn(taskSetup)
                        dependsOn(arrayOf(resolveAssemble(extension.testVariant)))
                        doLast {
                            val result = FirebaseTestLabProcessCreator.callFirebaseTestLab(ProcessData(
                                sdk = sdk,
                                gCloudBucketName = cloudBucketName,
                                gCloudDirectory = cloudDirectoryName,
                                device = test.device,
                                apk = resolveUnderTestApk(extension, test.apk, blankApk),
                        
                                testType = TestType.Robo
                            ))
                            processResult(result, ignoreFailures)
                        }
                    })
                }
        }

        val testResultFile = File(project.buildDir, "TestResults.txt")

        val instrumentationTasks: List<Task> = combineAll(appVersions, extension.testVariant.outputs)
        { deviceAndMap, testApk -> Test(deviceAndMap.device, deviceAndMap.apk, testApk) }
            .map { test ->
                val devicePart = test.device.name.capitalize()
                val apkPart = dashToCamelCase(test.apk.name).capitalize()
                val testApkPart = test.testApk.let { if (it.filters.isEmpty()) "" else dashToCamelCase(it.name).capitalize() }
                val taskName = "$runTestsTaskInstrumentation$devicePart$apkPart$testApkPart"
                val numShards = test.device.numShards

                val apkUnderTest = resolveUnderTestApk(extension, test.apk, blankApk)
                val testApk = resolveApk(extension.testVariant, test.testApk)
                val processData = ProcessData(
                    sdk = sdk,
                    gCloudBucketName = cloudBucketName,
                    gCloudDirectory = cloudDirectoryName,
                    device = test.device,
                    apk = apkUnderTest,
                    testType = TestType.Instrumentation(testApk)
                )

                if (numShards > 0) {
                    project.tasks.create(taskName, InstrumentationShardingTask::class.java) {
                        group = Constants.FIREBASE_TEST_LAB
                        description = "Run Instrumentation test for ${test.device.name} device on $variantName/${test.apk.name} in Firebase Test Lab"
                        this.processData = processData
                        this.stateFile = testResultFile

                        if (downloader != null) {
                            mustRunAfter(cleanTask)
                        }
                        dependsOn(taskSetup)
                        dependsOn(arrayOf(resolveAssemble(extension.testVariant), resolveTestAssemble(extension.testVariant)))
                        
                        doFirst {
                            testResultFile.writeText("")
                        }

                        doLast {
                            val testResults = testResultFile.readText()

                            logger.lifecycle("TESTS RESULTS: Every digit represents single shard.")
                            logger.lifecycle("\"0\" means -> tests for particular shard passed.")
                            logger.lifecycle("\"1\" means -> tests for particular shard failed.")

                            logger.lifecycle("RESULTS_CODE: $testResults")
                            logger.lifecycle("When result code is equal to 0 means that all tests for all shards passed, otherwise some of them failed.")
    
    
                            processResult(testResults, ignoreFailures)
                            
                        }
                    }

                } else {
                    project.task(taskName, closureOf<Task> {
                        inputs.files(testApk, apkUnderTest)
                        group = Constants.FIREBASE_TEST_LAB
                        description = "Run Instrumentation test for ${test.device.name} device on $variantName/${test.apk.name} in Firebase Test Lab"
                        if (downloader != null) {
                            mustRunAfter(cleanTask)
                        }
                        dependsOn(taskSetup)
                        dependsOn(arrayOf(resolveAssemble(extension.testVariant), resolveTestAssemble(extension.testVariant)))
                        doLast {
                            logger.log(LogLevel.INFO, "Run instrumentation tests for ${this.name}")
                            logger.log(LogLevel.DEBUG, "ProcessData for test: $processData")
                            val result = FirebaseTestLabProcessCreator.callFirebaseTestLab(processData)
                            logger.log(LogLevel.INFO, "Result of ${this.name}: $result")
                            processResult(result, ignoreFailures)
                        }
                    })
                }
            }

        val allInstrumentation: Task = project.task(runTestsTaskInstrumentation, closureOf<Task> {
            group = Constants.FIREBASE_TEST_LAB
            description = "Run all Instrumentation tests for $variantName in Firebase Test Lab"
            dependsOn(instrumentationTasks)

            doFirst {
                if (devices.isEmpty()) throw IllegalStateException("You need to set et least one device in:\n" +
                        "firebaseTestLab {" +
                        "  devices {\n" +
                        "    nexus6 {\n" +
                        "      androidApiLevels = [21]\n" +
                        "      deviceIds = [\"Nexus6\"]\n" +
                        "      locales = [\"en\"]\n" +
                        "    }\n" +
                        "  } " +
                        "}")

                if (instrumentationTasks.isEmpty()) throw IllegalStateException("Nothing match your filter")
            }
        })

        val allRobo: Task = project.task(runTestsTaskRobo, closureOf<Task> {
            group = Constants.FIREBASE_TEST_LAB
            description = "Run all Robo tests for $variantName in Firebase Test Lab"
            dependsOn(roboTasks)

            doFirst {
                if (devices.isEmpty()) throw IllegalStateException("You need to set et least one device in:\n" +
                        "firebaseTestLab {" +
                        "  devices {\n" +
                        "    nexus6 {\n" +
                        "      androidApiLevels = [21]\n" +
                        "      deviceIds = [\"Nexus6\"]\n" +
                        "      locales = [\"en\"]\n" +
                        "    }\n" +
                        "  } " +
                        "}")

                if (roboTasks.isEmpty()) throw IllegalStateException("Nothing match your filter")
            }
        })

        project.task(runTestsTask, closureOf<Task> {
            group = Constants.FIREBASE_TEST_LAB
            description = "Run all tests for $variantName in Firebase Test Lab"
            dependsOn(allRobo, allInstrumentation)
        })

        if (downloader != null) {
            listOf(variantSuffix, "${variantSuffix}Instrumentation").map{suffix ->
               project.task(taskPrefixDownload + suffix, closureOf<Task> {
                    group = Constants.FIREBASE_TEST_LAB
                    description = "Run Android Tests in Firebase Test Lab and download artifacts from google storage"
                    dependsOn(taskSetup)
                    dependsOn(taskPrefixExecute + suffix)
                    mustRunAfter(cleanTask)

                    doLast {
                        downloader.getResults()
                    }
                })
            }
        }
    }

    private fun processResult(result: TestResults, ignoreFailures: Boolean) {
        if (result.isSuccessful) {
            project.logger.lifecycle(result.message)
        } else {
            if (ignoreFailures) {
                project.logger.error("Error: ${result.message}")
            } else {
                throw GradleException(result.message)
            }
        }
    }

    private fun processResult(results: String, ignoreFailures: Boolean) =
        if (!results.contains("1")) {
            project.logger.lifecycle("SUCCESS: All tests passed.")
        } else {
            if (ignoreFailures) {
                println("FAILURE: Tests failed.")
                project.logger.error("FAILURE: Tests failed.")
            } else {
                throw GradleException("FAILURE: Tests failed.")
            }
        }
    
    /**
     * This file is used for library testing, normally, for apps, two APK files are provided, one with app and second
     * with tests. In case of library modules we have AAR (library archive) and APK with tests but Test Lab is not
     * accepting AAR as valid input (only APK). In fact library module APK file contains library module code and
     * testing code so single APK is sufficient to do all required tests but Firebase is still requiring APK to test,
     * so this is this missing APK required by Test Lab, even if it's not related to library module Firebase will
     * accept it and instrumentation tests will ignore it.
     */
    private fun createBlankApkForLibrary(project: Project): File {
        if (!project.buildDir.exists()) {
            if (!project.buildDir.mkdirs()) {
                throw IllegalStateException("Unable to create build dir ${project.buildDir}")
            }
        }
        
        val blankApk = File(project.buildDir, "blank.apk")
        
        if (!blankApk.exists()) {
            try {
                BufferedInputStream(BLANK_APK_RESOURCE.openStream()).use { inputStream ->
                    FileOutputStream(blankApk).use { fileOutputStream ->
                        val data = ByteArray(1024)
                        var byteContent = 0
                        while ({ byteContent = inputStream.read(data, 0, 1024); byteContent }() != -1) {
                            fileOutputStream.write(data, 0, byteContent)
                        }
                    }
                }
            } catch (e: IOException) {
                throw IllegalStateException("Unable to extract Blank APK file", e)
            }
        }
        return blankApk
    }
}

private fun resolveTestAssemble(variant: TestVariant): Task = try {
    variant.assembleProvider.get()
} catch (e: IllegalStateException) {
    variant.assemble
}

private fun resolveAssemble(variant: TestVariant): Task = try {
    variant.testedVariant.assembleProvider.get()
} catch (e: IllegalStateException) {
    variant.testedVariant.assemble
}

private fun resolveApk(variant: ApkVariant, baseVariantOutput: BaseVariantOutput): File =
    try {
        val applicationProvider = variant.packageApplicationProvider.get()
        applicationProvider.let {
            val filename = if (baseVariantOutput is ApkVariantOutput) {
                baseVariantOutput.outputFileName
            } else {
                it.apkNames.toList()[0]
            }
            File(it.outputDirectory.get().asFile, filename)
        }
    } catch (e: Exception) {
        when (e) {
            is IllegalStateException, is IndexOutOfBoundsException -> baseVariantOutput.outputFile
            else -> throw e
        }
    }

private fun resolveUnderTestApk(extension: FirebaseTestLabPlugin.ExtensionType, baseVariantOutput: BaseVariantOutput, blankApk: File): File =
    when (extension){
        is FirebaseTestLabPlugin.ExtensionType.Library -> blankApk
        is FirebaseTestLabPlugin.ExtensionType.Application -> resolveApk(extension.appVariant, baseVariantOutput)
    }

private fun <T1, T2, R> combineAll(l1: Collection<T1>, l2: Collection<T2>, func: (T1, T2) -> R): List<R> =
        l1.flatMap { t1 -> l2.map { t2 -> func(t1, t2)} }

fun dashToCamelCase(dash: String): String =
        dash.split('-', '_').joinToString("") { it.capitalize() }
