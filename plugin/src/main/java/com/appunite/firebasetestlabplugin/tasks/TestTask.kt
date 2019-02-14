package com.appunite.firebasetestlabplugin.tasks

import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.workers.WorkerConfiguration
import org.gradle.workers.WorkerExecutor
import java.io.File
import javax.inject.Inject

open class TestTask @Inject constructor(private val workerExecutor: WorkerExecutor) : DefaultTask() {
    lateinit var stateFile: File
    
    @TaskAction
    fun runAction() {
        (0 until 4).map { shardIndex ->
            workerExecutor.submit(TestProcess::class.java, object : Action<WorkerConfiguration> {
                override fun execute(config: WorkerConfiguration) {
                    config.params(stateFile, shardIndex)
                }
            })
        }
    }
}