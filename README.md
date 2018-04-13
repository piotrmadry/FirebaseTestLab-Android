# Firebase Test Lab Plugin for Android
[![Kotlin version badge](https://img.shields.io/badge/kotlin-1.1.60-blue.svg)](http://kotlinlang.org/)
[![License](https://img.shields.io/crates/l/rustc-serialize.svg)](https://github.com/piotrmadry/FirebaseTestLab-Android/blob/master/LICENSE)

![firebase](https://i.ytimg.com/vi/4_ZEEX1x17k/maxresdefault.jpg)

## Introduction
Firebase is actually the most popular developer tool platform, wchich handles almost every aspect of the app. It also gives possibility to run Android Tests on physical or virtual devices hosted in a Google data center through [Firebase Test Lab](https://firebase.google.com/docs/test-lab/). In order to fully exploit the potential of this tool I've created plugin to simplify process of creating tests configurations. It allows to run tests locally as well as on you CI server. 

#### Available features
- Automatic installation of `gcloud` command line tool
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

1. If you don't have a Firebase project for your app, go to the []Firebase console](https://console.firebase.google.com/) and click Create New Project to create one now. You will need ownership or edit permissions in your project.
2. Create a service account related with your firebase project with an Editor role in the [Google Cloud Platform console - IAM/Service Accounts](https://console.cloud.google.com/iam-admin/serviceaccounts/)
3. Copy `Project ID` from [Google Cloud Platform console - HOME](https://console.cloud.google.com/home)
4. Add plugin to your root project `build.gradle`:
   ```grovy
   buildscript {
       repositories {
           maven { url 'https://jitpack.io' }
       }
       dependencies {
           classpath 'com.github.jacek-marchwicki:FirebaseTestLab-Android:<version_from_github_releases_tab'
       }
   }
   ```
5. Add configuration in your project `build.gradle`:
    ```groovy
    apply plugin: 'firebase.test.lab'
 
    firebaseTestLab {
        keyFile = file("test-lab-key.json")
        googleProjectId = "your-project-app-id"
        devices {
            nexusEmulator {
                deviceIds = ["Nexus6"]
                androidApiLevels = [26]
            }
        }
    }
    ```
    List of available [devices](https://firebase.google.com/docs/test-lab/images/gcloud-device-list.png)
6. Run your instrumentations tests
    
    ```bash
    ./gradlew firebaseTestLabExecuteDebugInstrumentation
    ```
    
    Or run robo tests
    
    ```bash
    ./gradlew firebaseTestLabExecuteDebugRobo 
    ```

#### Advanced configuration

You can 
``` Goovy
// Setup firebase test lab plugin
firebaseTestLab {
    // REQUIRED obtain service key as described inside README
    keyFile = file("test-lab-key.json")
    // REQUIRED setup google project id ad described inside README
    googleProjectId = "your-project-app-id"

    // If you want you can ignore test failures
    // ignoreFailures = true

    // If you prefer you can use your custom google storage bucket for storing build sources and results
    // cloudBucketName = "your-custome-google-storage-bucket-name"
    // cloudDirectoryName = "your-custome-directory-name"

    // If you prefer to install gcloud tool manually you can set path by
    // cloudSdkPath = "/user/cloud-sdk/bin"

    // If you want to change default gcloud installation path (default is in build/gcloud directory)
    // you can set environment variable `export CLOUDSDK_INSTALL_DIR=`/cache/your_directory/`

    // REQUIRED
    devices {
        // REQUIRED add at least one device
        nexusEmulator {
            // REQUIRED Choose at least one device id
            // you can list all available via `gcloud firebase test android models list` or look on https://firebase.google.com/docs/test-lab/images/gcloud-device-list.png
            deviceIds = ["Nexus6"]

            // REQUIRED Choose at least one API level
            // you can list all available via `gcloud firebase test android models list` for your device model
            androidApiLevels = [26]

            // You can test app in landscape and portrait
            // screenOrientations = [com.appunite.firebasetestlabplugin.model.ScreenOrientation.PORTRAIT, com.appunite.firebasetestlabplugin.model.ScreenOrientation.LANDSCAPE]

            // Choose language (default is `en`)
            // you can list all available via `gcloud firebase test android locales list`
            // locales = ["en"]

            // If you are using ABI splits you can filter selected abi
            // filterAbiSplits = true
            // abiSplits = ["armeabi-v7a", "arm64-v8a", "x86", "x86_64"]

            // If you are using ABI splits you can remove testing universal APK
            // testUniversalApk = false

            // You can set timeout (in seconds) for test
            // timeout = 6000
        }
        // You can define more devices
        someOtherDevices {
            deviceIds = ["hammerhead", "shamu"]
            androidApiLevels = [21]
        }
    }
}
```

For more precise test selection run

```bash
./gradlew tasks 
```

to discover all available test options


