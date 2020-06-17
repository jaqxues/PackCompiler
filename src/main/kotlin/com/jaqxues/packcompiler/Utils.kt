package com.jaqxues.packcompiler

import java.io.File
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream


/**
 * This file was created by Jacques Hoffmann (jaqxues) in the Project PackCompiler.<br>
 * Date: 17.06.20 - Time 09:31.
 */

fun ZipInputStream.extractCurrentFile(targetFile: File) =
    targetFile.also { file ->
        file.outputStream().use {
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

fun executableCommand(path: String, name: String, winExtension: String): String {
    val cmd = File(path, name + if (isWindows) winExtension else "")
    check (cmd.exists()) { "File $cmd does not exist, command cannot be executed" }
    return cmd.absolutePath
}

val isWindows get() = "win" in System.getProperty("os.name").toLowerCase(Locale.ROOT)