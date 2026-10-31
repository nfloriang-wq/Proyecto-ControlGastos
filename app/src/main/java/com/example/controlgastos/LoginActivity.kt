package com.example.controlgastos

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

// clase encargada de controlar la pantalla de inicio de sesión.
class LoginActivity : AppCompatActivity() {

    // es el campo donde se ingresa el nombre del usuario.
    private lateinit var etUsuario: EditText

    private lateinit var btnIniciarSesion: Button

    // gestiona el ingreso de los usuarios.
    private lateinit var authRepository: AuthRepository

    // función que se ejecuta cuando se abre la pantalla.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // conecta esta actividad con el diseño XML de inicio de sesión.
        setContentView(R.layout.activity_login)

        // inicializa el repositorio encargado de gestionar los usuarios.
        authRepository = AuthRepository(this)

        // Relaciona las variables con los elementos del diseño XML.
        etUsuario = findViewById(R.id.etUsuario)
        btnIniciarSesion = findViewById(R.id.btnIniciarSesion)

        // acción que se hara al presionar el botón.
        btnIniciarSesion.setOnClickListener {

            // Obtiene el nombre ingresado y elimina los espacios innecesarios.
            val usuario = etUsuario.text.toString().trim()

            // Comprueba que el usuario haya ingresado un nombre.
            if (usuario.isEmpty()) {

                // Muestra un mensaje de error debajo del campo de texto.
                etUsuario.error = "Ingresa tu nombre"

                // Detiene la ejecución si el campo está vacío.
                return@setOnClickListener
            }

            // Busca o registra al usuario utilizando el repositorio.
            val usuarioEncontrado =
                authRepository.ingresarPorNombre(usuario)

            // Comprueba si se encontró o creó correctamente el usuario.
            if (usuarioEncontrado != null) {

                // Abre las preferencias donde se guardará la información de la sesión.
                val preferencias = getSharedPreferences(
                    "ControlGastosSesion",
                    MODE_PRIVATE
                )

                // guardado de identificador y nombre del usuario.
                preferencias.edit()
                    .putInt("usuario_id", usuarioEncontrado.id)
                    .putString(
                        "nombre_usuario",
                        usuarioEncontrado.nombreUsuario
                    )
                    .apply()

                val intent = Intent(this, MainActivity::class.java)

                // Limpia las actividades anteriores y establece la pantalla principal
                // como la nueva actividad de inicio.
                intent.flags =
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TASK

                // Abre la pantalla principal de la aplicación.
                startActivity(intent)

            } else {

                // Muestra un mensaje si no fue posible ingresar.
                Toast.makeText(
                    this,
                    "No se pudo ingresar. Intenta nuevamente.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
