package com.jaqxues.packcompiler

import org.gradle.api.Plugin
import org.gradle.api.Project
import java.io.File


/**
 * This file was created by Jacques Hoffmann (jaqxues) in the Project PackCompiler.<br>
 * Date: 16.06.20 - Time 20:14.
 */

@Suppress("MemberVisibilityCanBePrivate")
open class PackCompilerPluginExtension {
    var buildType: String? = null
    var attributes: Map<String, Any>? = null
    var jdkPath: String = System.getProperty("java.home")
    var getJarName: (() -> String)? = null
    var signConfigFile: File? = null
    var activeConfig: Pair<String, String>? = null

    val config get() =
        PackCompilerPluginConfig(
            buildType ?: throw IllegalStateException("Non-Nullable 'buildType' field was null"),
            attributes ?: throw IllegalStateException("Non-Nullable 'attributes' field was null"),
            jdkPath,
            getJarName ?: throw IllegalStateException("Non-Nullable 'getJarName' field was null"),
            signConfigFile,
            activeConfig
        )
}

data class PackCompilerPluginConfig(
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
                PackCompiler(extension.config).startCompilation()
            }
        }
    }
}