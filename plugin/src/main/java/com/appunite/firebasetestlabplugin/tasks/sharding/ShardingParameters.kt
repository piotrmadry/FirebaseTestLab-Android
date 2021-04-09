package com.appunite.firebasetestlabplugin.tasks.sharding

import com.appunite.firebasetestlabplugin.cloud.ProcessData
import org.gradle.workers.WorkParameters
import java.io.File

interface ShardingParameters : WorkParameters {
    var data: ProcessData
    var resultFile: File
}