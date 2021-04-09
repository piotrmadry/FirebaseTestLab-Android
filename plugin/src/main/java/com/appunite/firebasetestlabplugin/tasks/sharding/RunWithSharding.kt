package com.appunite.firebasetestlabplugin.tasks.sharding

import com.appunite.firebasetestlabplugin.cloud.FirebaseTestLabProcessCreator
import org.gradle.api.GradleException
import org.gradle.workers.WorkAction

abstract class RunWithSharding : WorkAction<ShardingParameters>{
    
    override fun execute() {
        val processData = parameters.data
        val resultFile = parameters.resultFile
    
        try {
            val testResults = FirebaseTestLabProcessCreator.callFirebaseTestLab(processData)
            resultFile.appendText(text = if (testResults.isSuccessful) "0" else "1")
        } catch (e: Exception){
            throw GradleException("There was a problem with processing ${e.message}")
        }
    }
}