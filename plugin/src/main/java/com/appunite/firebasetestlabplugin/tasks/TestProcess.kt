package com.appunite.firebasetestlabplugin.tasks

import org.gradle.api.GradleException
import java.io.File
import javax.inject.Inject

class TestProcess @Inject constructor(
    private val stateFile: File,
    private val value: Int
) : Runnable {
    override fun run() {
           try {
               stateFile.appendText(text = "0")
           } catch (e: Exception){
               throw GradleException("There was a problem with processing ${e.message} $value")
           }
    }
}