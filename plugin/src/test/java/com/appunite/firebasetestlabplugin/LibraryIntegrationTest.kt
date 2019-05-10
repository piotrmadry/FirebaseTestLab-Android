package com.appunite.firebasetestlabplugin

import com.android.build.gradle.LibraryExtension
import junit.framework.Assert.assertTrue
import org.gradle.api.Project
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.kotlin.dsl.configure
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test
import java.io.File

class LibraryIntegrationTest {

    fun prepareSimpleProject(): Project {
        val simpleProject = File(javaClass.getResource("simple").file)
        val project = ProjectBuilder.builder().withProjectDir(simpleProject).build()
        project.plugins.apply("com.android.library")
        project.configure<LibraryExtension> {
            compileSdkVersion(27)
            defaultConfig.apply {
                versionCode = 1
                versionName = "0.1"
                setMinSdkVersion(27)
                setTargetSdkVersion(27)
            }
        }
        project.plugins.apply("firebase.test.lab")
        project.configure<FirebaseTestLabPluginExtension> {
            googleProjectId = "test"
            keyFile = File(simpleProject, "key.json")
        }

        return project
    }

    @Test
    fun `check basic tasks`() {
        val project = prepareSimpleProject()
        (project as ProjectInternal).evaluate()

        assertTrue(project.getTasksByName("firebaseTestLabEnsureGCloudSdk", false).isNotEmpty())
        assertTrue(project.getTasksByName("firebaseTestLabSetProject", false).isNotEmpty())
        assertTrue(project.getTasksByName("firebaseTestLabAuth", false).isNotEmpty())
        assertTrue(project.getTasksByName("firebaseTestLabSetup", false).isNotEmpty())
    }
}