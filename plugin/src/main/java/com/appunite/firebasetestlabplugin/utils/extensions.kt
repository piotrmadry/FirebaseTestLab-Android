package com.appunite.firebasetestlabplugin.utils

import com.appunite.firebasetestlabplugin.FirebaseTestLabPluginExtension
import org.gradle.api.GradleException

fun FirebaseTestLabPluginExtension?.get(): FirebaseTestLabPluginExtension =
    this ?: throw GradleException("Plugin configured incorrectly. Please check documentation.")
