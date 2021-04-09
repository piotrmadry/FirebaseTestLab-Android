package com.appunite.firebasetestlabplugin.tasks

import com.appunite.firebasetestlabplugin.cloud.ProcessData
import com.appunite.firebasetestlabplugin.tasks.sharding.RunWithSharding
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.workers.WorkerExecutor
import java.io.File
import javax.inject.Inject

open class InstrumentationShardingTask @Inject constructor() : DefaultTask() {
    
    @Inject
    lateinit var  workerExecutor: WorkerExecutor
    
    @Input
    lateinit var processData: ProcessData
    
    @OutputFile
    lateinit var stateFile: File
    
    @TaskAction
    fun runAction() {
        val workerQueue = workerExecutor.noIsolation()
        (0 until processData.device.numShards).map { shardIndex ->
            workerQueue.submit(RunWithSharding::class.java) {
                this.data = processData.copy(shardIndex = shardIndex)
                this.resultFile = stateFile
            }
        }
    }
}