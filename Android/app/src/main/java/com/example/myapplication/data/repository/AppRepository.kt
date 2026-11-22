package com.example.myapplication.data.repository

import com.example.myapplication.data.api.ApiService
import com.example.myapplication.data.api.RetrofitClient
import com.example.myapplication.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class AppRepository {
    private val api: ApiService = RetrofitClient.api

    suspend fun login(correo: String, password: String): AuthResponse =
        withContext(Dispatchers.IO) { api.login(LoginRequest(correo, password)) }

    suspend fun forgotPassword(correo: String): String =
        withContext(Dispatchers.IO) { api.forgotPassword(ForgotPasswordRequest(correo)) }

    suspend fun resetPassword(token: String, nuevaPassword: String) =
        withContext(Dispatchers.IO) { api.resetPassword(ResetPasswordRequest(token, nuevaPassword)) }

    // EDIFICIOS
    suspend fun listarEdificios(token: String): List<Edificio> =
        withContext(Dispatchers.IO) { api.listarEdificios("Bearer $token") }

    suspend fun crearEdificio(request: EdificioRequest, token: String): Edificio =
        withContext(Dispatchers.IO) { api.crearEdificio("Bearer $token", request) }

    suspend fun actualizarEdificio(id: Int, request: EdificioRequest, token: String): Edificio =
        withContext(Dispatchers.IO) { api.actualizarEdificio("Bearer $token", id, request) }

    suspend fun eliminarEdificio(id: Int, token: String) =
        withContext(Dispatchers.IO) { api.eliminarEdificio("Bearer $token", id) }

    // PISOS
    suspend fun listarPisos(edificioId: Int, token: String): List<Piso> =
        withContext(Dispatchers.IO) { api.listarPisos(edificioId, "Bearer $token") }

    suspend fun crearPiso(request: PisoRequest, token: String): Piso =
        withContext(Dispatchers.IO) { api.crearPiso("Bearer $token", request) }

    suspend fun actualizarPiso(id: Int, request: PisoRequest, token: String): Piso =
        withContext(Dispatchers.IO) { api.actualizarPiso("Bearer $token", id, request) }

    suspend fun eliminarPiso(id: Int, token: String) =
        withContext(Dispatchers.IO) { api.eliminarPiso("Bearer $token", id) }

    // AMBIENTES
    suspend fun listarAmbientes(pisoId: Int, token: String): List<Ambiente> =
        withContext(Dispatchers.IO) { api.listarAmbientes(pisoId, "Bearer $token") }

    suspend fun crearAmbiente(request: AmbienteRequest, token: String): Ambiente =
        withContext(Dispatchers.IO) { api.crearAmbiente("Bearer $token", request) }

    suspend fun actualizarAmbiente(id: Int, request: AmbienteRequest, token: String): Ambiente =
        withContext(Dispatchers.IO) { api.actualizarAmbiente("Bearer $token", id, request) }

    suspend fun eliminarAmbiente(id: Int, token: String) =
        withContext(Dispatchers.IO) { api.eliminarAmbiente("Bearer $token", id) }

    // REPORTES
    suspend fun listarReportes(token: String): List<Reporte> =
        withContext(Dispatchers.IO) { api.listarReportes("Bearer $token") }

    suspend fun crearReporte(request: CrearReporteRequest, token: String): Reporte =
        withContext(Dispatchers.IO) { api.crearReporte("Bearer $token", request) }

    suspend fun actualizarEstadoReporte(id: Int, estado: String, token: String): Reporte =
        withContext(Dispatchers.IO) { api.actualizarEstadoReporte("Bearer $token", id, mapOf("estado" to estado)) }

    suspend fun asignarEncargado(id: Int, encargadoId: Int, token: String): Reporte =
        withContext(Dispatchers.IO) { api.asignarEncargado("Bearer $token", id, encargadoId) }

    suspend fun subirFotoReporte(id: Int, archivo: File, token: String): Reporte =
        withContext(Dispatchers.IO) {
            val reqFile = archivo.asRequestBody("image/*".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("foto", archivo.name, reqFile)
            api.subirFotoReporte("Bearer $token", id, body)
        }

    // USUARIOS
    suspend fun listarUsuarios(token: String): List<UsuarioResponse> =
        withContext(Dispatchers.IO) { api.listarUsuarios("Bearer $token") }

    suspend fun crearUsuario(request: UsuarioRequest, token: String): UsuarioResponse =
        withContext(Dispatchers.IO) { api.crearUsuario("Bearer $token", request) }

    suspend fun actualizarUsuario(id: Int, request: UsuarioRequest, token: String): UsuarioResponse =
        withContext(Dispatchers.IO) { api.actualizarUsuario("Bearer $token", id, request) }

    suspend fun eliminarUsuario(id: Int, token: String) =
        withContext(Dispatchers.IO) { api.eliminarUsuario("Bearer $token", id) }

    suspend fun cambiarEstadoUsuario(id: Int, activo: Boolean, token: String): UsuarioResponse =
        withContext(Dispatchers.IO) { api.cambiarEstadoUsuario("Bearer $token", id, mapOf("activo" to activo)) }

    suspend fun cambiarPasswordUsuario(id: Int, nuevaPassword: String, token: String): UsuarioResponse =
        withContext(Dispatchers.IO) { api.cambiarPasswordUsuario("Bearer $token", id, mapOf("nuevaPassword" to nuevaPassword)) }
}
