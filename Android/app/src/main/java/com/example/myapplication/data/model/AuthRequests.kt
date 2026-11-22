package com.example.myapplication.data.model

data class ForgotPasswordRequest(val correo: String)

data class ResetPasswordRequest(val token: String, val nuevaPassword: String)
