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
        logger = project.logger

        val extension = project.extensions.create("packCompiler", PackCompilerPluginExtension::class.java)
        val appProject = project.rootProject.findProject("app")
            ?: throw IllegalStateException("Could not find :app project!")
        checkProguardRules(appProject)
        val androidExtension = AndroidExtensionWrapper(appProject.extensions.getByName("android"))

        project.afterEvaluate {
            val conf = extension.config
            for (buildType in androidExtension.buildTypes) {
                val buildTypeCap = buildType.capitalize()
                check(project.tasks.findByName("assemble$buildTypeCap") != null) {
                    "Specified Build Type is not associated with an 'assemble' task! ('$buildType' - 'assemble$buildTypeCap')"
                }

                val packCompiler = PackCompiler(conf, buildType, project)

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

                val signConfig = androidExtension.getSignConfigsForBuildType(buildType)
                val shouldSign = signConfig != null && signConfig.keyAlias != "AndroidDebugKey"
                if (shouldSign) {
                    project.task("signPackJar$buildTypeCap") { t ->
                        t.dependsOn("bundlePack$buildTypeCap")
                        t.group = "pack compiler"
                        t.description = "Sign the output JarFile."

                        packCompiler.configureSignTask(t, project, signConfig!!)
                    }
                }

                if (conf.adbPushConfig != null) {
                    project.task("adbPushPack$buildTypeCap") { t ->
                        val jarFile = if (shouldSign) {
                            t.dependsOn("signPackJar$buildTypeCap")
                            packCompiler.signedJarFile
                        } else {
                            t.dependsOn("bundlePack$buildTypeCap")
                            packCompiler.unsignedJarFile
                        }
                        t.group = "pack compiler"
                        t.description = "Generates a Pack and pushes it to connected devices via ADB"

                        packCompiler.configureAdbPushTask(t, androidExtension.adbExecutable, jarFile)
                    }
                }
            }
        }
    }
}