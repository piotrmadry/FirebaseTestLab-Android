# Gradle Firebase Test Lab plugin

[![License](https://img.shields.io/badge/License-Apache%202.0-yellowgreen.svg)](https://www.apache.org/licenses/LICENSE-2.0)

Gradle plugin for Firebase inspired by [this project](https://github.com/gildor/gradle-firebase-test-lab-plugin)

This plugin uses [Firebase Test Lab](https://firebase.google.com/docs/test-lab/) to run your Instrumental tests and download results.

## Requirements
- Firebase project with configured billing plan (Custom bucket name and variety of testing devices requires it)
- Installed, authorized and inited [Google Cloud Sdk](https://cloud.google.com/sdk/?utm_source=google&utm_medium=cpc&utm_campaign=2017-q1-cloud-emea-gcp-bkws-freetrial&gclid=CLCGn7b0wdQCFcwaGAodGqsJqA&dclid=CIW1srb0wdQCFZvNsgodLwkBjQ)

## Configuration

```buildscript {
  repositories {
    maven {
      url "https://plugins.gradle.org/m2/"
    }
  }
  dependencies {
    classpath "gradle.plugin.firebase.test.lab:firebase-test-lab:0.7"
  }
}

apply plugin: "firebase.test.lab"```

```
or for Gradle 2.1 and higher
```
plugins {
  id "firebase.test.lab" version "0.7"
}
```
