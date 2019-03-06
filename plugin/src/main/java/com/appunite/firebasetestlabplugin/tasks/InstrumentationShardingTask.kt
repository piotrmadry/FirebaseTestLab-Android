package com.appunite.firebasetestlabplugin.tasks

import com.appunite.firebasetestlabplugin.cloud.ProcessData
import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.workers.WorkerConfiguration
import org.gradle.workers.WorkerExecutor
import java.io.File
import javax.inject.Inject

open class InstrumentationShardingTask @Inject constructor(private val workerExecutor: WorkerExecutor) : DefaultTask() {
    
    lateinit var processData: ProcessData
    lateinit var stateFile: File
    
    @TaskAction
    fun runAction() {
        (0 until processData.device.numShards).map { shardIndex ->
            workerExecutor.submit(FirebaseTestLabProcess::class.java, object : Action<WorkerConfiguration> {
                override fun execute(config: WorkerConfiguration) {
                    config.params(processData.copy(shardIndex = shardIndex), stateFile)
                }
            })
        }
    }
}