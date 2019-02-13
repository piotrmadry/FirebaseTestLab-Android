package com.appunite.firebasetestlabplugin

import com.android.build.gradle.AppExtension
import junit.framework.TestCase.assertTrue
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.kotlin.dsl.configure
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test
import java.io.File

class IntegrationTest {

    @Test
    fun `test evaluate simple project success`() {
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
        (project as ProjectInternal).evaluate()
    }

    @Test
    fun `test evaluate simple project with plugin success`() {
        val simpleProject = File(javaClass.getResource("simple").file)
        val project = ProjectBuilder.builder().withProjectDir(simpleProject).build()
        project.plugins.apply("com.android.application")
        project.plugins.apply("firebase.test.lab")
        project.configure<AppExtension> {
            compileSdkVersion(27)
        }
        (project as ProjectInternal).evaluate()

        assertTrue(project.getTasksByName("firebaseTestLabEnsureGCloudSdk", false).isNotEmpty())
        assertTrue(project.getTasksByName("firebaseTestLabSetProject", false).isNotEmpty())
        assertTrue(project.getTasksByName("firebaseTestLabAuth", false).isNotEmpty())
        assertTrue(project.getTasksByName("firebaseTestLabSetup", false).isNotEmpty())
    }

    @Test
    fun `run firebaseTestLabSetup install gcloud`() {
        val simpleProject = File(javaClass.getResource("simple").file)
        val project = ProjectBuilder.builder().withProjectDir(simpleProject).build()
        project.plugins.apply("com.android.application")
        project.plugins.apply("firebase.test.lab")
        project.configure<AppExtension> {
            compileSdkVersion(27)
            defaultConfig.also {
                it.versionCode = 1
                it.versionName = "0.1"
                it.setMinSdkVersion(27)
                it.setTargetSdkVersion(27)
            }
        }
        project.configure<FirebaseTestLabPluginExtension> {
            googleProjectId = "test"
            keyFile = File(simpleProject, "key.json")
        }
        (project as ProjectInternal).evaluate()
        executeTask(project, "firebaseTestLabSetProject")

        assertTrue(File(File(File(File(project.buildDir, "gcloud"), "google-cloud-sdk"), "bin"), "gcloud").exists())
    }

    @Test
    fun `ensure after evaluation tasks presented`() {
        val simpleProject = File(javaClass.getResource("simple").file)
        val project = ProjectBuilder.builder().withProjectDir(simpleProject).build()
        project.plugins.apply("com.android.application")
        project.plugins.apply("firebase.test.lab")
        project.configure<AppExtension> {
            compileSdkVersion(27)
            defaultConfig.also {
                it.versionCode = 1
                it.versionName = "0.1"
                it.setMinSdkVersion(27)
                it.setTargetSdkVersion(27)
            }
        }
        project.configure<FirebaseTestLabPluginExtension> {
            googleProjectId = "test"
            keyFile = File(simpleProject, "key.json")
            createDevice("myDevice") {
                deviceIds = listOf("Nexus6")
            }
        }
        (project as ProjectInternal).evaluate()


        assertTrue(project.getTasksByName("firebaseTestLabExecuteDebugInstrumentation", false).isNotEmpty())
        assertTrue(project.getTasksByName("firebaseTestLabExecuteDebugRobo", false).isNotEmpty())
    }

    @Test
    fun `ensure tasks are created for abi splits with universal apk`() {
        val simpleProject = File(javaClass.getResource("simple").file)
        val project = ProjectBuilder.builder().withProjectDir(simpleProject).build()
        project.plugins.apply("com.android.application")
        project.plugins.apply("firebase.test.lab")
        project.configure<AppExtension> {
            compileSdkVersion(27)
            defaultConfig.also {
                it.versionCode = 1
                it.versionName = "0.1"
                it.setMinSdkVersion(27)
                it.setTargetSdkVersion(27)
            }
            splits.also {
                it.abi.also {
                    it.isEnable = true
                    it.reset()
                    it.include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
                    it.isUniversalApk = true
                }
            }
        }
        project.configure<FirebaseTestLabPluginExtension> {
            googleProjectId = "test"
            keyFile = File(simpleProject, "key.json")
            createDevice("myDevice") {
                deviceIds = listOf("Nexus6")
            }
        }
        (project as ProjectInternal).evaluate()

        assertTrue(project.getTasksByName("firebaseTestLabExecuteDebugInstrumentationMyDeviceArm64V8aDebug", false).isNotEmpty())
        assertTrue(project.getTasksByName("firebaseTestLabExecuteDebugInstrumentationMyDeviceX86Debug", false).isNotEmpty())
        assertTrue(project.getTasksByName("firebaseTestLabExecuteDebugInstrumentationMyDeviceX8664Debug", false).isNotEmpty())
        assertTrue(project.getTasksByName("firebaseTestLabExecuteDebugInstrumentationMyDeviceArmeabiV7aDebug", false).isNotEmpty())
        assertTrue(project.getTasksByName("firebaseTestLabExecuteDebugInstrumentationMyDeviceUniversalDebug", false).isNotEmpty())
    }

    @Test
    fun `ensure tasks are created for abi splits without unversal apk`() {
        val simpleProject = File(javaClass.getResource("simple").file)
        val project = ProjectBuilder.builder().withProjectDir(simpleProject).build()
        project.plugins.apply("com.android.application")
        project.plugins.apply("firebase.test.lab")
        project.configure<AppExtension> {
            compileSdkVersion(27)
            defaultConfig.also {
                it.versionCode = 1
                it.versionName = "0.1"
                it.setMinSdkVersion(27)
                it.setTargetSdkVersion(27)
            }
            splits.also {
                it.abi.also {
                    it.isEnable = true
                    it.reset()
                    it.include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
                    it.isUniversalApk = false
                }
            }
        }
        project.configure<FirebaseTestLabPluginExtension> {
            googleProjectId = "test"
            keyFile = File(simpleProject, "key.json")
            createDevice("myDevice") {
                deviceIds = listOf("Nexus6")
            }
        }
        (project as ProjectInternal).evaluate()

        assertTrue(project.getTasksByName("firebaseTestLabExecuteDebugInstrumentationMyDeviceArm64V8aDebug", false).isNotEmpty())
        assertTrue(project.getTasksByName("firebaseTestLabExecuteDebugInstrumentationMyDeviceX86Debug", false).isNotEmpty())
        assertTrue(project.getTasksByName("firebaseTestLabExecuteDebugInstrumentationMyDeviceX8664Debug", false).isNotEmpty())
        assertTrue(project.getTasksByName("firebaseTestLabExecuteDebugInstrumentationMyDeviceArmeabiV7aDebug", false).isNotEmpty())
        assertTrue(project.getTasksByName("firebaseTestLabExecuteDebugInstrumentationMyDeviceUniversalDebug", false).isEmpty())
    }

    @Test
    fun `ensure tasks are created for abi splits with filter`() {
        val simpleProject = File(javaClass.getResource("simple").file)
        val project = ProjectBuilder.builder().withProjectDir(simpleProject).build()
        project.plugins.apply("com.android.application")
        project.plugins.apply("firebase.test.lab")
        project.configure<AppExtension> {
            compileSdkVersion(27)
            defaultConfig.also {
                it.versionCode = 1
                it.versionName = "0.1"
                it.setMinSdkVersion(27)
                it.setTargetSdkVersion(27)
            }
            splits.also {
                it.abi.also {
                    it.isEnable = true
                    it.reset()
                    it.include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
                    it.isUniversalApk = true
                }
            }
        }
        project.configure<FirebaseTestLabPluginExtension> {
            googleProjectId = "test"
            keyFile = File(simpleProject, "key.json")
            createDevice("myDevice") {
                deviceIds = listOf("Nexus6")
                filterAbiSplits = true
                abiSplits = setOf("armeabi-v7a")
                testUniversalApk = false
            }
        }
        (project as ProjectInternal).evaluate()

        assertTrue(project.getTasksByName("firebaseTestLabExecuteDebugInstrumentationMyDeviceArm64V8aDebug", false).isEmpty())
        assertTrue(project.getTasksByName("firebaseTestLabExecuteDebugInstrumentationMyDeviceX86Debug", false).isEmpty())
        assertTrue(project.getTasksByName("firebaseTestLabExecuteDebugInstrumentationMyDeviceX8664Debug", false).isEmpty())
        assertTrue(project.getTasksByName("firebaseTestLabExecuteDebugInstrumentationMyDeviceArmeabiV7aDebug", false).isNotEmpty())
        assertTrue(project.getTasksByName("firebaseTestLabExecuteDebugInstrumentationMyDeviceUniversalDebug", false).isEmpty())
    }

    @Test
    fun `ensure tasks are created for abi splits with filter all`() {
        val simpleProject = File(javaClass.getResource("simple").file)
        val project = ProjectBuilder.builder().withProjectDir(simpleProject).build()
        project.plugins.apply("com.android.application")
        project.plugins.apply("firebase.test.lab")
        project.configure<AppExtension> {
            compileSdkVersion(27)
            defaultConfig.also {
                it.versionCode = 1
                it.versionName = "0.1"
                it.setMinSdkVersion(27)
                it.setTargetSdkVersion(27)
            }
            splits.also {
                it.abi.also {
                    it.isEnable = true
                    it.reset()
                    it.include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
                    it.isUniversalApk = true
                }
            }
        }
        project.configure<FirebaseTestLabPluginExtension> {
            googleProjectId = "test"
            keyFile = File(simpleProject, "key.json")
            createDevice("myDevice") {
                deviceIds = listOf("Nexus6")
                filterAbiSplits = true
            }
        }
        (project as ProjectInternal).evaluate()

        assertTrue(project.getTasksByName("firebaseTestLabExecuteDebugInstrumentationMyDeviceArm64V8aDebug", false).isEmpty())
        assertTrue(project.getTasksByName("firebaseTestLabExecuteDebugInstrumentationMyDeviceX86Debug", false).isEmpty())
        assertTrue(project.getTasksByName("firebaseTestLabExecuteDebugInstrumentationMyDeviceX8664Debug", false).isEmpty())
        assertTrue(project.getTasksByName("firebaseTestLabExecuteDebugInstrumentationMyDeviceArmeabiV7aDebug", false).isEmpty())
        assertTrue(project.getTasksByName("firebaseTestLabExecuteDebugInstrumentationMyDeviceUniversalDebug", false).isNotEmpty())
    }

    @Test
    fun `ensure tasks are created when abi split is disabled`() {
        val simpleProject = File(javaClass.getResource("simple").file)
        val project = ProjectBuilder.builder().withProjectDir(simpleProject).build()
        project.plugins.apply("com.android.application")
        project.plugins.apply("firebase.test.lab")
        project.configure<AppExtension> {
            compileSdkVersion(27)
            defaultConfig.also {
                it.versionCode = 1
                it.versionName = "0.1"
                it.setMinSdkVersion(27)
                it.setTargetSdkVersion(27)
            }
        }
        project.configure<FirebaseTestLabPluginExtension> {
            googleProjectId = "test"
            keyFile = File(simpleProject, "key.json")
            createDevice("myDevice") {
                deviceIds = listOf("Nexus6")
            }
        }
        (project as ProjectInternal).evaluate()

        assertTrue(project.getTasksByName("firebaseTestLabExecuteDebugInstrumentationMyDeviceDebug", false).isNotEmpty())
    }

    private fun executeTask(project: Project, taskName: String) {
        executeTask(project.getTasksByName(taskName, false).first())
    }

    private fun executeTask(task: Task) {
        task.taskDependencies.getDependencies(task).forEach {
            subTask -> executeTask(subTask)
        }
        println("Executing task: ${task.name}")
        task.actions.forEach { it.execute(task) }
    }
    
    @Test
    fun `ensure that tasks are sharded for single device when numShards is filled`() {
        val simpleProject = File(javaClass.getResource("simple").file)
        val project = ProjectBuilder.builder().withProjectDir(simpleProject).build()
        project.plugins.apply("com.android.application")
        project.plugins.apply("firebase.test.lab")
        project.configure<AppExtension> {
            compileSdkVersion(27)
            defaultConfig.also {
                it.versionCode = 1
                it.versionName = "0.1"
                it.setMinSdkVersion(27)
                it.setTargetSdkVersion(27)
            }
        }
        project.configure<FirebaseTestLabPluginExtension> {
            googleProjectId = "test"
            keyFile = File(simpleProject, "key.json")
            createDevice("myDevice") {
                deviceIds = listOf("Nexus6")
                numShards = 2
            }
        }
        (project as ProjectInternal).evaluate()
        
        
        assertTrue(project.getTasksByName("firebaseTestLabExecuteDebugInstrumentationMyDeviceDebugNumShards2ShardIndex0", false).isNotEmpty())
        assertTrue(project.getTasksByName("firebaseTestLabExecuteDebugInstrumentationMyDeviceDebugNumShards2ShardIndex1", false).isNotEmpty())
        assertTrue(project.getTasksByName("firebaseTestLabExecuteAllShardForConfigurationMyDeviceDebug", false).isNotEmpty())
    }
    
    @Test
    fun `ensure that tasks are sharded for two devices when numShards is filled`() {
        val simpleProject = File(javaClass.getResource("simple").file)
        val project = ProjectBuilder.builder().withProjectDir(simpleProject).build()
        project.plugins.apply("com.android.application")
        project.plugins.apply("firebase.test.lab")
        project.configure<AppExtension> {
            compileSdkVersion(27)
            defaultConfig.also {
                it.versionCode = 1
                it.versionName = "0.1"
                it.setMinSdkVersion(27)
                it.setTargetSdkVersion(27)
            }
        }
        project.configure<FirebaseTestLabPluginExtension> {
            googleProjectId = "test"
            keyFile = File(simpleProject, "key.json")
            createDevice("myDevice") {
                deviceIds = listOf("Nexus6")
                numShards = 2
            }
    
            createDevice("mySecondDevice") {
                deviceIds = listOf("Nexus5")
                numShards = 3
            }
        }
        (project as ProjectInternal).evaluate()
        
        
        assertTrue(project.getTasksByName("firebaseTestLabExecuteDebugInstrumentationMyDeviceDebugNumShards2ShardIndex0", false).isNotEmpty())
        assertTrue(project.getTasksByName("firebaseTestLabExecuteDebugInstrumentationMyDeviceDebugNumShards2ShardIndex1", false).isNotEmpty())
        assertTrue(project.getTasksByName("firebaseTestLabExecuteAllShardForConfigurationMyDeviceDebug", false).isNotEmpty())
    
        assertTrue(project.getTasksByName("firebaseTestLabExecuteDebugInstrumentationMySecondDeviceDebugNumShards3ShardIndex0", false).isNotEmpty())
        assertTrue(project.getTasksByName("firebaseTestLabExecuteDebugInstrumentationMySecondDeviceDebugNumShards3ShardIndex1", false).isNotEmpty())
        assertTrue(project.getTasksByName("firebaseTestLabExecuteDebugInstrumentationMySecondDeviceDebugNumShards3ShardIndex2", false).isNotEmpty())
        assertTrue(project.getTasksByName("firebaseTestLabExecuteAllShardForConfigurationMySecondDeviceDebug", false).isNotEmpty())
    }
}