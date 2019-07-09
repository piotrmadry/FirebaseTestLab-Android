# Firebase Test Lab Plugin for Android
[![Plugin version badge](https://img.shields.io/static/v1.svg?label=plugin&message=2.5.0&color=blue)](https://plugins.gradle.org/plugin/firebase.test.lab)
[![License](https://img.shields.io/crates/l/rustc-serialize.svg)](https://github.com/piotrmadry/FirebaseTestLab-Android/blob/master/LICENSE)

## Introduction
Plugin for which integrates Firebase Test Lab with Android Project. Simplify running Android Tests on Firebase platform locally as well as on using Continuous integration. 

### Contributors
- [Jacek Marchwicki](https://github.com/jacek-marchwicki)

#### Available features

- Automatic installation of `gcloud` command line tool
- Creating tasks for testable `buildType`[By default it is `debug`. If you want to change it use `testBuildType "buildTypeName"`]
- Creating tasks for every defined device and configuration separately [ including Instrumented / Robo tests ]
- Creating tasks which runs all configurations at once
- Ability to download tests results to specific location
- Ability to clear directory inside bucket before test run
- Instrumented tests sharding

#### Benefits

- Readability
- Simplicity
- Remote and Local Testing
- Compatible with Gradle 3.0 
- Instrumented Tests sharding for parallel test execution

#### Setup 

1. If you don't have a Firebase project for your app, go to the [Firebase console](https://console.firebase.google.com/) and click Create New Project to create one now. You will need ownership or edit permissions in your project.
2. Create a service account related with your firebase project with an Editor role in the [Google Cloud Platform console - IAM/Service Accounts](https://console.cloud.google.com/iam-admin/serviceaccounts/)
3. Copy `Project ID` from [Google Cloud Platform console - HOME](https://console.cloud.google.com/home)
4. Add plugin to your root project `build.gradle`:
   ```grovy
   buildscript {
       repositories {
           maven {
              url "https://plugins.gradle.org/m2/"
           }
       }
       dependencies {
           classpath "firebase.test.lab:plugin:X.X.X"
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
                deviceIds = ["hammerhead"]
                androidApiLevels = [23]
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
    // If not specified default is: a timestamp with a random suffix
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
            deviceIds = ["hammerhead"]

            // REQUIRED Choose at least one API level
            // you can list all available via `gcloud firebase test android models list` for your device model
            androidApiLevels = [23]

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
            
            // For instrumented test you can specify number of shards, which allows to split all the tests for [numShards] times and execute them in parallel
            // numShards = 4

            // You can set timeout (in seconds) for test
            // timeout = 6000

            // Enable Android Test Orchestrator more info at: https://developer.android.com/training/testing/junit-runner
            // isUseOrchestrator = true // default false

            // A list of one or more test target filters to apply (default: run all test targets)
            // testTargets = ["size large"]

            // Environment variables are mirrored as extra options to the am instrument -e KEY1 VALUE1
            // environmentVariables = ["clearPackageData=true", "coverage=true"]

            // The fully-qualified class name of the instrumentation test runner
            // testRunnerClass = "com.my.package.MyRunner"

            // Pass any custom param for gcloud
            // customParamsForGCloudTool = --no-performance-metrics
        }
        // You can define more devices
        someOtherDevices {
            deviceIds = ["shamu", "flounder"]
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


