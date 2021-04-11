package com.appunite.firebasetestlabplugin.model

import java.io.File
import java.io.Serializable

data class GoogleCloudSDK(val gcloud: File, val gsutil: File) : Serializable