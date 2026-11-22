package com.example.myapplication.data.model

data class AuthResponse(
    val token: String,
    val tipo: String,
    val usuarioId: Int,
    val correo: String,
    val rol: String
)

data class LoginRequest(val correo: String, val password: String)

data class Edificio(
    val id: Int? = null,
    val codigo: String,
    val nombre: String,
    val descripcion: String? = null,
    val activo: Boolean = true
)

data class Piso(
    val id: Int? = null,
    val edificioId: Int,
    val numeroPiso: Int,
    val nombre: String,
    val activo: Boolean = true
)

data class Ambiente(
    val id: Int? = null,
    val pisoId: Int,
    val tipo: String,
    val especialidad: String? = null,
    val codigo: String? = null,
    val nombre: String,
    val activo: Boolean = true
)

data class Reporte(
    val id: Int,
    val codigoTicket: String,
    val ambienteId: Int,
    val docenteId: Int,
    val titulo: String,
    val descripcion: String,
    val fotoUrl: String? = null,
    val prioridad: String = "MEDIA",
    val estado: String = "POR_ATENDER",
    val observacionAtencion: String? = null
)

data class CrearReporteRequest(
    val ambienteId: Int,
    val titulo: String,
    val descripcion: String,
    val prioridad: String = "MEDIA",
    val estado: String = "POR_ATENDER"
)

data class UsuarioSesion(
    val id: Int,
    val correo: String,
    val nombreCompleto: String,
    val rol: String,
    val token: String
)

data class EdificioRequest(
    val codigo: String,
    val nombre: String,
    val descripcion: String? = null
)

data class PisoRequest(
    val edificioId: Int,
    val numeroPiso: Int,
    val nombre: String
)

data class AmbienteRequest(
    val pisoId: Int,
    val tipo: String,
    val especialidad: String? = null,
    val codigo: String? = null,
    val nombre: String
)

data class UsuarioRequest(
    val nombres: String,
    val apellidos: String,
    val dni: String,
    val correo: String,
    val password: String,
    val telefono: String?,
    val rolId: Int
)

data class UsuarioResponse(
    val id: Int,
    val nombres: String,
    val apellidos: String,
    val nombreCompleto: String,
    val dni: String,
    val correo: String,
    val telefono: String?,
    val activo: Boolean,
    val rolId: Int,
    val rolNombre: String
)