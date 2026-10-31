package com.example.controlgastos

import android.os.Bundle
import android.content.Intent
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import umg.edu.gt.progra2.gastos.modelo.Gasto
import umg.edu.gt.progra2.gastos.modelo.Categoria
import umg.edu.gt.progra2.gastos.servicio.ControlGastos
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Clase principal que controla la pantalla de administración de gastos.
class MainActivity : AppCompatActivity() {

    // Campos donde el usuario ingresa la descripción, el monto y el sueldo.
    private lateinit var etDescripcion: EditText
    private lateinit var etMonto: EditText
    private lateinit var etSueldo: EditText

    // Lista desplegable que permite seleccionar la categoría del gasto.
    private lateinit var spCategoria: Spinner

    // Botones para guardar gastos, guardar sueldo y cerrar sesión.
    private lateinit var btnGuardar: Button
    private lateinit var btnGuardarSueldo: Button
    private lateinit var btnCerrarSesion: Button

    // Textos utilizados para mostrar el saludo y el resumen del presupuesto.
    private lateinit var tvSaludo: TextView
    private lateinit var tvTotal: TextView
    private lateinit var tvSueldo: TextView
    private lateinit var tvGastosMes: TextView
    private lateinit var tvDisponible: TextView

    // Textos donde se muestran los gastos registrados y el versículo bíblico.
    private lateinit var tvListaGastos: TextView
    private lateinit var tvVersiculo: TextView
    private lateinit var tvReferencia: TextView

    // Repositorios y servicio encargados de administrar los datos de la aplicación.
    private lateinit var repositorio: GastoRepositorySQLite
    private lateinit var controlGastos: ControlGastos
    private lateinit var repositorioSueldo: SueldoRepositorySQLite

    private var usuarioId: Int = -1

    // Función que se ejecuta al abrir la pantalla principal.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val preferencias = getSharedPreferences(
            "ControlGastosSesion",
            MODE_PRIVATE
        )

        usuarioId = preferencias.getInt("usuario_id", -1)

        // Comprueba si existe una sesión activa.
        if (usuarioId == -1) {

            // Si no hay una sesión activa, regresa a la pantalla de inicio de sesión.
            startActivity(Intent(this, LoginActivity::class.java))

            // Finaliza esta actividad.
            finish()
            return
        }

        // Inicializa el repositorio de gastos correspondiente al usuario actual.
        repositorio = GastoRepositorySQLite(this, usuarioId)

        // Crea el servicio que permite administrar y validar los gastos.
        controlGastos = ControlGastos(repositorio)

        // Inicializa el repositorio encargado de administrar el sueldo del usuario.
        repositorioSueldo = SueldoRepositorySQLite(
            this,
            usuarioId
        )

        // Conecta la actividad con el diseño XML de la pantalla principal.
        setContentView(R.layout.activity_main)

        // Obtener el nombre del usuario e iniciar el saludo.
        tvSaludo = findViewById(R.id.tvSaludo)

        // Recupera el nombre del usuario guardado en las preferencias.
        // Elimina espacios innecesarios y obtiene una cadena vacía si no existe.
        val nombreUsuario = preferencias
            .getString("nombre_usuario", "")
            ?.trim()
            .orEmpty()

        // Muestra un saludo personalizado utilizando el nombre del usuario.
        tvSaludo.text = if (nombreUsuario.isNotEmpty()) {
            "Hola, $nombreUsuario 👋"
        } else {
            "Hola 👋"
        }

        // campos de texto con los elementos del diseño XML.
        etDescripcion = findViewById(R.id.etDescripcion)
        etMonto = findViewById(R.id.etMonto)
        etSueldo = findViewById(R.id.etSueldo)

        spCategoria = findViewById(R.id.spCategoria)

        // Relaciona los botones con los elementos del diseño XML.
        btnGuardar = findViewById(R.id.btnGuardar)
        btnGuardarSueldo = findViewById(R.id.btnGuardarSueldo)
        btnCerrarSesion = findViewById(R.id.btnCerrarSesion)

        // textos que muestran el resumen del presupuesto.
        tvTotal = findViewById(R.id.tvTotal)
        tvSueldo = findViewById(R.id.tvSueldo)
        tvGastosMes = findViewById(R.id.tvGastosMes)
        tvDisponible = findViewById(R.id.tvDisponible)

        // textos donde se muestran los gastos y el versículo.
        tvListaGastos = findViewById(R.id.tvListaGastos)
        tvVersiculo = findViewById(R.id.tvVersiculo)
        tvReferencia = findViewById(R.id.tvReferencia)

        configurarCategorias()

        lifecycleScope.launch {
            mostrarGastos()
        }

        cargarVersiculo()

        btnGuardar.setOnClickListener {
            registrarGasto()
        }

        btnGuardarSueldo.setOnClickListener {
            guardarSueldo()
        }

        btnCerrarSesion.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Cerrar sesión")
                .setMessage("¿Deseas salir de tu cuenta?")
                .setNegativeButton("Cancelar", null)

                // Si el usuario confirma, elimina los datos de la sesión.
                .setPositiveButton("Cerrar sesión") { _, _ ->

                    // Elimina el identificador y el nombre guardados.
                    preferencias.edit()
                        .remove("usuario_id")
                        .remove("nombre_usuario")
                        .apply()

                    // Prepara la navegación hacia la pantalla de inicio de sesión.
                    val intent = Intent(
                        this,
                        LoginActivity::class.java
                    )

                    // Limpia las actividades anteriores para evitar regresar
                    // a la pantalla principal después de cerrar sesión.
                    intent.flags =
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_CLEAR_TASK

                    // Abre nuevamente la pantalla de inicio de sesión.
                    startActivity(intent)

                    finish()
                }
                .show()
        }
    }

    // Esta función permite registrar el sueldo del usuario.
    private fun guardarSueldo() {

        val sueldoTexto = etSueldo.text.toString().trim()

        if (sueldoTexto.isEmpty()) {
            etSueldo.error = "Ingresa tu sueldo mensual"
            return
        }

        // Convierte el texto ingresado a un número decimal.
        val sueldo = sueldoTexto.toDoubleOrNull()

        // Comprueba que el sueldo sea un número válido y mayor que cero.
        if (sueldo == null || !sueldo.isFinite() || sueldo <= 0) {
            etSueldo.error = "Ingresa un sueldo válido"
            return
        }

        // Guarda el sueldo en el repositorio correspondiente al usuario.
        repositorioSueldo.guardarSueldo(sueldo)

        etSueldo.text.clear()

        Toast.makeText(
            this,
            "Sueldo guardado correctamente",
            Toast.LENGTH_SHORT
        ).show()

        // Actualiza la información de los gastos y el presupuesto.
        lifecycleScope.launch {
            mostrarGastos()
        }
    }

    // Cargar el versículo del día.
    // versículo bíblico mediante la API.
    private fun cargarVersiculo() {

        lifecycleScope.launch {

            // Permite controlar posibles errores durante la conexión.
            try {

                // Ejecuta la conexión a Internet en un hilo secundario.
                // Esto evita bloquear la pantalla mientras se obtiene la información.
                val resultado = withContext(Dispatchers.IO) {

                    // Dirección de la API que proporciona el versículo del día en español.
                    val url = URL(
                        "https://api.midvash.com/v1/votd?language=es"
                    )

                    // Abre la conexión con el servidor.
                    val conexion =
                        url.openConnection() as HttpURLConnection

                    try {

                        // Configura el método de solicitud HTTP.
                        conexion.requestMethod = "GET"

                        // Establece el tiempo máximo para establecer la conexión.
                        conexion.connectTimeout = 15000

                        // Establece el tiempo máximo para recibir la respuesta.
                        conexion.readTimeout = 15000

                        // Indica que se espera recibir información en formato JSON.
                        conexion.setRequestProperty(
                            "Accept",
                            "application/json"
                        )

                        // Obtiene el código de respuesta del servidor.
                        val codigo = conexion.responseCode

                        // Comprueba que la respuesta del servidor sea correcta.
                        if (codigo != HttpURLConnection.HTTP_OK) {
                            throw IOException("Error HTTP: $codigo")
                        }

                        // Lee el contenido recibido desde la API.
                        val respuesta = conexion.inputStream
                            .bufferedReader()
                            .use { it.readText() }

                        // Convierte la respuesta recibida en un objeto JSON.
                        val json = JSONObject(respuesta)

                        // Obtiene el texto del versículo.
                        val texto = json
                            .optString("text", "")
                            .trim()

                        // Obtiene la referencia bíblica del versículo.
                        val referencia = json
                            .optString("reference", "")
                            .trim()

                        // Comprueba que la respuesta contenga el texto
                        // y la referencia del versículo.
                        if (texto.isBlank() || referencia.isBlank()) {
                            throw IOException(
                                "La respuesta no contiene el versículo esperado"
                            )
                        }

                        // Devuelve el versículo y su referencia juntos.
                        Pair(texto, referencia)

                    } finally {

                        // Cierra la conexión con el servidor.
                        conexion.disconnect()
                    }
                }

                // Muestra el texto del versículo en la pantalla.
                tvVersiculo.text = resultado.first

                // Muestra la referencia bíblica correspondiente.
                tvReferencia.text = resultado.second

            } catch (e: Exception) {

                // Muestra un mensaje si no fue posible obtener el versículo.
                tvVersiculo.text =
                    "No se pudo cargar el versículo. Intenta nuevamente."

                // Limpia el campo de la referencia si ocurrió un error.
                tvReferencia.text = ""

                // Registra el error para facilitar su identificación.
                android.util.Log.e(
                    "ControlGastos",
                    "Error al cargar el versículo",
                    e
                )
            }
        }
    }

    // Configurar las categorías.
    // Esta función prepara las opciones que aparecerán en la lista desplegable.
    private fun configurarCategorias() {

        // Obtiene todas las categorías definidas en la clase Categoria.
        val categorias = Categoria.values()

        // Crea un adaptador para mostrar las categorías en el Spinner.
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            categorias
        )

        // Define el diseño que tendrán las opciones desplegables.
        adapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        // Asigna el adaptador a la lista de categorías.
        spCategoria.adapter = adapter
    }

    // Registrar un gasto.
    // Esta función obtiene los datos ingresados y permite guardar un nuevo gasto.
    private fun registrarGasto() {

        // Obtiene la descripción y el monto ingresados por el usuario.
        val descripcion = etDescripcion.text.toString().trim()
        val montoTexto = etMonto.text.toString().trim()

        // Comprueba que la descripción no esté vacía.
        if (descripcion.isEmpty()) {
            etDescripcion.error = "Ingresa una descripción"
            return
        }

        // Comprueba que el monto no esté vacío.
        if (montoTexto.isEmpty()) {
            etMonto.error = "Ingresa un monto"
            return
        }

        // Convierte el monto ingresado a un número decimal.
        val monto = montoTexto.toDoubleOrNull()

        // Comprueba que el monto sea válido y mayor que cero.
        if (monto == null || !monto.isFinite() || monto <= 0) {
            etMonto.error = "Ingresa un monto válido"
            return
        }

        // Obtiene la categoría seleccionada en la lista desplegable.
        val categoria = spCategoria.selectedItem as Categoria

        // Obtiene la fecha actual con el formato año-mes-día.
        val fecha = SimpleDateFormat(
            "yyyy-MM-dd",
            Locale.getDefault()
        ).format(Date())

        // Crea un objeto Gasto con la información ingresada.
        val gasto = Gasto(
            descripcion = descripcion,
            monto = monto,
            categoria = categoria,
            fecha = fecha
        )

        // Ejecuta el registro del gasto mediante una tarea del ciclo de vida.
        lifecycleScope.launch {

            // Envía el gasto al servicio para realizar su validación y registro.
            val error = controlGastos.registrar(gasto)

            // Comprueba si ocurrió algún error durante el registro.
            if (error != null) {

                // Muestra el mensaje de error correspondiente.
                Toast.makeText(
                    this@MainActivity,
                    error,
                    Toast.LENGTH_LONG
                ).show()

            } else {

                // Limpia los campos después de registrar correctamente el gasto.
                etDescripcion.text.clear()
                etMonto.text.clear()

                // Informa que el gasto fue registrado correctamente.
                Toast.makeText(
                    this@MainActivity,
                    "Gasto registrado correctamente",
                    Toast.LENGTH_SHORT
                ).show()

                // Actualiza la lista de gastos y los valores del presupuesto.
                mostrarGastos()
            }
        }
    }

    // Mostrar gastos y actualizar el presupuesto.
    // Esta función obtiene los gastos y calcula el estado del dinero del usuario.
    private suspend fun mostrarGastos() {

        // Obtiene la lista de gastos registrados del usuario actual.
        val gastos = controlGastos.listar()

        // Calcula la suma total de todos los gastos registrados.
        val total = controlGastos.total()

        // Obtener el sueldo guardado.
        // Consulta el sueldo mensual en un hilo secundario.
        val sueldo = withContext(Dispatchers.IO) {
            repositorioSueldo.obtenerSueldo()
        }

        // Obtener el mes actual.
        // Genera el año y el mes actual con el formato correspondiente.
        val mesActual = SimpleDateFormat(
            "yyyy-MM",
            Locale.getDefault()
        ).format(Date())

        // Sumar únicamente los gastos del mes actual.
        // Filtra los gastos cuya fecha comienza con el año y mes actuales.
        // Después suma los montos de los gastos encontrados.
        val gastosMes = gastos
            .filter { it.fecha.startsWith(mesActual) }
            .sumOf { it.monto }

        // Calcular el dinero disponible.
        // Resta los gastos del mes actual al sueldo mensual.
        val disponible = sueldo - gastosMes

        // Mostrar sueldo, gastos mensuales y disponible.
        // Presenta el sueldo mensual con dos decimales.
        tvSueldo.text =
            "Sueldo mensual: Q %.2f".format(sueldo)

        // Presenta la suma de los gastos realizados durante el mes.
        tvGastosMes.text =
            "Gastos del mes: Q %.2f".format(gastosMes)

        // Presenta el dinero que queda disponible después de los gastos.
        tvDisponible.text =
            "Dinero disponible: Q %.2f".format(disponible)

        // Mostrar total de todos los gastos registrados.
        // Presenta la suma de todos los gastos del usuario.
        tvTotal.text =
            "Total de gastos: Q %.2f".format(total)

        // Mostrar la lista de gastos.
        // Comprueba si existen gastos registrados.
        if (gastos.isEmpty()) {

            // Muestra un mensaje cuando todavía no hay gastos.
            tvListaGastos.text =
                "Todavía no hay gastos registrados"

            // Desactiva la acción de presionar la lista.
            tvListaGastos.setOnClickListener(null)

            return
        }

        // Organiza los gastos en una cadena de texto.
        // Muestra la descripción, el monto, la categoría y la fecha.
        val lista = gastos.joinToString("\n\n") { gasto ->
            "${gasto.descripcion}\n" +
                    "Monto: Q %.2f\n".format(gasto.monto) +
                    "Categoría: ${gasto.categoria}\n" +
                    "Fecha: ${gasto.fecha}\n" +
                    "Toca aquí para eliminar este gasto"
        }

        // Muestra la lista de gastos en la pantalla.
        tvListaGastos.text = lista

        // Establece una acción al presionar el listado de gastos.
        tvListaGastos.setOnClickListener {

            // Crea una lista con las descripciones y los montos de los gastos.
            // Estas opciones aparecerán en un cuadro de selección.
            val opciones = gastos.map { gasto ->
                "${gasto.descripcion} - Q %.2f".format(gasto.monto)
            }.toTypedArray()

            // Muestra un cuadro para seleccionar el gasto que se desea eliminar.
            AlertDialog.Builder(this@MainActivity)
                .setTitle("Selecciona un gasto para eliminar")
                .setItems(opciones) { _, posicion ->

                    // Obtiene el gasto seleccionado por el usuario.
                    val gastoSeleccionado = gastos[posicion]

                    // Muestra un segundo cuadro para confirmar la eliminación.
                    AlertDialog.Builder(this@MainActivity)
                        .setTitle("Confirmar eliminación")
                        .setMessage(
                            "¿Deseas eliminar el gasto " +
                                    "\"${gastoSeleccionado.descripcion}\"?"
                        )

                        // Permite cancelar la eliminación.
                        .setNegativeButton("Cancelar", null)

                        // Ejecuta la eliminación si el usuario confirma.
                        .setPositiveButton("Eliminar") { _, _ ->

                            // Ejecuta la operación de eliminación.
                            lifecycleScope.launch {

                                // Envía el ID del gasto al servicio para eliminarlo.
                                val eliminado = controlGastos.eliminar(
                                    gastoSeleccionado.id
                                )

                                // Comprueba si el gasto se eliminó correctamente.
                                if (eliminado) {

                                    // Informa que el gasto fue eliminado.
                                    Toast.makeText(
                                        this@MainActivity,
                                        "Gasto eliminado correctamente",
                                        Toast.LENGTH_SHORT
                                    ).show()

                                } else {

                                    // Informa que no fue posible eliminar el gasto.
                                    Toast.makeText(
                                        this@MainActivity,
                                        "No se pudo eliminar el gasto",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }

                                // Actualizar presupuesto después de eliminar.
                                // Vuelve a consultar los gastos y actualizar los totales.
                                mostrarGastos()
                            }
                        }
                        .show()
                }

                // Permite cerrar el cuadro sin seleccionar un gasto.
                .setNegativeButton("Cancelar", null)
                .show()
        }
    }
}
