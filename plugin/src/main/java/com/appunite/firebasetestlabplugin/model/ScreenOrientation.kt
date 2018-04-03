package com.appunite.firebasetestlabplugin.model

enum class ScreenOrientation {
    PORTRAIT {
        override val gcloudName: String = "portrait"
    },
    LANDSCAPE {
        override val gcloudName: String = "landscape"
    };

    internal abstract val gcloudName: String
}
