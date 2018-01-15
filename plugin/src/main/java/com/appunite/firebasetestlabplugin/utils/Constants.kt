package com.appunite.firebasetestlabplugin.utils

class Constants {

    companion object {
        const val GCLOUD = "gcloud"
        const val GSUTIL = "gsutil"
        const val DOWNLOAD_PHASE = "DOWNLOAD: "
        const val CLOUD_SDK_NOT_FOUND_OR_REMOTE = "Cloud SDK path is remote or not valid"
        const val BUCKET_NAME_INVALID = "Please add valid google cloud bucket name!"
        const val FIREBASE_TEST_LAB = "Firebase Test Lab"
        const val WARN_CORRECT_RESULT_PATH = "Take care of correct path!"
        const val ERROR = "Error: "
        const val RESULTS_TEST_DIR_NOT_VALID = "You need to specify dir name for your tests"
        const val PLATFORM_NOT_SPECIFIED = "You need to specify at least one platform to run tests"
        const val IGNORE_FAUILURE_ENABLED = "Ignore failures is enabled. Failures will not break tests processing"
        const val RESULT_SUCCESSFUL = 0
    }
}