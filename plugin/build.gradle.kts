import com.gradle.publish.PluginBundleExtension
import org.gradle.kotlin.dsl.`kotlin-dsl`
import org.gradle.kotlin.dsl.gradleKotlinDsl
import org.gradle.kotlin.dsl.kotlin
import org.gradle.script.lang.kotlin.*


plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    id("com.gradle.plugin-publish") version "0.9.9"
}

group = "firebase.test.lab"
version = "1.0.4"

gradlePlugin {
    (plugins) {
        "FirebaseTestLabPlugin" {
            id = "firebase-test-lab"
            implementationClass = "com.appunite.firebasetestlabplugin.FirebaseTestLabPlugin"
        }
    }
}

configure<PluginBundleExtension> {
    website = "https://github.com/piotrmadry/firebase-test-lab-gradle-plugin"
    vcsUrl = "https://github.com/piotrmadry/firebase-test-lab-gradle-plugin.git"
    description = "Gradle plugin for Android Test Lab"
    tags = listOf("firebase", "test-lab", "espresso", "instrumental-tests", "kotlin", "android")

    this.plugins {
        "FirebaseTestLabPlugin" {
            id = "firebase.test.lab"
            displayName = "Gradle Firebase Test Lab Plugin"
        }
    }
}

dependencies {
    compileOnly(gradleKotlinDsl())
    implementation(kotlin("stdlib", "1.1.2"))
    implementation("com.android.tools.build:gradle:3.0.1")
}


