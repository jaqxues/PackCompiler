package com.jaqxues.packcompiler

import com.google.gson.JsonParser
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.FileCollection
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
private const val DEX_DIR = BUILD_PATH + "dexes/"
private const val JAR_TARGET_PATH = "outputs/pack/%s/"

private const val PACK_APK = "outputs/apk/%1\$s/%2\$s-%1\$s.apk"
private val CLASSES_DEX_REGEX = "classes\\d*\\.dex".toRegex()

class PackCompiler(private val conf: PackCompilerPluginConfig, buildType: String, project: Project) {
    private val buildDir = project.buildDir
    private val projectName = project.name

    private val packApkPath = PACK_APK.format(buildType, projectName)
    private val dexDir = DEX_DIR.format(buildType)
    private val jarTargetPath = JAR_TARGET_PATH.format(buildType)

    val unsignedJarFile get() = File(buildDir, jarTargetPath + "${conf.jarName}_unsigned.jar")
    val signedJarFile get() = File(buildDir, jarTargetPath + "${conf.jarName}.jar")

    fun configureDexTask(t: Task) {
        t.outputs.dir(File(buildDir, dexDir))
        t.outputs.upToDateWhen { false }

        t.doLast {
            val apkFile = File(buildDir, packApkPath)
            check(apkFile.exists()) { "Pack File does not exist, cannot extract .dex file(s) ('${apkFile.absolutePath}')" }

            ZipInputStream(apkFile.inputStream()).use { zipInStream ->
                var found = false
                for (entry in zipInStream.entries) {
                    if (entry.isDirectory || !(entry.name matches CLASSES_DEX_REGEX)) continue
                    found = true

                    val dexFile = File(buildDir, dexDir + entry.name)
                    // Copy classes(d).dex file
                    zipInStream.extractCurrentFile(dexFile)
                }

                if (!found)
                    throw IllegalStateException("Could not find a single 'classes.dex' entry in the specified file (${apkFile.absolutePath})")
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

        File(buildDir, dexDir).listFiles()?.forEach {
            if (it.name matches CLASSES_DEX_REGEX)
                t.from(it.absolutePath)
        }
    }

    fun configureSignTask(t: Task, project: Project, signConfig: SignConfigModel) {
        t.outputs.file(signedJarFile)
        t.outputs.upToDateWhen { false }

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
        t.outputs.file(file)
        t.outputs.upToDateWhen { false }

        t.doLast {
            val config = conf.adbPushConfig!!

            if (config.deviceConfigFile != null && !config.deviceConfigFile.exists()) {
                generateAdbConfig(config.deviceConfigFile, t.project.rootDir)
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

    private fun generateAdbConfig(file: File, rootDir: File) {
        val parent = file.parentFile
        println(file.absolutePath)
        println(parent.absolutePath)
        if (!parent.exists()) {
            checkGitIgnore(parent, rootDir)
            parent.mkdirs()
        }
        file.createNewFile()
        this::class.java.classLoader.getResourceAsStream("AdbPushConfig.json")!!.use { input ->
            file.outputStream().use { output ->
                input.copyTo(output, 8192)
            }
        }
        logger.warn("\n\nJson Config File was not found! A new Config File was generated, please configure it for your needs.")
        logger.warn("Path to Json: ${file.absolutePath}\n\n")
    }

    private fun checkGitIgnore(file: File, rootDir: File) {
        File(rootDir, ".gitignore").apply {
            val relative = file.toRelativeString(rootDir)
            if (!exists()) {
                parentFile.mkdirs()
                createNewFile()
            }
            bufferedReader().useLines {
                for (line in it) {
                    if (relative in line)
                        return
                }
            }
            appendText("\n$relative\n")
            logger.error("$absolutePath has been modified to include /$relative!")
        }
    }
}
