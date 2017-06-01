package com.appunite

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.script.lang.kotlin.closureOf
import java.io.File

class FirebaseTestLabPlugin : Plugin<Project> {

    private val GRADLE_METHOD_NAME = "FirebaseTestLabConfig"

    private lateinit var project: Project
    private lateinit var config: FirebaseTestLabPluginExtension

    override fun apply(project: Project) {
        this.project = project

        project.extensions.create(
                GRADLE_METHOD_NAME,
                FirebaseTestLabPluginExtension::class.java,
                project)

        project.afterEvaluate {
            initConfig()
            createTasks()
        }
    }

    private fun initConfig(){
        config = project.extensions.findByType(FirebaseTestLabPluginExtension::class.java).apply {
            if (!File(cloudSdkPath, "gcloud").canExecute()){
                println("Cloud SDK Path not good")
            }
            if (cloudBucketName.isNullOrBlank()){
                println("Bucket name is not good")
            }

            println("Paths: " + artifacts.getArtifactPaths())
        }
    }
    private fun createTasks() {
        project.task("SimpleTask", closureOf<Task> {
            group = "Test Group"
            description = "Test Description"
        })
    }
}