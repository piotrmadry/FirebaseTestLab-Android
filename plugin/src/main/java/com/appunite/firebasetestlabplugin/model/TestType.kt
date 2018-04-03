package com.appunite.firebasetestlabplugin.model

enum class TestType {
    INSTRUMENTATION {
        override val gcloudName: String = "instrumentation"
    },
    ROBO {
        override val gcloudName: String = "robo"
    };
    internal abstract val gcloudName: String
}
