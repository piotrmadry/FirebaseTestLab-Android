
import com.gradle.publish.PluginBundleExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.script.lang.kotlin.*

group = "firebase.test.lab"
version = "0.0.6.5"

buildscript {
    repositories {
        jcenter()
        gradleScriptKotlin()
        maven { setUrl("https://plugins.gradle.org/m2/") }
    }

    dependencies {
        classpath(kotlinModule("gradle-plugin"))
        classpath("com.gradle.publish:plugin-publish-plugin:0.9.7")
    }
}

apply {
    plugin("kotlin")
    plugin("maven")
    plugin("com.gradle.plugin-publish")
    plugin("maven-publish")
}

repositories {
    jcenter()
    gradleScriptKotlin()
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

configure<PublishingExtension> {
    this.publications {
        create<MavenPublication>("local"){
            from(components.getByName("java"))
        }
    }
    this.repositories {
        mavenLocal()
    }
}

dependencies {
    compile(gradleApi())
    compile(kotlinModule("stdlib", "1.1.2"))
    compile(gradleScriptKotlinApi())
    compile("com.android.tools.build:gradle:2.2.3")
    testCompile("junit:junit:4.11")
}
