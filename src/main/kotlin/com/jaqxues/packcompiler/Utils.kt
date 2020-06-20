package com.jaqxues.packcompiler

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

lateinit var logger: Logger
