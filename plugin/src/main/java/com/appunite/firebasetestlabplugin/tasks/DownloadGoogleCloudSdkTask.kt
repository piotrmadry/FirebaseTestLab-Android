package com.appunite.firebasetestlabplugin.tasks

import com.appunite.firebasetestlabplugin.FirebaseTestLabPluginExtension
import com.appunite.firebasetestlabplugin.utils.get

class DownloadGoogleCloudSdkTask : HiddenExec() {

    private val extension: FirebaseTestLabPluginExtension =
        project.extensions.findByType(FirebaseTestLabPluginExtension::class.java).get()


}