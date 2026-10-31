package com.example.controlgastos

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import android.util.Base64

object PasswordUtils {

    private const val ITERACIONES = 120000
    private const val LONGITUD_CLAVE = 256

    fun generarHash(contrasena: String): String {
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)

        val spec = PBEKeySpec(
            contrasena.toCharArray(),
            salt,
            ITERACIONES,
            LONGITUD_CLAVE
        )

        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val hash = factory.generateSecret(spec).encoded
        spec.clearPassword()

        return Base64.encodeToString(salt, Base64.NO_WRAP) +
                ":" +
                Base64.encodeToString(hash, Base64.NO_WRAP)
    }

    fun verificar(contrasena: String, hashGuardado: String): Boolean {
        return try {
            val partes = hashGuardado.split(":")
            if (partes.size != 2) return false

            val salt = Base64.decode(partes[0], Base64.NO_WRAP)
            val hashOriginal = Base64.decode(partes[1], Base64.NO_WRAP)

            val spec = PBEKeySpec(
                contrasena.toCharArray(),
                salt,
                ITERACIONES,
                LONGITUD_CLAVE
            )

            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            val hashNuevo = factory.generateSecret(spec).encoded
            spec.clearPassword()

            MessageDigest.isEqual(hashOriginal, hashNuevo)
        } catch (e: Exception) {
            false
        }
    }
}
