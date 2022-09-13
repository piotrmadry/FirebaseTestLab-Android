package com.appunite.firebasetestlabplugin.tasks

import org.gradle.api.GradleException
import org.gradle.api.tasks.Exec
import java.io.ByteArrayOutputStream

/**
 * Executes a command line process.
 * In case of failure throws GradleException.
 */

private const val SUCCESSFUL_EXEC_RESULT = 0
private const val EXEC_RESULT_ERROR_MESSAGE = "Execution result failed. See output above."

abstract class HiddenExec : Exec() {
    init {
        standardOutput = ByteArrayOutputStream()
        errorOutput = standardOutput
        isIgnoreExitValue = true

        doLast {
            executionResult.orNull?.let {
                if (it.exitValue != SUCCESSFUL_EXEC_RESULT) {
                    println(standardOutput.toString())
                    throw GradleException(EXEC_RESULT_ERROR_MESSAGE)
                }
            }
        }
    }
}