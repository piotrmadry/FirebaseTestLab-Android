package com.appunite.firebasetestlabplugin.utils

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

class Storage(buildDir: File) {
    private val file = File(buildDir, "setup.json")
    private val gson = Gson()
    
    fun read(key: String) = readStorage()[key]
    
    fun write(key: String, value: String) {
        val map = readStorage()
        map[key] = value
        gson.toJson(map)
    }
    
    private fun readStorage(): HashMap<String, String> {
        val text: String = file.bufferedReader().readLine()
        return gson.fromJson(text, object : TypeToken<HashMap<String, Any>>() {}.type)
    }
}