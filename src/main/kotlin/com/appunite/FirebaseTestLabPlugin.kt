package com.appunite

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.script.lang.kotlin.closureOf

class FirebaseTestLabPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        target.afterEvaluate {
            target.task("testTask", closureOf<Task> {
                group = "test task group"
                description = "Test description"
            })
        }
    }

}