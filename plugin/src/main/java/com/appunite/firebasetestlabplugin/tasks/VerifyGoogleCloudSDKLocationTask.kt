package com.appunite.firebasetestlabplugin.tasks

import com.appunite.firebasetestlabplugin.utils.Constants
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.io.File

class VerifyGoogleCloudSDKLocationTask(private val googleCloudSDKLocationPath: String?) : DefaultTask(){
    
    init {
        group = Constants.FIREBASE_TEST_LAB
        description = "Verify provided Google Cloud SDK location."
    }
    
    @TaskAction
    fun run(){
        val gcloud = File(googleCloudSDKLocationPath, Constants.GCLOUD)
        val gsutil = File(googleCloudSDKLocationPath, Constants.GSUTIL)
        
        if (!gcloud.exists()) {
            throw IllegalStateException("GCloud doesn't exist in the path: ${gcloud.absolutePath}. Please set correct path to property \"cloudSdkPath\"")
        }
        if (!gsutil.exists()) {
            throw IllegalStateException("GCloud doesn't exist in the path: ${gsutil.absolutePath}. Please set correct path to property \"cloudSdkPath\"")
        }
        
        outputs.files(gcloud, gsutil)
    }
}