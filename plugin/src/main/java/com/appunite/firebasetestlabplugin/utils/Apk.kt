package com.appunite.firebasetestlabplugin.utils

import com.android.build.gradle.api.TestVariant
import java.io.File

interface ApkSource {
    val testApk: File
    val apk: File
}

internal class VariantApkSource(variant: TestVariant) : ApkSource {
    override val apk: File = variant.testedVariant.outputs.first().outputFile
    override val testApk: File = variant.outputs.first().outputFile
}