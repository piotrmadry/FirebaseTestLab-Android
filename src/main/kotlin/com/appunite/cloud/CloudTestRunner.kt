package com.appunite.cloud

import com.appunite.model.*
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
        val process = processCreator.start()

        val resultsDir = "$cloudBucketName/$resultsTestDir"

        process.errorStream.bufferedReader().forEachLine { logger.lifecycle(it) }
        process.inputStream.bufferedReader().forEachLine { logger.lifecycle(it) }

        val resultCode = process.waitFor()
        return TestResults(
                isSuccessful = resultCode == RESULT_SUCCESSFUL,
                resultDir = resultsDir,
                message = resultMessages[resultCode] ?: ""
        )
    }
}