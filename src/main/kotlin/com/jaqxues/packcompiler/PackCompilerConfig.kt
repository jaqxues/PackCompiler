package com.jaqxues.packcompiler


/**
 * This file was created by Jacques Hoffmann (jaqxues) in the Project PackCompiler.<br>
 * Date: 19.06.20 - Time 13:17.
 */
@Suppress("MemberVisibilityCanBePrivate")
open class PackCompilerPluginExtension {
    var attributes: Map<String, Any>? = null
    var getJarName: ((PackCompilerPluginConfig) -> String)? = null

    val config get() =
        PackCompilerPluginConfig(
            attributes ?: throw IllegalStateException("Non-Nullable 'attributes' field was null"),
            getJarName ?: throw IllegalStateException("Non-Nullable 'getJarName' field was null")
        )
}

data class PackCompilerPluginConfig(
    val attributes: Map<String, Any>,
    private val getJarName: (PackCompilerPluginConfig) -> String
) {
    val jarName by lazy { getJarName(this) }
}
