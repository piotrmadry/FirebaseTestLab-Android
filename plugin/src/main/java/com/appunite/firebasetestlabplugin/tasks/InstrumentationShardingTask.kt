package com.appunite.firebasetestlabplugin.tasks

import com.appunite.firebasetestlabplugin.cloud.ProcessData
import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.workers.WorkerConfiguration
import org.gradle.workers.WorkerExecutor
import java.io.File
import javax.inject.Inject

open class InstrumentationShardingTask @Inject constructor(private val workerExecutor: WorkerExecutor) : DefaultTask() {
    
    @Input
    lateinit var processData: ProcessData
    @OutputFile
    lateinit var stateFile: File
    
    @TaskAction
    fun runAction() {
        (0 until processData.device.numShards).map { shardIndex ->
            workerExecutor.submit(FirebaseTestLabProcess::class.java) {
                this.params(processData.copy(shardIndex = shardIndex), stateFile)
            }
        }
    }
}