package com.woutwerkman.rhinok

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

open class GenerateRhinoKontextCodeTask : DefaultTask() {
    /** The location of the yaml file with the Rhino context based on the project root */
    @Input
    var yamlLocation: String? = null

    /** The location based on the project root where the Kotlin intent file will be generated */
    @Input
    var outputLocation: String? = null

    @TaskAction
    fun generate() {
        val absoluteYamlLocation = yamlLocation?.let { project.rootDir.resolve(it) }
        if (absoluteYamlLocation?.exists() != true) {
            project.logger.error("$absoluteYamlLocation is not a valid yaml location!")
            return
        }

        val absoluteOutputLocation = outputLocation?.let { project.rootDir.resolve(it) }
        if (absoluteOutputLocation == null) {
            project.logger.error("Output location must be given!")
            return
        }
        absoluteOutputLocation.takeUnless { it.exists() }?.createNewFile()
        absoluteOutputLocation.bufferedWriter().use { bufferedWriter ->
            bufferedWriter.append(
                RhinoKCodeGenerator().generateFrom(RhinoKYamlReader().readRhinoContext(absoluteYamlLocation.reader()))
            )
        }
    }
}
