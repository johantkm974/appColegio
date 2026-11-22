package com.example.myapplication.data.api

import com.example.myapplication.data.model.*
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Part
import okhttp3.MultipartBody

interface ApiService {

    // ==================== AUTH ====================
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): AuthResponse

    @POST("api/auth/forgot-password")
    suspend fun forgotPassword(@Body request: ForgotPasswordRequest): String

    @POST("api/auth/reset-password")
    suspend fun resetPassword(@Body request: ResetPasswordRequest): Unit

    // ==================== EDIFICIOS ====================
    @GET("api/edificios")
    suspend fun listarEdificios(@Header("Authorization") token: String): List<Edificio>

    @POST("api/edificios")
    suspend fun crearEdificio(@Header("Authorization") token: String, @Body request: EdificioRequest): Edificio

    @PUT("api/edificios/{id}")
    suspend fun actualizarEdificio(@Header("Authorization") token: String, @Path("id") id: Int, @Body request: EdificioRequest): Edificio

    @DELETE("api/edificios/{id}")
    suspend fun eliminarEdificio(@Header("Authorization") token: String, @Path("id") id: Int): Unit

    // ==================== PISOS ====================
    @GET("api/pisos/edificio/{edificioId}")
    suspend fun listarPisos(@Path("edificioId") edificioId: Int, @Header("Authorization") token: String): List<Piso>

    @POST("api/pisos")
    suspend fun crearPiso(@Header("Authorization") token: String, @Body request: PisoRequest): Piso

    @PUT("api/pisos/{id}")
    suspend fun actualizarPiso(@Header("Authorization") token: String, @Path("id") id: Int, @Body request: PisoRequest): Piso

    @DELETE("api/pisos/{id}")
    suspend fun eliminarPiso(@Header("Authorization") token: String, @Path("id") id: Int): Unit

    // ==================== AMBIENTES ====================
    @GET("api/ambientes/piso/{pisoId}")
    suspend fun listarAmbientes(@Path("pisoId") pisoId: Int, @Header("Authorization") token: String): List<Ambiente>

    @POST("api/ambientes")
    suspend fun crearAmbiente(@Header("Authorization") token: String, @Body request: AmbienteRequest): Ambiente

    @PUT("api/ambientes/{id}")
    suspend fun actualizarAmbiente(@Header("Authorization") token: String, @Path("id") id: Int, @Body request: AmbienteRequest): Ambiente

    @DELETE("api/ambientes/{id}")
    suspend fun eliminarAmbiente(@Header("Authorization") token: String, @Path("id") id: Int): Unit

    // ==================== REPORTES ====================
    @GET("api/reportes")
    suspend fun listarReportes(@Header("Authorization") token: String): List<Reporte>

    @POST("api/reportes")
    suspend fun crearReporte(@Header("Authorization") token: String, @Body request: CrearReporteRequest): Reporte

    @PATCH("api/reportes/{id}/estado")
    suspend fun actualizarEstadoReporte(@Header("Authorization") token: String, @Path("id") id: Int, @Body request: Map<String, String>): Reporte

    @PATCH("api/reportes/{id}/encargado/{encargadoId}")
    suspend fun asignarEncargado(@Header("Authorization") token: String, @Path("id") id: Int, @Path("encargadoId") encargadoId: Int): Reporte

    @Multipart
    @POST("api/reportes/{id}/foto")
    suspend fun subirFotoReporte(@Header("Authorization") token: String, @Path("id") id: Int, @Part foto: MultipartBody.Part): Reporte

    // ==================== USUARIOS ====================
    @GET("api/usuarios")
    suspend fun listarUsuarios(@Header("Authorization") token: String): List<UsuarioResponse>

    @POST("api/usuarios")
    suspend fun crearUsuario(@Header("Authorization") token: String, @Body request: UsuarioRequest): UsuarioResponse

    @PUT("api/usuarios/{id}")
    suspend fun actualizarUsuario(@Header("Authorization") token: String, @Path("id") id: Int, @Body request: UsuarioRequest): UsuarioResponse

    @DELETE("api/usuarios/{id}")
    suspend fun eliminarUsuario(@Header("Authorization") token: String, @Path("id") id: Int): Unit

    @PATCH("api/usuarios/{id}/estado")
    suspend fun cambiarEstadoUsuario(@Header("Authorization") token: String, @Path("id") id: Int, @Body request: Map<String, Boolean>): UsuarioResponse

    @PATCH("api/usuarios/{id}/password")
    suspend fun cambiarPasswordUsuario(@Header("Authorization") token: String, @Path("id") id: Int, @Body request: Map<String, String>): UsuarioResponse
}
