package com.jaqxues.packcompiler

import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.security.KeyStore
import java.security.UnrecoverableKeyException
import java.util.*


/**
 * This file was created by Jacques Hoffmann (jaqxues) in the Project PackCompiler.<br>
 * Date: 16.06.20 - Time 21:18.
 */
class SignConfigModel(
    internal val keyStorePath: String,
    @Transient
    internal val keyStorePassword: String,
    internal val keyAlias: String,
    @Transient
    internal val keyPassword: String
) {
    fun checkSignKey() {
        if (!File(keyStorePath).exists()) throw FileNotFoundException("Specified Keystore File does not exist")

        try {
            val keystore = KeyStore.getInstance("JKS")
            FileInputStream(keyStorePath).use { inStream ->
                keystore.load(inStream, keyStorePassword.toCharArray())
            }
            if (!keystore.isKeyEntry(keyAlias))
                throw SignException("Keystore does not contain a Key with Alias $keyAlias")

            check(keystore.getKey(keyAlias, keyPassword.toCharArray()) != null) { "Unknown Exception occurred" }
        } catch (e: UnrecoverableKeyException) {
            throw SignException("Key Password Incorrect", e)
        } catch (e: IOException) {
            if (e.cause is UnrecoverableKeyException) {
                throw SignException("KeyStore Password incorrect", e)
            }
            throw e
        }
    }

    companion object {
        fun fromFile(file: File): SignConfigModel {
            val properties = file.inputStream().use { inStream ->
                val props = Properties()
                props.load(inStream)
                props
            }
            return SignConfigModel(
                properties["keyStorePath"] as String,
                properties["keyStorePassword"] as String,
                properties["keyAlias"] as String,
                properties["keyPassword"] as String
            )
        }
    }
}

class SignException : IllegalStateException {
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
}