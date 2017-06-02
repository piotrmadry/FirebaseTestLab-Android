package com.appunite.extensions

const val RESULT_SUCCESSFUL = 0

val resultMessages = mapOf(
        0 to "All test executions passed.",
        1 to "A general failure occurred. Possible causes include: a filename that does not exist, or an HTTP/network error.",
        2 to "Testing exited because unknown commands or arguments were provided.",
        10 to "One or more test cases (tested classes or class methods) within a test execution did not pass.",
        15 to "Firebase Test Lab for Android could not determine if the test matrix passed or failed because of an unexpected error.",
        18 to "The test environment for this test execution is not supported because of incompatible test dimensions. This error might occur if the selected Android API level is not supported by the selected device type.",
        19 to "The test matrix was canceled by the user.",
        20 to "A test infrastructure error occurred."
)
data class TestResults(
        val isSuccessful: Boolean,
        val resultDir: String?,
        val message: String
)

enum class TestType {
    instrumentation,
    robo
}
