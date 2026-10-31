package com.example.controlgastos

import android.content.ContentValues
import android.content.Context

class SueldoRepositorySQLite(
    context: Context,
    private val usuarioId: Int
) {

    private val dbHelper = GastoSQLiteHelper(context)

    // Obtener el sueldo mensual del usuario
    fun obtenerSueldo(): Double {

        val db = dbHelper.readableDatabase

        val cursor = db.rawQuery(
            "SELECT monto FROM sueldos WHERE usuario_id = ?",
            arrayOf(usuarioId.toString())
        )

        val sueldo = if (cursor.moveToFirst()) {
            cursor.getDouble(0)
        } else {
            0.0
        }

        cursor.close()

        return sueldo
    }

    // Guardar o actualizar el sueldo mensual
    fun guardarSueldo(monto: Double) {

        val db = dbHelper.writableDatabase

        val valores = ContentValues().apply {
            put("usuario_id", usuarioId)
            put("monto", monto)
        }

        db.insertWithOnConflict(
            "sueldos",
            null,
            valores,
            android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
        )
    }
}