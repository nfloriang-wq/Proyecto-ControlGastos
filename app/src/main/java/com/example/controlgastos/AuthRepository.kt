package com.example.controlgastos

import android.content.ContentValues
import android.content.Context

// Gestionar el acceso de usuarios en laaplicación.
class AuthRepository(context: Context) {

    // Guarda el contexto de la aplicación para usarlo en la base de datos.
    private val appContext = context.applicationContext

    // Función que permite ingresar a la aplicación usando el nombre de usuario.
    // Devuelve un objeto Usuario si el ingreso es correcto o null si ocurre algún problema.
    fun ingresarPorNombre(nombreUsuario: String): Usuario? {

        // Elimina los espacios en blanco al inicio y al final del nombre.
        val nombre = nombreUsuario.trim()

        // Comprueba que el nombre ingresado no esté vacío.
        if (nombre.isEmpty()) {
            return null
        }

        val helper = GastoSQLiteHelper(appContext)

        // Abre la base de datos en modo escritura.
        val db = helper.writableDatabase

        return try {
            db.beginTransaction()

            // Busca si ya existe un usuario con el nombre ingresado.
            val cursor = db.rawQuery(
                "SELECT id, nombre_usuario, password_hash FROM usuarios WHERE nombre_usuario = ?",
                arrayOf(nombre)
            )

            var usuario: Usuario? = null

            // Comprueba si la consulta encontró algún registro.
            if (cursor.moveToFirst()) {

                // Obtiene los datos del usuario desde la base de datos.
                usuario = Usuario(
                    id = cursor.getInt(0),
                    nombreUsuario = cursor.getString(1),
                    passwordHash = cursor.getString(2)
                )
            }

            cursor.close()

            // Si el usuario no existe, lo registra.
            if (usuario == null) {

                // Prepara los valores que se guardarán en la tabla usuarios.
                val valores = ContentValues().apply {
                    put("nombre_usuario", nombre)

                    put("password_hash", "")
                }

                // Inserta el nuevo usuario y obtiene su identificador.
                val idNuevo = db.insert("usuarios", null, valores)

                // Comprueba si el usuario se insertó correctamente.
                if (idNuevo != -1L) {

                    usuario = Usuario(
                        id = idNuevo.toInt(),
                        nombreUsuario = nombre,
                        passwordHash = ""
                    )

                    // Conservar los gastos antiguos de la cuenta interna.
                    // Prepara el identificador del nuevo usuario para actualizar los gastos.
                    val valoresGastos = ContentValues().apply {
                        put("usuario_id", idNuevo.toInt())
                    }

                    // Actualiza los gastos que pertenecían al usuario interno con ID 1,
                    // asignándolos al nuevo usuario creado.
                    db.update(
                        "gastos",
                        valoresGastos,
                        "usuario_id = ?",
                        arrayOf("1")
                    )
                }
            }

            db.setTransactionSuccessful()

            usuario

        } catch (e: Exception) {

            // Registra el error ocurrido durante el proceso de ingreso.
            android.util.Log.e(
                "ControlGastos",
                "Error al ingresar por nombre",
                e
            )

            null

        } finally {

            // Comprueba si todavía existe una transacción activa.
            if (db.inTransaction()) {

                db.endTransaction()
            }
        }
    }
}
