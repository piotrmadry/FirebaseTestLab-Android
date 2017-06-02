@file:Suppress("RemoveCurlyBracesFromTemplate")

package com.appunite

import com.android.build.gradle.AppExtension
import com.android.build.gradle.api.TestVariant
import com.appunite.cloud.CloudTestResultDownloader
import com.appunite.cloud.CloudTestRunner
import com.appunite.extensions.Platform
import com.appunite.extensions.TestResults
import com.appunite.extensions.TestType
import com.appunite.utils.ApkSource
import com.appunite.utils.BuildParameterApkSource
import com.appunite.utils.VariantApkSource
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.script.lang.kotlin.closureOf
import java.io.File

class FirebaseTestLabPlugin : Plugin<Project> {

    private val GRADLE_METHOD_NAME = "FirebaseTestLabConfig"
    private val ANDROID = "android"
    private val RESULT_PATH = "reports/firebase-test-lab"

    private lateinit var project: Project
    private lateinit var config: FirebaseTestLabPluginExtension

    override fun apply(project: Project) {
        this.project = project

        project.extensions.create(
                GRADLE_METHOD_NAME,
                FirebaseTestLabPluginExtension::class.java,
                project)

        project.afterEvaluate {
            initConfig()
            createTasks()
        }
    }

    private fun initConfig() {
        config = project.extensions.findByType(FirebaseTestLabPluginExtension::class.java).apply {
            if (!File(cloudSdkPath, "gcloud").canExecute()) {
                println("Cloud SDK Path not good")
            }
            if (cloudBucketName.isNullOrBlank()) {
                println("Bucket name is not good")
            }

            println("Paths: " + artifacts.getArtifactPaths())
        }
    }

    private fun createTasks() {
        val platforms = config.platforms.toList()

        (project.extensions.findByName(ANDROID) as AppExtension).apply {
            testVariants.forEach { variant ->
                val apks = VariantApkSource(variant)

                platforms.forEach { platform ->
                    createTask(TestType.instrumentation, platform, variant, apks)
                    createTask(TestType.robo, platform, variant, apks)
                }
            }
        }

        val apks = BuildParameterApkSource(project)
        if (config.enableVariantLessTasks) {
            platforms.forEach { platform ->
                createTask(TestType.instrumentation, platform, null, apks)
                createTask(TestType.robo, platform, null, apks)
            }
        }
    }

    private fun createTask(
            type: TestType,
            platform: Platform,
            variant: TestVariant?,
            apks: ApkSource
    ) {
        val variantName = variant?.testedVariant?.name?.capitalize() ?: ""
        project.logger.debug("Creating Firebase task for $variantName")
        project.task("test${variantName}${platform.name.capitalize()}${type.toString().capitalize()}TestLab", closureOf<Task> {
            group = "Test Lab"
            description = "Run ${type} tests " +
                    (if (variant == null) "" else "for the ${variantName} build ") +
                    "in Firebase Test Lab."
            if (variant == null) {
                description += "\nTo run test for your matrix without build project" +
                        " you must specify paths to apk and test apk using parameters -Papk and -PtestApk"
            }
            //Add dependencies on assemble tasks of application and tests
            //But only for "variant" builds,
            if (variant != null) {
                dependsOn(*when (type) {
                    TestType.instrumentation -> arrayOf("assemble${variantName}", "assemble${variant.name.capitalize()}")
                    TestType.robo -> arrayOf("assemble${variantName}")
                })
            }
            doLast {
                val result = runTestLabTest(type, platform, apks)
                processResult(result, config.ignoreFailures)
                downloadArtifacts(result)
            }
        })
    }

    private fun processResult(result: TestResults, ignoreFailures: Boolean) {
        if (result.isSuccessful) {
            project.logger.lifecycle(result.message)
        } else {
            if (ignoreFailures) {
                project.logger.error(result.message)
            } else {
                throw GradleException(result.message)
            }
        }
    }

    private fun downloadArtifacts(result: TestResults) {
        project.logger.lifecycle("Artifact downloading started")
        CloudTestResultDownloader(
                config.artifacts.getArtifactPaths(),
                File(project.buildDir, RESULT_PATH),
                File(config.cloudSdkPath),
                config.cloudBucketName,
                result,
                project.logger)
    }

    private fun runTestLabTest(
            testType: TestType,
            platform: Platform,
            apks: ApkSource
    ): TestResults {
        return CloudTestRunner(
                config.cloudBucketName,
                project.logger,
                testType,
                File(config.cloudSdkPath),
                platform,
                apks).run()
    }
}