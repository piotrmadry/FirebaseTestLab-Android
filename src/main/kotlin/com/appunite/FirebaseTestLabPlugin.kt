@file:Suppress("RemoveCurlyBracesFromTemplate")

package com.appunite

import com.android.build.gradle.AppExtension
import com.android.build.gradle.api.TestVariant
import com.appunite.cloud.CloudTestResultDownloader
import com.appunite.cloud.CloudTestRunner
import com.appunite.model.ArtifactType
import com.appunite.model.Platform
import com.appunite.model.TestResults
import com.appunite.model.TestType
import com.appunite.utils.ApkSource
import com.appunite.utils.Constants
import com.appunite.utils.VariantApkSource
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.script.lang.kotlin.closureOf
import java.io.File

class FirebaseTestLabPlugin : Plugin<Project> {

    private val GRADLE_METHOD_NAME = "firebaseTestLab"
    private val ANDROID = "android"
    private val RESULT_PATH = "ui-tests"
    private val TASK_NAME = "uploadTestLab"

    private lateinit var project: Project
    private lateinit var config: FirebaseTestLabPluginExtension
    private lateinit var downloader: CloudTestResultDownloader

    private var artifactsToExcludeMap: Map<ArtifactType, Boolean> = hashMapOf()

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
            createTaskForAndroid()
            testingTask()
        }
    }

    private fun setup() {
        config = project.extensions.findByType(FirebaseTestLabPluginExtension::class.java).apply {
            artifactsToExcludeMap = this.artifacts.getArtifactsMap().filterValues { it == false }
        }
        downloader = CloudTestResultDownloader(artifactsToExcludeMap, File(config.resultsDestinationPath, RESULT_PATH),
                File(config.cloudSdkPath), config.cloudBucketName, config.resultsTestDir, project.logger)
    }

    private fun createTaskForAndroid() {
        val platforms = config.platforms.toList()
        (project.extensions.findByName(ANDROID) as AppExtension).apply {
            val debugVariant = testVariants.toList()[0]
            val variantApk = VariantApkSource(debugVariant)
            createTestLabTask(TestType.instrumentation, platforms, debugVariant, variantApk)
        }
    }

    private fun createTestLabTask(testType: TestType,
                                  platforms: List<Platform>,
                                  variant: TestVariant,
                                  apkSource: ApkSource) {
        val variantName = variant.testedVariant?.name?.capitalize() ?: ""

        project.task(TASK_NAME, closureOf<Task> {
            group = Constants.FIREBASE_TEST_LAB
            description = "Run Instrumental tests in Firebase Test Lab"

            dependsOn(* when (testType) {
                TestType.instrumentation -> arrayOf("assemble${variantName}", "assemble${variant.name.capitalize()}")
                TestType.robo -> arrayOf("assemble${variantName}")
            })
            doFirst { configDataValidation() }
            doLast {
                downloader.clearBucket(config.cloudSdkPath, config.cloudBucketName)
                platforms.forEach { platform ->
                    val result = runTestLabTest(testType, platform, apkSource)
                    processResult(result, config.ignoreFailures)
                }
                downloader.fetchArtifacts()
            }
        })
    }

    private fun configDataValidation() {
        if (!File(Constants.GCLOUD, config.cloudSdkPath).exists() || !File(Constants.GSUTIL, config.cloudSdkPath).exists()) {
            project.logger.warn(Constants.CLOUD_SDK_NOT_FOUND_OR_REMOTE)
        }
        if (config.cloudBucketName.isNullOrEmpty()) {
            throw GradleException(Constants.BUCKET_NAME_INVALID)
        }
        if (config.resultsTestDir.isNullOrEmpty()) {
            throw GradleException(Constants.RESULTS_TEST_DIR_NOT_VALID)
        }
        if (config.platforms.toList().isEmpty()) {
            throw GradleException(Constants.PLATFORM_NOT_SPECIFIED)
        }
        if (config.artifacts.getArtifactsMap().filterValues { it }.isEmpty()) {
            project.logger.warn(Constants.ARTIFACTS_NOT_CONFIGURED)
        }
        if (config.ignoreFailures) {
            project.logger.warn(Constants.IGNORE_FAUILURE_ENABLED)
        }
        if (config.resultsDestinationPath != project.buildDir.toString()){
            project.logger.warn("Please be sure that this path is correctly")
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

    private fun runTestLabTest(
            testType: TestType,
            platform: Platform,
            apkSource: ApkSource): TestResults {
        return CloudTestRunner(config.cloudBucketName, config.resultsTestDir, project.logger,
                testType, File(config.cloudSdkPath), platform, apkSource).run()
    }

    private fun testingTask() {
        project.task("Logger", closureOf<Task> {
            group = "Debug"
            description = "Print config info"

            doLast {
                logger.lifecycle("Number PLATFORMS: " + config.platforms.size)
                config.platforms.forEach { platform ->
                    logger.lifecycle("Name: " + platform.name)
                }
                logger.lifecycle("Filtered ARTIFACTS to exclude: " + artifactsToExcludeMap.size)
                val startQuery = "-x \".*\\.txt$|.*\\.apk$"
                val endQuery = "|.*\\.txt$\""
                val excludeQuery = StringBuilder().append(startQuery)
                artifactsToExcludeMap.keys.forEach { key ->
                    when (key) {
                        ArtifactType.VIDEO -> excludeQuery.append("|.*\\.mp4$")
                        ArtifactType.XML -> excludeQuery.append("|.*\\.results$")
                        ArtifactType.LOGCAT -> excludeQuery.append("|.*\\logcat$")
                        ArtifactType.JUNIT -> excludeQuery.append("|.*\\.xml$")
                    }
                }
                excludeQuery.append(endQuery).toString()
                logger.lifecycle("EXCLUDE BUILDER: " + excludeQuery)
                val excludeFiles = "-x \".*\\.txt$|.*\\.mp4$|.*\\.apk$|.*\\.results$|.*\\logcat$|.*\\.txt$\""
                logger.lifecycle("EXLUDE EXAMPLE: " + excludeFiles)

            }
        })
    }
}