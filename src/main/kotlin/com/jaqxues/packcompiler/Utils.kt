package com.jaqxues.packcompiler

import org.gradle.api.Project
import org.gradle.api.logging.Logger
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream


/**
 * This file was created by Jacques Hoffmann (jaqxues) in the Project PackCompiler.<br>
 * Date: 17.06.20 - Time 09:31.
 */

fun ZipInputStream.extractCurrentFile(targetFile: File) {
    targetFile.parentFile.mkdirs()
    targetFile.createNewFile()
    targetFile.outputStream().use {
        copyTo(it, 4096)
    }
}

// fixme: Kotlin Type Inference not working correctly
@Suppress("RemoveExplicitTypeArguments")
val ZipInputStream.entries get() = iterator<ZipEntry> {
    while (true) {
        val entry = nextEntry
        yield(entry ?: return@iterator)
        closeEntry()
    }
}

fun checkProguardRules(project: Project) {
    val file = File(project.buildDir, "pack_compiler/proguard_mappings/current.txt")
    if (file.exists())
        return

    file.parentFile.mkdirs()
    file.createNewFile()

    project.file("proguard-rules.pro").apply {
        useLines {
            for (line in it) {
                if ("# Save Mappings to specified path and reuse mapping so APK - Pack Compatibility won't break" in line)
                    return
            }
        }
        appendText(
            "\n\n" +
            """
                # Save Mappings to specified path and reuse mapping so APK - Pack Compatibility won't break
                -printmapping build/pack_compiler/proguard_mappings/current.txt
                -applymapping build/pack_compiler/proguard_mappings/current.txt
            """.trimIndent()
        )
        logger.warn("Modified Proguard rules for Pack Compatibility!")
    }
}

lateinit var logger: Logger
