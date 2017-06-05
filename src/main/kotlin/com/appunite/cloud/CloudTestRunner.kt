package com.appunite.cloud

import com.appunite.extensions.*
import com.appunite.utils.ApkSource
import com.appunite.utils.asCommand
import com.appunite.utils.command
import com.appunite.utils.joinArgs
import org.gradle.api.logging.Logger
import java.io.File

internal interface FirebaseTestLabRunner {
    fun run(): TestResults
}

internal class CloudTestRunner(val cloudBucketName: String,
                               val resultsTestDir: String,
                               val logger: Logger,
                               testType: TestType,
                               cloudSdkPath: File,
                               platform: Platform,
                               apks: ApkSource) : FirebaseTestLabRunner {

    /**
     * Declare process builder that contain terminal command to run tests
     */
    private val processCreator = ProcessBuilder("""
        ${command("gcloud", cloudSdkPath)}
                firebase test android run
                --format json
                --results-bucket $cloudBucketName
                --results-dir $resultsTestDir
                --type $testType
                --locales ${platform.locales.joinArgs()},
                --os-version-ids ${platform.androidApiLevels.joinArgs()}
                --orientations ${platform.orientations.joinArgs()}
                --device-ids ${platform.deviceIds.joinArgs()}
                --app ${apks.apk}
                ${if (testType == TestType.instrumentation) "--test ${apks.testApk}" else ""}
                ${if (platform.timeoutSec > 0) "--timeoutSec ${platform.timeoutSec}s" else ""}
    """.asCommand())

    override fun run(): TestResults {
        val process = processCreator.start() // Run command
        logger.debug(processCreator.command().joinArgs()) // Print whole command

        var resultDir: String? = null

        process.errorStream.bufferedReader().forEachLine {
            logger.lifecycle(it)
//            if (it.contains(cloudBucketName)) {
//                resultDir = "$cloudBucketName\\/(.*)\\/".toRegex().find(it)?.groups?.get(1)?.value
//                if (resultDir == null) {
//                    logger.error(Constants.ERROR + "Cannot achieve result dir name. Results will not be downloaded.")
//                } else {
//                    logger.lifecycle("Target result dir name is $resultDir")
//                }
//            }
        }

        process.inputStream.bufferedReader().forEachLine { logger.lifecycle(it) }

        val resultCode = process.waitFor()
        return TestResults(
                isSuccessful = resultCode == RESULT_SUCCESSFUL,
                resultDir = resultDir,
                message = resultMessages[resultCode] ?: ""
        )
    }
}