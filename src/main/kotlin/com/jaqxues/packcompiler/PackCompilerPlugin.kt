package com.jaqxues.packcompiler

import org.gradle.api.Plugin
import org.gradle.api.Project
import java.io.File


/**
 * This file was created by Jacques Hoffmann (jaqxues) in the Project PackCompiler.<br>
 * Date: 16.06.20 - Time 20:14.
 */

data class PackCompilerPluginExtension(
    val buildType: String,
    val attributes: Map<String, Any>,
    val jdkPath: String = System.getProperty("java.home"),
    val getJarName: () -> String,
    val signConfigFile: File? = null,
    val activeConfig: Pair<String, String>? = null
)

class PackCompilerPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create("packCompiler", PackCompilerPluginExtension::class.java)
        project.task("assemblePack") {
            it.dependsOn("assemble")
            it.group = "build"
            it.description = "Compiles a Pack with the current Pack Configuration"

            it.doLast {
                PackCompiler(extension).startCompilation()
            }
        }
    }
}