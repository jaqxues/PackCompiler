package com.jaqxues.packcompiler

import groovy.lang.Closure
import org.gradle.util.ConfigureUtil
import java.io.File


/**
 * This file was created by Jacques Hoffmann (jaqxues) in the Project PackCompiler.<br>
 * Date: 19.06.20 - Time 13:17.
 */
@Suppress("MemberVisibilityCanBePrivate")
open class PackCompilerPluginExtension {
    var attributes: ((String) -> Map<String, Any>)? = null
    var getJarName: ((Map<String, Any>) -> String)? = null
    var adbPush: Closure<AdbPushConfigExtension>? = null

    fun buildConfig(buildType: String) =
            PackCompilerPluginConfig(
                (attributes ?: throw IllegalStateException("Non-Nullable 'attributes' field was null"))(buildType),
                getJarName ?: throw IllegalStateException("Non-Nullable 'getJarName' field was null"),
                adbPush?.let {
                    AdbPushConfigExtension().run {
                        ConfigureUtil.configure(it, this)
                        config
                    }
                }
            )
}

open class AdbPushConfigExtension {
    var deviceConfigFile: File? = null
    var defaultPath: String? = null
    var directory: String? = null

    val config: AdbPushConfig
        get() {
            return AdbPushConfig(
                deviceConfigFile,
                defaultPath ?: throw IllegalStateException("Non-Nullable 'defaultPath' field was null"),
                directory ?: throw IllegalStateException("Non-Nullable 'directory' field was null")
            )
        }
}

data class PackCompilerPluginConfig(
    val attributes: Map<String, Any>,
    val jarName: String,
    val adbPushConfig: AdbPushConfig?
) {
    constructor(attributes: Map<String, Any>, getJarName: (Map<String, Any>) -> String, adbPushConfig: AdbPushConfig?)
            : this(attributes, getJarName(attributes), adbPushConfig)
}

data class AdbPushConfig(
    val deviceConfigFile: File?,
    val defaultPath: String,
    val directory: String
)