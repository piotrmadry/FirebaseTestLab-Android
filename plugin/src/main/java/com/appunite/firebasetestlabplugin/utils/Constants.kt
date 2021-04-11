package com.appunite.firebasetestlabplugin.utils

import com.appunite.firebasetestlabplugin.FirebaseTestLabPlugin

class Constants {
    companion object {
        
        const val GCLOUD = "gcloud"
        const val GSUTIL = "gsutil"
        const val FIREBASE_TEST_LAB = "Firebase Test Lab"
        const val GRADLE_METHOD_NAME = "firebaseTestLab"
        const val ANDROID = "android"
        const val INSTALL_GOOGLE_CLOUD_SDK = "firebaseTestLabInstallGCloudSdk"
        const val ENSURE_GOOGLE_CLOUD_SDK_INSTALLED = "firebaseTestLabEnsureGCloudSdk"
        const val VERIFY_GOOGLE_CLOUD_SDK_INSTALLATION_LOCATION = "firebaseTestLabVerifyGCloudSdkLocation"
        const val VERIFY_GOOGLE_CLOUD_SDK_INSTALLATION_OS_SUPPORTED = "firebaseTestLabVerifyOSSupportedGCloudSdkInstallation"
        const val taskAuth = "firebaseTestLabAuth"
        const val taskSetup = "firebaseTestLabSetup"
        const val taskSetProject = "firebaseTestLabSetProject"
        const val taskPrefixDownload = "firebaseTestLabDownload"
        const val taskPrefixExecute = "firebaseTestLabExecute"
        const val BLANK_APK_RESOURCE_PATH = "/blank.apk"
    
        val BLANK_APK_RESOURCE = FirebaseTestLabPlugin::class.java.getResource(BLANK_APK_RESOURCE_PATH)
    }
}