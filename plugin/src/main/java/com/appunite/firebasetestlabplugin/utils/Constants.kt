package com.appunite.firebasetestlabplugin.utils

import com.appunite.firebasetestlabplugin.FirebaseTestLabPlugin

class Constants {
    companion object {
        const val GCLOUD = "gcloud"
        const val GSUTIL = "gsutil"
        const val FIREBASE_TEST_LAB = "Firebase Test Lab"
        const val GRADLE_METHOD_NAME = "firebaseTestLab"
        const val ANDROID = "android"
        const val ensureGCloudSdk = "firebaseTestLabEnsureGCloudSdk"
        const val taskAuth = "firebaseTestLabAuth"
        const val taskSetup = "firebaseTestLabSetup"
        const val taskSetProject = "firebaseTestLabSetProject"
        const val taskPrefixDownload = "firebaseTestLabDownload"
        const val taskPrefixExecute = "firebaseTestLabExecute"
        const val BLANK_APK_RESOURCE_PATH = "/blank.apk"
    
        val BLANK_APK_RESOURCE = FirebaseTestLabPlugin::class.java.getResource(BLANK_APK_RESOURCE_PATH)
    }
}