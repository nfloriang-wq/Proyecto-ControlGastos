package com.example.controlgastos

import android.content.ContentValues
import android.content.Context

class UsuarioRepositorySQLite(context: Context) {

    private val dbHelper = GastoSQLiteHelper(context)

    fun existeUsuario(nombreUsuario: String): Boolean {
        val db = dbHelper.readableDatabase

        val cursor = db.rawQuery(
            "SELECT id FROM usuarios WHERE nombre_usuario = ?",
            arrayOf(nombreUsuario)
        )

        val existe = cursor.moveToFirst()
        cursor.close()

        return existe
    }

    fun registrarUsuario(
        nombreUsuario: String,
        passwordHash: String
    ): Boolean {
        val db = dbHelper.writableDatabase

        val valores = ContentValues().apply {
            put("nombre_usuario", nombreUsuario)
            put("password_hash", passwordHash)
        }

        val resultado = db.insert("usuarios", null, valores)

        return resultado != -1L
    }

    fun buscarUsuario(nombreUsuario: String): Usuario? {
        val db = dbHelper.readableDatabase

        val cursor = db.rawQuery(
            """
            SELECT id, nombre_usuario, password_hash
            FROM usuarios
            WHERE nombre_usuario = ?
            """.trimIndent(),
            arrayOf(nombreUsuario)
        )

        val usuario = if (cursor.moveToFirst()) {
            Usuario(
                id = cursor.getInt(0),
                nombreUsuario = cursor.getString(1),
                passwordHash = cursor.getString(2)
            )
        } else {
            null
        }

        cursor.close()
        return usuario
    }
}
