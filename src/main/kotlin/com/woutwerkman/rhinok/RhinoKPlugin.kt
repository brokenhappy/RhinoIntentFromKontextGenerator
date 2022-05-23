package com.woutwerkman.rhinok

import org.gradle.api.Plugin
import org.gradle.api.Project

@Suppress("unused")
class RhinoKPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.tasks.create("generateRhinoKontextCode", GenerateRhinoIntentsFromKontextTask::class.java)
    }
}