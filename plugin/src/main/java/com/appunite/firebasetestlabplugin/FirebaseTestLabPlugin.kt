package com.appunite.firebasetestlabplugin

import com.android.build.VariantOutput
import com.android.build.gradle.AppExtension
import com.android.build.gradle.api.BaseVariantOutput
import com.android.build.gradle.api.TestVariant
import com.appunite.firebasetestlabplugin.cloud.CloudTestResultDownloader
import com.appunite.firebasetestlabplugin.cloud.FirebaseTestLabProcessCreator
import com.appunite.firebasetestlabplugin.cloud.TestType
import com.appunite.firebasetestlabplugin.model.Device
import com.appunite.firebasetestlabplugin.model.TestResults
import com.appunite.firebasetestlabplugin.utils.Constants
import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.*
import org.gradle.api.tasks.Exec
import org.gradle.kotlin.dsl.closureOf
import org.gradle.kotlin.dsl.createTask
import org.gradle.kotlin.dsl.task
import java.io.ByteArrayOutputStream
import java.io.File

internal class FirebaseTestLabPlugin : Plugin<Project> {

    open class HiddenExec : Exec() {
        init {
            standardOutput = ByteArrayOutputStream()
            errorOutput = standardOutput
            isIgnoreExitValue = true

            doLast {
                if (execResult.exitValue != 0) {
                    println(standardOutput.toString())
                    throw GradleException("exec failed; see output above")
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
            setup()
        }
    }

    data class Sdk(val gcloud: File, val gsutil: File)

    private fun createDownloadSdkTask(project: Project, cloudSdkPath: String?): Sdk =
            if (cloudSdkPath != null) {
                val sdkPath = File(cloudSdkPath)
                val gcloud = File(sdkPath, Constants.GCLOUD)
                val gsutil = File(sdkPath, Constants.GSUTIL)

                project.task(ensureGCloudSdk, {
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
                })
                Sdk(gcloud, gsutil)
            } else {
                val env = System.getenv("CLOUDSDK_INSTALL_DIR")
                val installDir = when {
                    !env.isNullOrEmpty() -> File(env)
                    else -> File(project.buildDir, "gcloud")
                }

                project.logger.lifecycle("gCloud sdk installation dir: $installDir")
                val cloudSdkDir = File(installDir, "google-cloud-sdk")
                val sdkPath = File(cloudSdkDir, "bin")
                project.logger.lifecycle("gCloud sdk path: $sdkPath")

                val gcloud = File(sdkPath, Constants.GCLOUD)
                val gsutil = File(sdkPath, Constants.GSUTIL)

                project.createTask(ensureGCloudSdk, HiddenExec::class, {
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
                })
                Sdk(gcloud, gsutil)
            }

    private fun setup() {
        project.extensions.findByType(FirebaseTestLabPluginExtension::class.java)?.apply {
            val devices = devices.toList()


            val sdk = createDownloadSdkTask(project, cloudSdkPath)

            project.createTask(taskAuth, HiddenExec::class, {
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
            })
            project.createTask(taskSetProject, HiddenExec::class, {
                group = Constants.FIREBASE_TEST_LAB
                description = "Configure google cloud sdk project"

                dependsOn(ensureGCloudSdk)
                doFirst {
                    if (googleProjectId == null) {
                        throw GradleException("You need to set firebaseTestLab.googleProjectId before run")
                    }
                }
                commandLine = listOf(sdk.gcloud.absolutePath, "config", "set", "project", "$googleProjectId")
            })
            project.task(taskSetup, {
                group = Constants.FIREBASE_TEST_LAB
                description = "Setup and configure google cloud sdk"

                dependsOn(taskSetProject)
                dependsOn(taskAuth)
            })


            val downloader: CloudTestResultDownloader? = if (cloudBucketName != null) {
                CloudTestResultDownloader(
                        sdk,
                        resultsTypes,
                        File(cloudDirectoryName),
                        File(project.buildDir, cloudDirectoryName),
                        cloudBucketName!!,
                        project.logger
                )
            } else {
                null
            }

            if (clearDirectoryBeforeRun && downloader == null) {
                throw IllegalStateException("If you want to clear directory before run you need to setup cloudBucketName")
            }

            val firebaseTestLabProcessCreator = FirebaseTestLabProcessCreator(
                    sdk,
                    cloudBucketName,
                    cloudDirectoryName,
                    project.logger
            )

            (project.extensions.findByName(ANDROID) as AppExtension).apply {
                testVariants.toList().forEach { testVariant ->
                    createGroupedTestLabTask(devices, testVariant, firebaseTestLabProcessCreator, ignoreFailures, downloader)
                }
            }


        }
    }

    data class DeviceAppMap(val device: Device, val apk: BaseVariantOutput)

    data class Test(val device: Device, val apk: BaseVariantOutput, val testApk: BaseVariantOutput)

    private fun createGroupedTestLabTask(
            devices: List<Device>,
            variant: TestVariant,
            firebaseTestLabProcessCreator: FirebaseTestLabProcessCreator,
            ignoreFailures: Boolean,
            downloader: CloudTestResultDownloader?) {
        val variantName = variant.testedVariant?.name?.capitalize() ?: ""

        val cleanTask = "firebaseTestLabClean${variantName.capitalize()}"

        val runTestsTask = "firebaseTestLabExecute${variantName.capitalize()}"
        val runTestsTaskInstrumentation = "${runTestsTask}Instrumentation"
        val runTestsTaskRobo = "${runTestsTask}Robo"
        val downloadTask = "firebaseTestLabDownload${variantName.capitalize()}"

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

        val appVersions = combineAll(devices, variant.testedVariant.outputs, ::DeviceAppMap)
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
        val roboTasks = appVersions
                .map {
                    test ->
                    val devicePart = test.device.name.capitalize()
                    val apkPart = dashToCamelCase(test.apk.name).capitalize()
                    val taskName = "$runTestsTaskRobo$devicePart$apkPart"
                    project.task(taskName, closureOf<Task> {
                        inputs.files(test.apk.outputFile)
                        group = Constants.FIREBASE_TEST_LAB
                        description = "Run Robo test for ${test.device.name} device on $variantName/${test.apk.name} in Firebase Test Lab"
                        if (downloader != null) {
                            mustRunAfter(cleanTask)
                        }
                        dependsOn(taskSetup)
                        dependsOn(arrayOf(test.apk.assemble))
                        doLast {
                            val result = firebaseTestLabProcessCreator.callFirebaseTestLab(test.device, test.apk.outputFile, TestType.Robo)
                            processResult(result, ignoreFailures)
                        }
                    })
                }

        val instrumentationTasks = combineAll(appVersions, variant.outputs, {deviceAndMap, testApk -> Test(deviceAndMap.device, deviceAndMap.apk, testApk)})
                .map {
                    test ->
                    val devicePart = test.device.name.capitalize()
                    val apkPart = dashToCamelCase(test.apk.name).capitalize()
                    val testApkPart = test.testApk.let { if (it.filters.isEmpty()) "" else dashToCamelCase(it.name).capitalize() }
                    val taskName = "$runTestsTaskInstrumentation$devicePart$apkPart$testApkPart"
                    project.task(taskName, closureOf<Task> {
                        inputs.files(test.testApk.outputFile, test.apk.outputFile)
                        group = Constants.FIREBASE_TEST_LAB
                        description = "Run Instrumentation test for ${test.device.name} device on $variantName/${test.apk.name} in Firebase Test Lab"
                        if (downloader != null) {
                            mustRunAfter(cleanTask)
                        }
                        dependsOn(taskSetup)
                        dependsOn(arrayOf(test.apk.assemble, test.testApk.assemble))
                        doLast {
                            val result = firebaseTestLabProcessCreator.callFirebaseTestLab(test.device, test.apk.outputFile, TestType.Instrumentation(test.testApk.outputFile))
                            processResult(result, ignoreFailures)
                        }
                    })
                }

        val allInstrumentation = project.task(runTestsTaskInstrumentation, closureOf<Task> {
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

        val allRobo = project.task(runTestsTaskRobo, closureOf<Task> {
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
            project.task(downloadTask, closureOf<Task> {
                group = Constants.FIREBASE_TEST_LAB
                description = "Run Android Tests in Firebase Test Lab and download artifacts from google storage"
                dependsOn(taskSetup)
                dependsOn(runTestsTask)
                mustRunAfter(cleanTask)

                doLast {
                    downloader.getResults()
                }
            })
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
}


private fun <T1, T2, R> combineAll(l1: Collection<T1>, l2: Collection<T2>, func: (T1, T2) -> R): List<R> =
        l1.flatMap { t1 -> l2.map { t2 -> func(t1, t2)} }

private fun dashToCamelCase(dash: String): String =
        dash.split('-', '_').joinToString("") { it.capitalize() }