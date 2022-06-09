package com.appunite.firebasetestlabplugin.model

import java.io.Serializable

class Device(val name: String) : Serializable {
    var locales: List<String> = listOf("en")
    var screenOrientations: List<String> = listOf("portrait")
    var androidApiLevels: List<Int> = listOf()
    var deviceIds: List<String> = listOf()
    var timeout: Long = 0

    var testUniversalApk: Boolean = true

    var filterAbiSplits = false
    var abiSplits: Set<String> = setOf()
    var isUseOrchestrator = false
    var numShards = 0
    
    var environmentVariables: List<String> = listOf()
    var customParamsForGCloudTool: List<String> = listOf()
    var testTargets: List<String> = listOf()
    var testRunnerClass: String? = null
}







