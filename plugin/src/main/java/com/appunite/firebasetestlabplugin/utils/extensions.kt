package com.appunite.firebasetestlabplugin.utils

import com.appunite.firebasetestlabplugin.FirebaseTestLabPluginExtension
import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.util.GradleVersion

fun FirebaseTestLabPluginExtension?.get(): FirebaseTestLabPluginExtension =
    this ?: throw GradleException("Plugin configured incorrectly. Please check documentation.")

val IS_GRADLE_MIN_49: Boolean = GradleVersion.current() >= GradleVersion.version("4.9-rc-1")

inline fun <reified T : Task> createTask(
    project: Project,
    name: String,
    configuration: Action<in T>
) {
    if (IS_GRADLE_MIN_49) {
        project.tasks.register(name, T::class.java, configuration)
    } else {
        project.tasks.create(name, T::class.java, configuration)
    }
}

fun Task.getTaskByName(name: String): Task = project.tasks.getByName(name)

fun Task.getOutputFileByFileName(fileName: String): String {
    return outputs.files.filter { outputTask -> outputTask.name == fileName }.asPath
}


