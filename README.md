# Gradle Firebase Test Lab plugin

[![License](https://img.shields.io/badge/License-Apache%202.0-yellowgreen.svg)](https://www.apache.org/licenses/LICENSE-2.0)

Gradle plugin for Firebase inspired by [this project](https://github.com/gildor/gradle-firebase-test-lab-plugin)

This plugin uses [Firebase Test Lab](https://firebase.google.com/docs/test-lab/) to run your Instrumental tests and download results.

## Requirements
- Firebase project with configured billing plan (Custom bucket name and variety of testing devices requires it)
- Installed, authorized and inited [Google Cloud Sdk](https://cloud.google.com/sdk/?utm_source=google&utm_medium=cpc&utm_campaign=2017-q1-cloud-emea-gcp-bkws-freetrial&gclid=CLCGn7b0wdQCFcwaGAodGqsJqA&dclid=CIW1srb0wdQCFZvNsgodLwkBjQ)

## Features
- Ability to run it on CI side (with proper Cloud SDK authorization)
- Your bucket directory and local download directory are synced. It means that you will download only not yet synced files.
- You can specify download location
- Bucket naming management

## Configuration

```
buildscript {
  repositories {
    maven {
      url "https://plugins.gradle.org/m2/"
    }
  }
  dependencies {
    classpath "gradle.plugin.firebase.test.lab:firebase-test-lab:0.7"
  }
}

apply plugin: "firebase.test.lab"
```
or for Gradle 2.1 and higher
```
plugins {
  id "firebase.test.lab" version "0.7"
}
```
## Plugin Configuration

```
firebaseTestLab {
    //[REQUIRED FILED] Path to gcloud from your Cloud SDK
    cloudSdkPath = "/builds/zumba/zumba-android/cloud-sdk/bin"
    //[REQUIRED FILED] You bucket name to identify place to store your tests results [requres billing plan]
    cloudBucketName = "zumba-test"
    // Destination path for your results
    resultsDestinationPath = "/builds/zumba/zumba-android"
    // Name of directory where you can find your tests results (remotely in your bucket at Google Cloud Console and at your       download location)
    resultsTestDir = "ui-testing"
    //It will not throw any exception even if some tests failed
    ignoreFailures = true

    // Types of artifacts to download
    artifacts {
        junit = true
        logcat = false
        video = false
        instrumentation = false
    }
    // Types of tests [devices](https://firebase.google.com/docs/test-lab/images/gcloud-device-list.png)  
    platforms {
        //Those fields are required to run tests on this device
        galaxyNote4 {
            androidApiLevels = [22]
            deviceIds = ["trelte"]
        }
        // More advanced configuration
        nexus5 {
            androidApiLevels = [21]
            deviceIds = ["hammerhead"]
            locales = ["en", "fr"]
            orientations = ["portrait", "landscape"]
            timeout = 20
        }
    }
}
```
#### Note that every addition element in fields lists will create new device configuration. For example: hammerhead-21-en-portrait, hammerhead-21-fr-portrait, hammerhead-21-en-landscape, hammerhead-21-fr-landscape. 

## How to run 
`./gradlew uploadTestLab`

