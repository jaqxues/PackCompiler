package com.jaqxues.packcompiler

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.jvm.tasks.Jar


/**
 * This file was created by Jacques Hoffmann (jaqxues) in the Project PackCompiler.<br>
 * Date: 16.06.20 - Time 20:14.
 */
class PackCompilerPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val extension = project.extensions.create("packCompiler", PackCompilerPluginExtension::class.java)

        project.afterEvaluate {
            val conf = extension.config
            for (buildType in conf.buildTypes) {
                val buildTypeCap = buildType.capitalize()
                check(project.tasks.findByName("assemble$buildTypeCap") != null) {
                    "Specified Build Type is not associated with an 'assemble' task! ('$buildType' - 'assemble$buildTypeCap')"
                }

                val packCompiler = PackCompiler(conf, buildType, project.buildDir.absolutePath)

                project.task("extractPackDex$buildTypeCap") { t ->
                    t.dependsOn("assemble$buildTypeCap")
                    t.group = "pack compiler"
                    t.description = "Extract Dex File(s) from the Dynamic Feature APKs"

                    packCompiler.configureDexTask(t)
                }

                project.tasks.register("bundlePack$buildTypeCap", Jar::class.java) { t ->
                    t.dependsOn("extractPackDex$buildTypeCap")
                    t.group = "pack compiler"
                    t.description = "Compile the classes.dex and the given manifest to a JarFile"

                    packCompiler.configureJarTask(t)
                }

                if (conf.signConfigFile != null && conf.signConfigFile.exists()) {
                    project.task("signPackJar$buildTypeCap") { t ->
                        t.dependsOn("bundlePack$buildTypeCap")
                        t.group = "pack compiler"
                        t.description = "Sign the output JarFile."

                        packCompiler.configureSignTask(t, project)
                    }
                }
            }
        }
    }
}