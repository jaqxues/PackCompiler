package com.jaqxues.packcompiler

import java.io.File


/**
 * This file was created by Jacques Hoffmann (jaqxues) in the Project PackCompiler.<br>
 * Date: 19.06.20 - Time 13:17.
 */
@Suppress("MemberVisibilityCanBePrivate")
open class PackCompilerPluginExtension {
    var buildTypes: List<String>? = null
    var attributes: Map<String, Any>? = null
    var getJarName: ((PackCompilerPluginConfig) -> String)? = null
    var signConfigFile: File? = null

    val config get() =
        PackCompilerPluginConfig(
            buildTypes ?: throw IllegalStateException("Non-Nullable 'buildType' field was null"),
            attributes ?: throw IllegalStateException("Non-Nullable 'attributes' field was null"),
            signConfigFile,
            getJarName ?: throw IllegalStateException("Non-Nullable 'getJarName' field was null")
        )
}

data class PackCompilerPluginConfig(
    val buildTypes: List<String>,
    val attributes: Map<String, Any>,
    val signConfigFile: File? = null,
    private val getJarName: (PackCompilerPluginConfig) -> String
) {
    val jarName by lazy { getJarName(this) }
}
