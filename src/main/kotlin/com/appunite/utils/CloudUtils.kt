package com.appunite.utils

import java.io.File

internal fun <T> List<T>.joinArgs() = joinToString(",")

internal fun String.asCommand(): List<String> = split(" ", "\n").filterNot(String::isNullOrBlank)

internal fun String.startCommand(): Process = ProcessBuilder(asCommand()).start()

internal fun command(command: String, gcloudPath: File?) = "${if (gcloudPath == null) "" else gcloudPath.canonicalPath + "/"}$command"