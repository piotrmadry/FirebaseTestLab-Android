package com.appunite.firebasetestlabplugin.utils

import com.android.build.gradle.AppExtension
import org.gradle.api.Project
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.kotlin.dsl.configure
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test
import java.io.File

class StorageTest {
    
    private fun prepareSimpleProject(): Project {
        val simpleProject = File(javaClass.getResource("simple").file)
        val project = ProjectBuilder.builder().withProjectDir(simpleProject).build()
        project.plugins.apply("com.android.application")
        
        project.configure<AppExtension> {
            compileSdkVersion(27)
            defaultConfig.apply {
                versionCode = 1
                versionName = "0.1"
                setMinSdkVersion(27)
                setTargetSdkVersion(27)
            }
        }
        return project
    }
    
    @Test
    fun `read from empty storage returns null`() {
        val project = prepareSimpleProject() as ProjectInternal
        val xx = project.evaluate()
        assert(xx != null)
//        assert(Storage(project.buildDir).read("test") == null)
    }
}