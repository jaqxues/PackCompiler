package com.jaqxues.packcompiler

import com.google.gson.JsonParser
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.jvm.tasks.Jar
import se.vidstige.jadb.JadbConnection
import se.vidstige.jadb.JadbDevice
import se.vidstige.jadb.RemoteFile
import java.io.File
import java.nio.file.Paths
import java.util.zip.ZipInputStream


/**
 * This file was created by Jacques Hoffmann (jaqxues) in the Project PackCompiler.<br>
 * Date: 16.06.20 - Time 20:23.
 */
private const val BUILD_PATH = "pack_compiler/%s/"
private const val DEX_FILE_PATH = BUILD_PATH + "classes.dex"
private const val JAR_TARGET_PATH = "outputs/pack/%s/"

private const val PACK_APK = "outputs/apk/%1\$s/%2\$s-%1\$s.apk"

class PackCompiler(private val conf: PackCompilerPluginConfig, buildType: String, project: Project) {
    private val buildDir = project.buildDir
    private val projectName = project.name

    private val packApkPath = PACK_APK.format(buildType, projectName)
    private val dexFilePath = DEX_FILE_PATH.format(buildType)
    private val jarTargetPath = JAR_TARGET_PATH.format(buildType)

    val unsignedJarFile get() = File(buildDir, jarTargetPath + "${conf.jarName}_unsigned.jar")
    val signedJarFile get() = File(buildDir, jarTargetPath + "${conf.jarName}.jar")

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

    @Suppress("UnstableApiUsage")
    fun configureJarTask(t: Jar) {
        t.manifest {
            it.attributes += conf.attributes
        }
        // Remove ".jar" since this is added by the task itself
        t.archiveBaseName.set(unsignedJarFile.absolutePath.dropLast(4))

        t.from(File(buildDir, dexFilePath).absolutePath)
    }

    fun configureSignTask(t: Task, project: Project, signConfig: SignConfigModel) {
        t.doLast {
            signConfig.checkSignKey()
            check(unsignedJarFile.exists()) { "File to be signed does not exist ('${unsignedJarFile.absolutePath}')" }

            // https://ant.apache.org/manual/Tasks/signjar.html
            project.ant.invokeMethod(
                "signjar", mapOf(
                    "jar" to unsignedJarFile.absolutePath,
                    "alias" to signConfig.keyAlias,
                    "storePass" to signConfig.keyStorePassword,
                    "keystore" to signConfig.keyStorePath,
                    "keypass" to signConfig.keyPassword,
                    "signedJar" to signedJarFile.absolutePath,
                    "tsaurl" to "http://timestamp.digicert.com"
                )
            )
        }
    }

    fun configureAdbPushTask(t: Task, adbExecutable: File, file: File) {
        t.doLast {
            val config = conf.adbPushConfig!!

            if (config.deviceConfigFile != null && !config.deviceConfigFile.exists()) {
                generateAdbConfig(config.deviceConfigFile)
                throw IllegalStateException("Exiting for you to notice - Please customize the Json at ${config.deviceConfigFile.absolutePath}")
            }

            // Starting Adb Server
            val returnCode = try {
                ProcessBuilder()
                    .command(adbExecutable.absolutePath, "start-server")
                    .inheritIO()
                    .start()
                    .waitFor()
            } catch (e: Exception) {
                throw IllegalStateException("Unable to start the Adb Server", e)
            }

            if (returnCode != 0)
                throw IllegalStateException("'adb start-server' returned $returnCode")

            val connection = JadbConnection()

            var deviceDefault = true
            var emulatorDefault = true
            val customState = mutableMapOf<String, Boolean>()
            val customPaths = mutableMapOf<String, String>()

            fun getDefaultForDevice(name: String) = if ("emulator" in name) emulatorDefault else deviceDefault

            config.deviceConfigFile?.let { file ->
                file.reader().use {
                    for (el in JsonParser.parseReader(it).asJsonArray) {
                        val jsonObj = el.asJsonObject
                        val name = jsonObj.getAsJsonPrimitive("name").asString
                        var enabled = jsonObj.getAsJsonPrimitive("enabled")?.asBoolean

                        if (enabled != null) {
                            if (name == "emulator_default") {
                                emulatorDefault = enabled
                                continue
                            } else if (name == "device_default") {
                                deviceDefault = enabled
                                continue
                            }

                            if (enabled != getDefaultForDevice(name)) {
                                customState[name] = enabled
                                if (!enabled) continue
                            }
                        }

                        enabled = enabled ?: getDefaultForDevice(name)

                        if (!enabled) continue

                        val pushPath = jsonObj.getAsJsonPrimitive("pushPath")?.asString
                        if (pushPath != null && pushPath != config.defaultPath) {
                            customPaths[name] = pushPath
                        }
                    }
                }
            }

            val devs = connection.devices.mapNotNull{ device ->
                val name = device.serial
                if (device.state != JadbDevice.State.Device)
                    return@mapNotNull null
                val enabled = customState[name] ?: getDefaultForDevice(name)
                if (!enabled) return@mapNotNull null

                val pushPath = customPaths[name] ?: config.defaultPath
                check(pushPath.startsWith("/")) { "Path must be absolute / start with '/'" }
                val path = Paths.get(pushPath, config.directory, file.name).toString()
                    .replace("\\", "/")

                try {
                    device.push(file, RemoteFile(path))
                } catch (e: Exception) {
                    throw IllegalStateException("Could not push file to device $name ('$path')", e)
                }
                name
            }

            println("Pushed to ${devs.size} device(s):")
            for (name in devs) println("\t* $name")
        }
    }

    private fun generateAdbConfig(file: File) {
        file.parentFile.mkdirs()
        file.createNewFile()
        file.writeText(
            """
                [
                  {
                    "name": "emulator_default",
                    "enabled": true
                  },
                  {
                    "name": "device_default",
                    "enabled": true
                  },
                  {
                    "name": "r9i23250",
                    "enabled": true,
                    "pushPath": "/sdcard/3423_4234/"
                
                
                
                    ,"TODO": "Read the Notes(!) and configure for your needs",
                    "Notes": [
                      "You do NOT need to configure this if default values are enough!",
                      "If you do not need to configure anything, replace the contents of this file with '[]'",
                
                      "Get the 'name' via `adb devices`",
                      "Allowing custom pushPaths to use device-specific mounts. This is not needed for internal storage",
                      "'enabled' will allow you to turn adb push off for specific devices",
                      "The path your file will be pushed to is 'pushPath_or_default_path/directory_from_build.gradle/generated_jar_file_name(_unsigned).jar'",
                      "emulator_default and device_default allow enabling or disabling all emulators or devices at once"
                    ],
                    "Default Values": {
                      "emulator_default": true,
                      "device_default": true,
                      "pushPath": "/storage/emulated/0/"
                    }
                  }
                ]
                """.trimIndent()
        )
        logger.warn("\n\nJson Config File was not found! A new Config File was generated, please configure it for your needs.")
        logger.warn("Path to Json: ${file.absolutePath}\n\n")
    }
}
