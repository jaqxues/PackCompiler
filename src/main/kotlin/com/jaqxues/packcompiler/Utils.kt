package com.jaqxues.packcompiler

import java.io.File
import java.util.*
import java.util.zip.ZipInputStream


/**
 * This file was created by Jacques Hoffmann (jaqxues) in the Project PackCompiler.<br>
 * Date: 17.06.20 - Time 09:31.
 */

fun ZipInputStream.extractCurrentFile(targetPath: String): File {
    val file = File(targetPath)
    file.outputStream().use {
        copyTo(it, 4096)
    }
    return file
}

val ZipInputStream.entries get() = iterator {
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