package com.jaqxues.packcompiler

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.jvm.tasks.Jar
import java.io.File
import java.util.zip.ZipInputStream


/**
 * This file was created by Jacques Hoffmann (jaqxues) in the Project PackCompiler.<br>
 * Date: 16.06.20 - Time 20:23.
 */
private const val BUILD_PATH = "pack_compiler/%s/"
private const val MANIFEST_FILE_PATH = BUILD_PATH + "manifest.txt"
private const val DEX_FILE_PATH = BUILD_PATH + "classes.dex"
private const val JAR_TARGET_PATH = "outputs/pack/%s/"

private const val PACK_APK = "outputs/apk/%1\$s/%2\$s-%1\$s.apk"

class PackCompiler(private val conf: PackCompilerPluginConfig, buildType: String, project: Project) {
    private val buildDir = project.buildDir
    private val projectName = project.name

    private val packApkPath = PACK_APK.format(buildType, projectName)
    private val buildPath = BUILD_PATH.format(buildType)
    private val manifestFilePath = MANIFEST_FILE_PATH.format(buildType)
    private val dexFilePath = DEX_FILE_PATH.format(buildType)
    private val jarTargetPath = JAR_TARGET_PATH.format(buildType)

    private val unsignedJarName get() = File(buildDir, jarTargetPath + "${conf.jarName}_unsigned.jar").absolutePath
    private val signedJarName get() = File(buildDir, jarTargetPath + "${conf.jarName}.jar").absolutePath

    fun configureDexTask(t: Task) {
        t.doLast {
            val apkFile = File(buildDir, packApkPath)
            check(apkFile.exists()) { "Pack File does not exist, cannot extract .dex file(s) ('${apkFile.absolutePath}')" }

            ZipInputStream(apkFile.inputStream()).use { zipInStream ->
                for (entry in zipInStream.entries) {
                    if (entry.isDirectory) continue
                    if (entry.name != "classes.dex") continue

                    // Copy classes.dex file
                    zipInStream.extractCurrentFile(File(buildDir, dexFilePath))
                    return@use
                }
                throw IllegalStateException("Could not find a 'classes.dex' entry in the specified file (${apkFile.absolutePath})")
            }
        }
    }

    fun configureJarTask(t: Jar) {
        t.manifest {
            it.attributes += conf.attributes
        }
        // Remove ".jar" since this is added by the task itself
        t.archiveBaseName.set(unsignedJarName.dropLast(4))

        t.from(File(buildDir, dexFilePath).absolutePath)
    }

    fun configureSignTask(t: Task, project: Project) {
        t.doLast {
            val signConfig = SignConfigModel.fromFile(conf.signConfigFile!!)
            signConfig.checkSignKey()
            check(File(unsignedJarName).exists()) { "File to be signed does not exist ('$unsignedJarName')" }

            // https://ant.apache.org/manual/Tasks/signjar.html
            project.ant.invokeMethod(
                "signjar", mapOf(
                    "jar" to unsignedJarName,
                    "alias" to signConfig.keyAlias,
                    "storePass" to signConfig.keyStorePassword,
                    "keystore" to signConfig.keyStorePath,
                    "keypass" to signConfig.keyPassword,
                    "signedJar" to signedJarName,
                    "tsaurl" to "http://timestamp.digicert.com"
                )
            )
        }
    }
}
