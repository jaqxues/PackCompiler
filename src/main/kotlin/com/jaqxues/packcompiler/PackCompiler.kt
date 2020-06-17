package com.jaqxues.packcompiler

import java.io.File
import java.util.zip.ZipInputStream


/**
 * This file was created by Jacques Hoffmann (jaqxues) in the Project PackCompiler.<br>
 * Date: 16.06.20 - Time 20:23.
 */
const val BUILD_PATH = "pack_compiler/"
const val MANIFEST_FILE_PATH = BUILD_PATH + "manifest.txt"
const val DEX_FILE_PATH = BUILD_PATH + "classes.dex"
const val JAR_TARGET_PATH = "outputs/pack/"

const val PACK_APK = "outputs/apk/%s/packimpl-%<s.apk"

class PackCompiler(private val extension: PackCompilerPluginConfig, buildType: String, private val buildDir: String) {
    private val packApkPath = PACK_APK.format(buildType)

    fun startCompilation() {
        val manifestFile = createManifestFile()

        val jarTarget = File(buildDir, JAR_TARGET_PATH + extension.jarName + ".jar")
        jarTarget.parentFile.mkdirs()

        extractDexFile()

        val unsignedJarFile = createJarFile(manifestFile)

        val signConfigFile = extension.signConfigFile
        if (signConfigFile != null) {
            signOutput(unsignedJarFile, signConfigFile, jarTarget)
        }
    }

    private fun signOutput(jarFile: File, signConfigFile: File, jarTargetFile: File) {
        val signConfig = SignConfigModel.fromFile(signConfigFile)
        signConfig.checkSignKey()
        val pb = ProcessBuilder(
            executableCommand(extension.jdkPath, "bin/jarsigner", "exe"),
            "-tsa", "http://timestamp.digicert.com/",
            "-keystore", signConfig.keyStorePath,
            "-signedjar", jarTargetFile.absolutePath,
            jarFile.absolutePath,
            signConfig.keyAlias
        )
            .redirectError(ProcessBuilder.Redirect.INHERIT)
        val process = pb.start()
        process.outputStream.writer().use { writer ->
            writer.append(signConfig.keyStorePassword)
                .append("\n")
                .append(signConfig.keyPassword)
                .flush()
        }
        if (process.waitFor() != 0)
            throw IllegalStateException("Could not run the jarsigner command ('${pb.command().joinToString()}')")
    }

    private fun createJarFile(manifestFile: File): File {
        val unsignedJarFile = File(buildDir, JAR_TARGET_PATH + extension.jarName + "_unsigned.jar")
        val pb = ProcessBuilder(
            executableCommand(extension.jdkPath, "bin/jar", "exe"),
            "cfm",
            unsignedJarFile.absolutePath,
            manifestFile.absolutePath,
            "-C", File(buildDir, BUILD_PATH).absolutePath,
            "classes.dex"
        ).inheritIO()
        if (pb.start().waitFor() != 0)
            throw IllegalStateException("Could not run the jar command ('${pb.command().joinToString()}'")
        return unsignedJarFile
    }

    private fun createManifestFile(): File {
        val maniFestFile = File(buildDir, MANIFEST_FILE_PATH)
        if (!maniFestFile.exists()) {
            maniFestFile.parentFile.mkdirs()
            maniFestFile.createNewFile()
        }

        maniFestFile.writer().use { writer ->
            for ((k, v) in extension.attributes)
                writer.write("$k: $v\n")
            writer.flush()
        }

        return maniFestFile
    }

    private fun extractDexFile(): File {
        val apkFile = File(buildDir, packApkPath)
        check(apkFile.exists()) { "Pack File does not exist, cannot extract .dex file(s) ('${apkFile.absolutePath}')" }

        return ZipInputStream(apkFile.inputStream()).use { zipInStream ->
            for (entry in zipInStream.entries) {
                if (entry.isDirectory) continue
                if (entry.name != "classes.dex") continue

                // Copy classes.dex file
                return@use zipInStream.extractCurrentFile(File(buildDir, DEX_FILE_PATH))
            }
            throw IllegalStateException("Could not find a 'classes.dex' entry in the specified file (${apkFile.absolutePath})")
        }
    }
}
