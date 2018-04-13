import com.gradle.publish.PluginBundleExtension
import org.gradle.kotlin.dsl.`kotlin-dsl`
import org.gradle.kotlin.dsl.gradleKotlinDsl
import org.gradle.kotlin.dsl.kotlin
import org.gradle.script.lang.kotlin.*


plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    `maven`
    id("com.gradle.plugin-publish") version "0.9.9"
}

group = "firebase.test.lab"
version = "1.0.4"

repositories {
    maven("https://repo.gradle.org/gradle/libs-releases-local/")
}

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
    testCompile("junit:junit-dep:4.11")
    testCompile("org.gradle:gradle-kotlin-dsl:0.13.1")
}


