package com.appunite.utils

import com.android.build.gradle.api.TestVariant
import org.gradle.api.Project
import java.io.File

interface ApkSource {
    val testApk: File
    val apk: File
}

internal class BuildParameterApkSource(private val project: Project) : ApkSource {
    override val testApk: File
        get() = File(project.property("testApk") as String)
    override val apk: File
        get() = File(project.property("apk") as String)
}

internal class VariantApkSource(variant: TestVariant) : ApkSource {
    override val apk: File = variant.testedVariant.outputs.first().outputFile
    override val testApk: File = variant.outputs.first().outputFile
}