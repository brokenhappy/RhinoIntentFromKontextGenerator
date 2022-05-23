package com.woutwerkman.rhinok

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*
import org.jetbrains.annotations.Contract
import java.io.File
import java.nio.file.Path
import kotlin.io.path.name

open class GenerateRhinoIntentsFromKontextTask(
    /** The location of the yaml file with the Rhino context based on the project root */
    @Input val yamlLocation: String,
    /** The location of the file based on the project root where the Kotlin intent code will be placed */
    @Input val outputLocation: String,
    /**
     * The package in which the generated code will reside. Will not change the location of where the file is stored.
     * If null, it will base it on the path of the [outputLocation] and the sourceFile root it's inside or if this
     * could not be found, or if the [outputLocation] is directly in the source file,
     */
    @Input
    @Optional
    val packageName: String? = null
) : DefaultTask() {

    private sealed interface TaskInput {
        data class Incorrect(val reason: String) : TaskInput
        data class Correct(val outputFile: File, val yamlFile: File, val packageName: String?) : TaskInput
    }

    private fun getTaskInput(): TaskInput {
        val yamlFile = project.rootDir.resolve(yamlLocation).takeIf { it.exists() }
            ?: return TaskInput.Incorrect("$yamlLocation is not a valid location!")
        val outputFile: File = outputLocation.takeIf { it.endsWith(".kt") }?.let { project.rootDir.resolve(it) }
            ?: return TaskInput.Incorrect("A path to a .kt file must be given to generate the Intent code!")
        if (outputFile.isDirectory)
            return TaskInput.Incorrect("Output file must not be a directory, got ${outputFile.path}")
        return TaskInput.Correct(
            outputFile,
            yamlFile,
            packageName ?: determinePackageNameBasedOn(outputFile.toPath(), inside = sourceSets.getAllSources())
        )
    }

    @Contract(pure = false)
    @TaskAction
    fun generate() {
        when (val input = getTaskInput()) {
            is TaskInput.Incorrect -> project.logger.error(input.reason)
            is TaskInput.Correct -> {
                input.outputFile.setContent(generateCode(input.yamlFile, input.packageName))
            }
        }
    }

    private val sourceSets get() = project.extensions.getByName("sourceSets") as SourceSetContainer
}

private fun SourceSetContainer.getAllSources() = flatMap { it.allSource }.map { it.toPath() }

/** @see com.woutwerkman.rhinok.DeterminingPackageNameTest for documentation */
internal fun determinePackageNameBasedOn(outputFile: Path, inside: List<Path>): String? =
    inside.firstOrNull { source -> outputFile.startsWith(source) }?.let { outputFile.parent.toPackageNameInside(it) }

private fun Path.toPackageNameInside(root: Path): String? =
    if (this == root) null
    else generateSequence(this) { it.parent }
        .takeWhile { it != root }
        .toList()
        .asReversed()
        .joinToString(".") { it.name }

@Contract(pure = false)
private fun File.setContent(utf8Content: String) {
    bufferedWriter().use { writer -> writer.append(utf8Content) }
}

private fun generateCode(yamlFile: File, packageName: String?) : String =
    RhinoKCodeGenerator().generateFrom(RhinoKYamlReader().readRhinoContext(yamlFile.reader()), packageName)
