package com.appunite.firebasetestlabplugin.tasks

import com.appunite.firebasetestlabplugin.cloud.FirebaseTestLabProcessCreator
import com.appunite.firebasetestlabplugin.cloud.ProcessData
import org.gradle.api.GradleException
import java.io.File
import javax.inject.Inject

class FirebaseTestLabProcess @Inject constructor(
    private val processData: ProcessData,
    private val stateFile: File
) : Runnable {
    override fun run() {
           try {
               val testResults = FirebaseTestLabProcessCreator.callFirebaseTestLab(processData)
               stateFile.appendText(text = if (testResults.isSuccessful) "0" else "1")
           } catch (e: Exception){
               throw GradleException("There was a problem with processing ${e.message}")
           }
    }
}