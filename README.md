# Firebase Test Lab Plugin for Android
[![Kotlin version badge](https://img.shields.io/badge/kotlin-1.1.60-blue.svg)](http://kotlinlang.org/)
[![License](https://img.shields.io/crates/l/rustc-serialize.svg)](https://github.com/piotrmadry/FirebaseTestLab-Android/blob/master/LICENSE)

![firebase](https://i.ytimg.com/vi/4_ZEEX1x17k/maxresdefault.jpg)

## Introduction
Firebase is actually the most popular developer tool platform, wchich handles almost every aspect of the app. It also gives possibility to run Android Tests on physical or virtual devices hosted in a Google data center through [Firebase Test Lab](https://firebase.google.com/docs/test-lab/). In order to fully exploit the potential of this tool I've created plugin to simplify process of creating tests configurations. It allows to run tests locally as well as on you CI server. 

#### Available features
- Creating tasks for testable `buildType`[By default it is `debug`. If you want to change it use `testBuildType "buildTypeName"`]
- Creating tasks for every defined device and configuration separetly [ including Instrumented / Robo tests ]
- Creating tasks which runs all configurations at once
- Ability to download tests results to specific location
- Ability to clear directory inside bucket before test run

#### Benefits
- Readability
- Simplicity
- Remote and Local Testing
- Compatible with Gradle 3.0 

#### Setup 

``` Groovy
buildscript {
  repositories {
    maven {
      url "https://plugins.gradle.org/m2/"
    }
  }
  dependencies {
    classpath "gradle.plugin.firebase.test.lab:plugin:1.0.4"
  }
}

apply plugin: "firebase.test.lab"
```
``` Groovy
//For gradle 2.1+
plugins {
  id "firebase.test.lab" version "1.0.4"
}
```

#### How to use it

Add devices configurations inside `build.gradle`
List of available [devices](https://firebase.google.com/docs/test-lab/images/gcloud-device-list.png)

``` Goovy
firebaseTestLab {
    keyFile = file("keys.json")
    googleProjectId = "your-project-id"
    cloudSdkPath = "/user/cloud-sdk/bin"
    cloudBucketName = "bucket-test"
    cloudDirectoryName = "androidTests"
    clearDirectoryBeforeRun = true

    devices {
        galaxyS7 {
            androidApiLevels = [23]
            deviceIds = ["herolte"]
        }
    }
}
```






