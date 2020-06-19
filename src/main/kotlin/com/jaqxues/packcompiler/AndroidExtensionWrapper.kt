package com.jaqxues.packcompiler

import org.gradle.api.NamedDomainObjectContainer
import java.io.File


/**
 * This file was created by Jacques Hoffmann (jaqxues) in the Project PackCompiler.<br>
 * Date: 19.06.20 - Time 18:07.
 *
 * https://google.github.io/android-gradle-dsl/current/com.android.build.gradle.AppExtension.html
 */
class AndroidExtensionWrapper(private val extension: Any) {

    val buildTypes
        get() = extension.invokeMethod<NamedDomainObjectContainer<*>>("getBuildTypes")
            .names.toList()

    @Suppress("UNCHECKED_CAST")
    private fun <T> Any.invokeMethod(name: String) = this::class.java.getMethod(name).invoke(this) as T

    fun getSignConfigsForBuildType(buildType: String): SignConfigModel? {
        val currentBuildType = extension.invokeMethod<NamedDomainObjectContainer<*>>("getBuildTypes")
            .findByName(buildType) ?: throw IllegalArgumentException("BuildType $buildType is not in BuildTypes!")
        val signingConfig = currentBuildType.invokeMethod<Any?>("getSigningConfig") ?: return null

        return SignConfigModel(
            signingConfig.invokeMethod<File>("getStoreFile").absolutePath,
            signingConfig.invokeMethod("getStorePassword"),
            signingConfig.invokeMethod("getKeyAlias"),
            signingConfig.invokeMethod("getKeyPassword")
        )
    }

    val adbExecutable get() = extension.invokeMethod<File>("getAdbExecutable")
}