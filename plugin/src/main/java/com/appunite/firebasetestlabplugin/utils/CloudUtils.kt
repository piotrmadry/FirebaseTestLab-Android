package com.appunite.firebasetestlabplugin.utils

internal fun <T> List<T>.joinArgs() = joinToString(",")

internal fun String.asCommand(): List<String> = split(" ", "\n").filterNot(String::isNullOrBlank)