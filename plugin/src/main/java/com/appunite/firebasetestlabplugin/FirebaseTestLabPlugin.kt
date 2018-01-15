package com.appunite.firebasetestlabplugin

import com.android.build.gradle.AppExtension
import com.android.build.gradle.api.TestVariant
import com.appunite.firebasetestlabplugin.cloud.CloudTestResultDownloader
import com.appunite.firebasetestlabplugin.cloud.FirebaseTestLabProcessCreator
import com.appunite.firebasetestlabplugin.model.Device
import com.appunite.firebasetestlabplugin.model.TestResults
import com.appunite.firebasetestlabplugin.model.TestType
import com.appunite.firebasetestlabplugin.utils.ApkSource
import com.appunite.firebasetestlabplugin.utils.Constants
import com.appunite.firebasetestlabplugin.utils.VariantApkSource
import com.appunite.firebasetestlabplugin.utils.capitalizeIt
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.kotlin.dsl.closureOf
import java.io.File

internal class FirebaseTestLabPlugin : Plugin<Project> {

    companion object {
        private const val GRADLE_METHOD_NAME = "firebaseTestLab"
        private const val ANDROID = "android"
        private const val TASK_NAME = "UploadTestLab"
        private const val TASK_NAME_GROUPED = "AllDevices" + TASK_NAME
    }

    private lateinit var project: Project
    private var extension: FirebaseTestLabPluginExtension? = null
    private lateinit var resultDownloader: CloudTestResultDownloader
    private lateinit var processCreator: FirebaseTestLabProcessCreator

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
            extension?.let { createTaskForAndroid() }
        }
    }

    private fun setup() {
        extension = project.extensions.findByType(FirebaseTestLabPluginExtension::class.java)?.apply {
            resultDownloader = CloudTestResultDownloader(
                    resultsTypes,
                    File(cloudSdkPath),
                    File(cloudDirectoryName),
                    File(project.buildDir, cloudDirectoryName),
                    cloudBucketName!!,
                    project.logger
            )

            processCreator = FirebaseTestLabProcessCreator(
                    cloudBucketName,
                    cloudDirectoryName,
                    File(cloudSdkPath),
                    project.logger
            )
        }
    }

    private fun createTaskForAndroid() {
        val devices = extension!!.devices.toList()
        (project.extensions.findByName(ANDROID) as AppExtension).apply {
            testVariants.toList().forEach { testVariant ->
                val variantApk: ApkSource = VariantApkSource(testVariant)
                devices.forEach { device ->
                    createTestLabTask(TestType.INSTRUMENTATION, device, testVariant, variantApk)
                    createTestLabTask(TestType.ROBO, device, testVariant, variantApk)
                }
                createGroupedTestLabTask(TestType.INSTRUMENTATION, devices, testVariant, variantApk)
                createGroupedTestLabTask(TestType.ROBO, devices, testVariant, variantApk)
            }
        }
    }

    private fun createTestLabTask(testType: TestType,
                                  device: Device,
                                  variant: TestVariant,
                                  apkSource: ApkSource) {
        val variantName = variant.testedVariant?.name?.capitalize() ?: ""

        project.task("${variantName.toLowerCase()}${testType.toString().capitalizeIt()}${device.name.capitalize()}" + TASK_NAME, closureOf<Task> {
            group = Constants.FIREBASE_TEST_LAB
            description = "Run Android Tests in Firebase Test Lab"

            dependsOn(* when (testType) {
                TestType.INSTRUMENTATION -> arrayOf("assemble$variantName", "assemble${variant.name.capitalize()}")
                TestType.ROBO -> arrayOf("assemble$variantName")
            })
            doFirst { configDataValidation() }
            doLast {
                if (extension!!.clearDirectoryBeforeRun) {
                    resultDownloader.clearResultsDir()
                }
                val result = processCreator.callFirebaseTestLab(testType, device, apkSource)
                processResult(result, extension!!.ignoreFailures)
                resultDownloader.getResults()
            }
        })
    }

    private fun createGroupedTestLabTask(testType: TestType,
                                         devices: List<Device>,
                                         variant: TestVariant,
                                         apkSource: ApkSource) {
        val variantName = variant.testedVariant?.name?.capitalize() ?: ""

        project.task("${variantName.toLowerCase()}${testType.toString().capitalizeIt()}" + TASK_NAME_GROUPED, closureOf<Task> {
            group = Constants.FIREBASE_TEST_LAB
            description = "Run Android Tests in Firebase Test Lab"

            dependsOn(* when (testType) {
                TestType.INSTRUMENTATION -> arrayOf("assemble$variantName", "assemble${variant.name.capitalize()}")
                TestType.ROBO -> arrayOf("assemble$variantName")
            })
            doFirst { configDataValidation() }
            doLast {
                devices.forEach { device ->
                    val result = processCreator.callFirebaseTestLab(testType, device, apkSource)
                    processResult(result, extension!!.ignoreFailures)
                }
                resultDownloader.getResults()
            }
        })
    }

    private fun configDataValidation() {
        if (!File(Constants.GCLOUD, extension!!.cloudSdkPath).exists() || !File(Constants.GSUTIL, extension!!.cloudSdkPath).exists()) {
            project.logger.warn(Constants.CLOUD_SDK_NOT_FOUND_OR_REMOTE)
        }
        if (extension!!.cloudBucketName.isNullOrEmpty()) {
            throw GradleException(Constants.BUCKET_NAME_INVALID)
        }
        if (extension!!.cloudDirectoryName.isEmpty()) {
            throw GradleException(Constants.RESULTS_TEST_DIR_NOT_VALID)
        }
        if (extension!!.devices.toList().isEmpty()) {
            throw GradleException(Constants.PLATFORM_NOT_SPECIFIED)
        }
        if (extension!!.ignoreFailures) {
            project.logger.warn(Constants.IGNORE_FAUILURE_ENABLED)
        }
    }

    private fun processResult(result: TestResults, ignoreFailures: Boolean) {
        if (result.isSuccessful) {
            project.logger.lifecycle(result.message)
        } else {
            if (ignoreFailures) {
                project.logger.error(Constants.ERROR + result.message)
            } else {
                throw GradleException(result.message)
            }
        }
    }
}