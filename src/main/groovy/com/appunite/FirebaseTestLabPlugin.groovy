package com.appunite

import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class FirebaseTestLabPlugin implements Plugin<Project> {

    @Override
    public void apply(final Project target) {
        def showDevicesTask = target.tasks.create("showDevices") << {
            def adbExe = target.android.getAdbExe().toString()
            println "${adbExe} devices".execute().text
        }
        showDevicesTask.group = "Test Lab Plugin"
        showDevicesTask.description = "Runs adb devices command"
    }
}
