package com.example.controlgastos

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

// clase encargada de crear, abrir y actualizar la base de datos SQLite.
class GastoSQLiteHelper(context: Context) :
    SQLiteOpenHelper(context, "ControlGastos.db", null, 3) {

    // Función que se ejecuta cuando se crea la base de datos por primera vez.
    override fun onCreate(db: SQLiteDatabase) {

        // Crea la tabla de usuarios donde se almacenan las cuentas registradas.
        db.execSQL("""
            CREATE TABLE usuarios (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                nombre_usuario TEXT NOT NULL UNIQUE,
                password_hash TEXT NOT NULL
            )
        """.trimIndent())

        // Crea un usuario inicial para la aplicación.
        val usuarioInicial = ContentValues().apply {
            put("id", 1)

            put("nombre_usuario", "principal")

            put("password_hash", "")
        }

        // Inserta el usuario inicial en la tabla de usuarios.
        db.insert("usuarios", null, usuarioInicial)

        // Crea la tabla de gastos donde se almacenan los gastos registrados.
        db.execSQL("""
            CREATE TABLE gastos (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                descripcion TEXT NOT NULL,
                monto REAL NOT NULL,
                categoria TEXT NOT NULL,
                fecha TEXT NOT NULL,
                usuario_id INTEGER NOT NULL DEFAULT 1
            )
        """.trimIndent())

        // Crea la tabla de sueldos mensuales de los usuarios.
        db.execSQL("""
            CREATE TABLE sueldos (
                usuario_id INTEGER PRIMARY KEY,
                monto REAL NOT NULL DEFAULT 0
            )
        """.trimIndent())
    }

    override fun onUpgrade(
        db: SQLiteDatabase,
        oldVersion: Int,
        newVersion: Int
    ) {

        if (oldVersion < 2) {

            // Crea la tabla de usuarios durante la actualización.
            db.execSQL("""
                CREATE TABLE usuarios (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    nombre_usuario TEXT NOT NULL UNIQUE,
                    password_hash TEXT NOT NULL
                )
            """.trimIndent())

            // Prepara los datos del usuario inicial.
            val usuarioInicial = ContentValues().apply {
                put("id", 1)

                put("nombre_usuario", "principal")

                put("password_hash", "")
            }

            db.insert("usuarios", null, usuarioInicial)

            // Agrega la columna usuario_id a la tabla de gastos.
            // Esto permite identificar a qué usuario pertenece cada gasto.
            db.execSQL("""
                ALTER TABLE gastos
                ADD COLUMN usuario_id INTEGER NOT NULL DEFAULT 1
            """.trimIndent())
        }

        if (oldVersion < 3) {

            // Crea la tabla de sueldos para almacenar el monto mensual de cada usuario.
            db.execSQL("""
                CREATE TABLE sueldos (
                    usuario_id INTEGER PRIMARY KEY,
                    monto REAL NOT NULL DEFAULT 0
                )
            """.trimIndent())
        }
    }
}
