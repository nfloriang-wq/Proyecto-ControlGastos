package com.example.controlgastos

import android.content.ContentValues
import android.content.Context
import umg.edu.gt.progra2.gastos.datos.GastoRepository
import umg.edu.gt.progra2.gastos.modelo.Categoria
import umg.edu.gt.progra2.gastos.modelo.Gasto

// Encargada de administrar los gastos utilizando la base de datos SQLite.
class GastoRepositorySQLite(
    context: Context,
    private val usuarioId: Int = 1
) : GastoRepository {

    // ayudante que permite acceder a la base de datos.
    private val dbHelper = GastoSQLiteHelper(context)

    // Función que permite guardar un nuevo gasto en la base de datos.
    override suspend fun agregar(gasto: Gasto): Gasto {

        val db = dbHelper.writableDatabase

        // Prepara los datos del gasto que se van a guardar.
        val valores = ContentValues().apply {
            // Guarda la descripción del gasto.
            put("descripcion", gasto.descripcion)

            put("monto", gasto.monto)

            put("categoria", gasto.categoria.name)

            put("fecha", gasto.fecha)

            put("usuario_id", usuarioId)
        }

        // Inserta el gasto en la tabla y obtiene su identificador.
        val id = db.insert("gastos", null, valores)

        // Devuelve el gasto con el ID que se generó en la base de datos.
        return gasto.copy(id = id.toInt())
    }

    // Función que obtiene todos los gastos registrados del usuario actual.
    override suspend fun listar(): List<Gasto> {

        val db = dbHelper.readableDatabase

        val lista = mutableListOf<Gasto>()

        // Consulta los gastos del usuario y los ordena del más reciente al más antiguo.
        val cursor = db.rawQuery(
            "SELECT * FROM gastos WHERE usuario_id = ? ORDER BY id DESC",
            arrayOf(usuarioId.toString())
        )

        while (cursor.moveToNext()) {

            // Crea un objeto Gasto con la información obtenida de cada registro.
            val gasto = Gasto(
                id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                descripcion = cursor.getString(
                    cursor.getColumnIndexOrThrow("descripcion")
                ),
                monto = cursor.getDouble(
                    cursor.getColumnIndexOrThrow("monto")
                ),
                categoria = Categoria.valueOf(
                    cursor.getString(
                        cursor.getColumnIndexOrThrow("categoria")
                    )
                ),
                fecha = cursor.getString(
                    cursor.getColumnIndexOrThrow("fecha")
                )
            )

            lista.add(gasto)
        }

        cursor.close()

        return lista
    }

    // Función que busca un gasto específico utilizando su identificador.
    override suspend fun buscarPorId(id: Int): Gasto? {

        return listar().find { it.id == id }
    }


    // Función que permite modificar la información de un gasto existente.
    override suspend fun actualizar(gasto: Gasto): Boolean {

        val db = dbHelper.writableDatabase

        // Prepara los nuevos datos que se guardarán en el gasto.
        val valores = ContentValues().apply {

            // Actualiza la descripción del gasto.
            put("descripcion", gasto.descripcion)

            put("monto", gasto.monto)

            put("categoria", gasto.categoria.name)

            put("fecha", gasto.fecha)
        }

        // Actualiza el registro que coincida con el ID y el usuario correspondiente.
        val filas = db.update(
            "gastos",
            valores,
            "id = ? AND usuario_id = ?",
            arrayOf(gasto.id.toString(), usuarioId.toString())
        )

        return filas > 0
    }


    // Función que permite eliminar un gasto mediante su identificador.
    override suspend fun eliminar(id: Int): Boolean {

        val db = dbHelper.writableDatabase

        // Elimina el gasto que coincida con el ID y el usuario correspondiente.
        val filas = db.delete(
            "gastos",
            "id = ? AND usuario_id = ?",
            arrayOf(id.toString(), usuarioId.toString())
        )

        return filas > 0
    }
}
