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
    var buildTypes: List<String>? = null
    var attributes: Map<String, Any>? = null
    var jdkPath: String = System.getProperty("java.home")
    var getJarName: ((PackCompilerPluginConfig) -> String)? = null
    var signConfigFile: File? = null
    var activeConfig: Pair<String, String>? = null

    val config get() =
        PackCompilerPluginConfig(
            buildTypes ?: throw IllegalStateException("Non-Nullable 'buildType' field was null"),
            attributes ?: throw IllegalStateException("Non-Nullable 'attributes' field was null"),
            jdkPath,
            getJarName ?: throw IllegalStateException("Non-Nullable 'getJarName' field was null"),
            signConfigFile,
            activeConfig
        )
}

data class PackCompilerPluginConfig(
    val buildTypes: List<String>,
    val attributes: Map<String, Any>,
    val jdkPath: String = System.getProperty("java.home"),
    private val getJarName: (PackCompilerPluginConfig) -> String,
    val signConfigFile: File? = null,
    val activeConfig: Pair<String, String>? = null
) {
    val jarName by lazy { getJarName(this) }
}

class PackCompilerPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create("packCompiler", PackCompilerPluginExtension::class.java)
        project.afterEvaluate {
            val conf = extension.config
            for (buildType in conf.buildTypes) {
                val buildTypeCap = buildType.capitalize()
                check(project.tasks.findByName("assemble$buildTypeCap") != null) { "Specified Build Type is not associated with an 'assemble' task! ('$buildType' - 'assemble$buildTypeCap')" }
                project.task("assemblePack$buildTypeCap") {
                    it.dependsOn("assemble$buildTypeCap")
                    it.group = "build"
                    it.description = "Compiles a Pack with the current Pack Configuration (BuildType: $buildType)"

                    it.doLast {
                        PackCompiler(conf, buildType, project.buildDir.absolutePath).startCompilation()
                    }
                }
            }

            project.task("assemblePack") { t ->
                t.dependsOn(*conf.buildTypes.map { "assemblePack${it.capitalize()}" }.toTypedArray())
                t.group = "build"
                t.description = "Compiles Packs for all given BuildTypes"
            }
        }
    }
}