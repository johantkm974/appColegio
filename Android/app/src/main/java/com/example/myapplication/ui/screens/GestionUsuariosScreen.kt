package com.example.myapplication.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.myapplication.data.model.UsuarioRequest
import com.example.myapplication.data.model.UsuarioResponse
import com.example.myapplication.data.repository.AppRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GestionUsuariosScreen(
    token: String,
    adminIdActual: Int,
    repository: AppRepository,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var usuarios by remember { mutableStateOf<List<UsuarioResponse>>(emptyList()) }
    var cargando by remember { mutableStateOf(false) }
    var filtroTexto by remember { mutableStateOf("") }

    var mostrarDialogoForm by remember { mutableStateOf(false) }
    var usuarioEditando by remember { mutableStateOf<UsuarioResponse?>(null) }
    var usuarioPassword by remember { mutableStateOf<UsuarioResponse?>(null) }
    var usuarioEliminar by remember { mutableStateOf<UsuarioResponse?>(null) }

    fun recargar() {
        scope.launch {
            cargando = true
            try {
                usuarios = repository.listarUsuarios(token)
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Error al cargar usuarios: ${e.message}")
            } finally {
                cargando = false
            }
        }
    }

    LaunchedEffect(Unit) { recargar() }

    val filtrados = remember(usuarios, filtroTexto) {
        if (filtroTexto.isBlank()) usuarios
        else usuarios.filter {
            it.nombreCompleto.contains(filtroTexto, true) ||
                it.correo.contains(filtroTexto, true) ||
                it.dni.contains(filtroTexto, true) ||
                it.rolNombre.contains(filtroTexto, true)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gestión de Usuarios", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { recargar() }) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "Actualizar")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                usuarioEditando = null
                mostrarDialogoForm = true
            }) {
                Icon(Icons.Rounded.Add, contentDescription = "Nuevo usuario")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            OutlinedTextField(
                value = filtroTexto,
                onValueChange = { filtroTexto = it },
                placeholder = { Text("Buscar por nombre, DNI, correo o rol...") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            )

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when {
                    cargando -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    filtrados.isEmpty() -> Text(
                        "No se encontraron usuarios",
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    else -> {
                        LazyColumn(
                            contentPadding = PaddingValues(vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(filtrados, key = { it.id }) { u ->
                                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Rounded.Person,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(u.nombreCompleto, fontWeight = FontWeight.SemiBold)
                                                Text(
                                                    "${u.rolNombre} · DNI ${u.dni}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Text(
                                                    u.correo,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Switch(
                                                checked = u.activo,
                                                enabled = u.id != adminIdActual,
                                                onCheckedChange = { nuevoActivo ->
                                                    scope.launch {
                                                        try {
                                                            repository.cambiarEstadoUsuario(u.id, nuevoActivo, token)
                                                            recargar()
                                                        } catch (e: Exception) {
                                                            snackbarHostState.showSnackbar("Error: ${e.message}")
                                                        }
                                                    }
                                                }
                                            )
                                        }
                                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            TextButton(onClick = { usuarioPassword = u }) {
                                                Icon(Icons.Rounded.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text("Clave")
                                            }
                                            TextButton(onClick = {
                                                usuarioEditando = u
                                                mostrarDialogoForm = true
                                            }) {
                                                Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text("Editar")
                                            }
                                            if (u.id != adminIdActual) {
                                                TextButton(
                                                    onClick = { usuarioEliminar = u },
                                                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                                ) {
                                                    Icon(Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                                                    Spacer(Modifier.width(4.dp))
                                                    Text("Eliminar")
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ==================== DIÁLOGO CREAR / EDITAR USUARIO ====================
    if (mostrarDialogoForm) {
        val roles = listOf(1 to "ADMIN", 2 to "ENCARGADO", 3 to "DOCENTE")
        var nombres by remember(usuarioEditando) { mutableStateOf(usuarioEditando?.nombres ?: "") }
        var apellidos by remember(usuarioEditando) { mutableStateOf(usuarioEditando?.apellidos ?: "") }
        var dni by remember(usuarioEditando) { mutableStateOf(usuarioEditando?.dni ?: "") }
        var correo by remember(usuarioEditando) { mutableStateOf(usuarioEditando?.correo ?: "") }
        var telefono by remember(usuarioEditando) { mutableStateOf(usuarioEditando?.telefono ?: "") }
        var password by remember(usuarioEditando) { mutableStateOf("") }
        var rolId by remember(usuarioEditando) { mutableStateOf(usuarioEditando?.rolId ?: 3) }
        var expandirRol by remember { mutableStateOf(false) }

        val esNuevo = usuarioEditando == null
        val rolTexto = roles.firstOrNull { it.first == rolId }?.second ?: "DOCENTE"
        val valido = nombres.isNotBlank() &&
            apellidos.isNotBlank() &&
            dni.length == 8 &&
            correo.contains("@") &&
            (!esNuevo || password.length >= 6)

        AlertDialog(
            onDismissRequest = { mostrarDialogoForm = false },
            title = { Text(if (esNuevo) "Crear Usuario" else "Editar Usuario") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = nombres, onValueChange = { nombres = it },
                        label = { Text("Nombres") }, singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = apellidos, onValueChange = { apellidos = it },
                        label = { Text("Apellidos") }, singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = dni,
                        onValueChange = { if (it.length <= 8 && it.all(Char::isDigit)) dni = it },
                        label = { Text("DNI (8 dígitos)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = correo, onValueChange = { correo = it },
                        label = { Text("Correo institucional") }, singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = telefono, onValueChange = { telefono = it },
                        label = { Text("Teléfono (opcional)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (esNuevo) {
                        OutlinedTextField(
                            value = password, onValueChange = { password = it },
                            label = { Text("Contraseña (mín. 6 caracteres)") },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = rolTexto,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Rol") },
                            trailingIcon = { Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Box(modifier = Modifier.matchParentSize().clickable { expandirRol = true })
                        DropdownMenu(expanded = expandirRol, onDismissRequest = { expandirRol = false }) {
                            roles.forEach { (id, nombreRol) ->
                                DropdownMenuItem(
                                    text = { Text(nombreRol) },
                                    onClick = {
                                        rolId = id
                                        expandirRol = false
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = valido,
                    onClick = {
                        val req = UsuarioRequest(
                            nombres = nombres.trim(),
                            apellidos = apellidos.trim(),
                            dni = dni.trim(),
                            correo = correo.trim(),
                            password = password,
                            telefono = telefono.trim().ifBlank { null },
                            rolId = rolId
                        )
                        mostrarDialogoForm = false
                        scope.launch {
                            try {
                                val actual = usuarioEditando
                                if (actual == null) {
                                    repository.crearUsuario(req, token)
                                    snackbarHostState.showSnackbar("Usuario creado")
                                } else {
                                    repository.actualizarUsuario(actual.id, req, token)
                                    snackbarHostState.showSnackbar("Usuario actualizado")
                                }
                                recargar()
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("Error: ${e.message}")
                            }
                        }
                    }
                ) { Text("Guardar") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoForm = false }) { Text("Cancelar") }
            }
        )
    }

    // ==================== DIÁLOGO CAMBIAR CONTRASEÑA ====================
    usuarioPassword?.let { u ->
        var nuevaClave by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { usuarioPassword = null },
            title = { Text("Cambiar contraseña") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Usuario: ${u.nombreCompleto}", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = nuevaClave,
                        onValueChange = { nuevaClave = it },
                        label = { Text("Nueva contraseña (mín. 6)") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = nuevaClave.length >= 6,
                    onClick = {
                        val targetId = u.id
                        usuarioPassword = null
                        scope.launch {
                            try {
                                repository.cambiarPasswordUsuario(targetId, nuevaClave, token)
                                snackbarHostState.showSnackbar("Contraseña actualizada")
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("Error: ${e.message}")
                            }
                        }
                    }
                ) { Text("Actualizar") }
            },
            dismissButton = {
                TextButton(onClick = { usuarioPassword = null }) { Text("Cancelar") }
            }
        )
    }

    // ==================== CONFIRMACIÓN ELIMINAR ====================
    usuarioEliminar?.let { u ->
        AlertDialog(
            onDismissRequest = { usuarioEliminar = null },
            title = { Text("Eliminar usuario") },
            text = { Text("¿Seguro que deseas eliminar a ${u.nombreCompleto}?") },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        val id = u.id
                        usuarioEliminar = null
                        scope.launch {
                            try {
                                repository.eliminarUsuario(id, token)
                                recargar()
                                snackbarHostState.showSnackbar("Usuario eliminado")
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("Error: ${e.message}")
                            }
                        }
                    }
                ) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { usuarioEliminar = null }) { Text("Cancelar") }
            }
        )
    }
}
