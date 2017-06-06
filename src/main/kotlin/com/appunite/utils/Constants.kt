package com.appunite.utils

class Constants {
    companion object {
        val GCLOUD = "gcloud"
        val GSUTIL = "gsutil"
        val DOWNLOAD_PHASE = "DOWNLOAD PHASE: "
        val CLOUD_SDK_NOT_FOUND_OR_REMOTE = "Cloud SDK path is remote or not valid"
        val BUCKET_NAME_INVALID = "Please add valid google cloud bucket name!"
        val ARTIFACTS_NOT_CONFIGURED = "Your artifacts are not configured. Results will not be downloaded"
        val FIREBASE_TEST_LAB = "Firebase Test Lab"
        val ERROR = "Error: "
        val RESULTS_TEST_DIR_NOT_VALID = "You need to specify dir name for your tests"
        val PLATFORM_NOT_SPECIFIED = "You need to specify at least one platform to run tests"
        val IGNORE_FAUILURE_ENABLED = "Ignore failures is enabled. Failures will not break tests processing"

        val DOWNLOADING_ARTIFACTS_STARTED = "Artifact download started"
    }
}