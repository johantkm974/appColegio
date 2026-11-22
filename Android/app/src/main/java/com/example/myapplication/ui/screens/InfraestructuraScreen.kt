package com.example.myapplication.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import com.example.myapplication.data.model.*
import com.example.myapplication.data.repository.AppRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfraestructuraScreen(
    token: String,
    repository: AppRepository,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Navegación interna de 3 niveles: Edificios -> Pisos -> Ambientes
    var edificioSel by remember { mutableStateOf<Edificio?>(null) }
    var pisoSel by remember { mutableStateOf<Piso?>(null) }

    var edificios by remember { mutableStateOf<List<Edificio>>(emptyList()) }
    var pisos by remember { mutableStateOf<List<Piso>>(emptyList()) }
    var ambientes by remember { mutableStateOf<List<Ambiente>>(emptyList()) }
    var cargando by remember { mutableStateOf(false) }

    // Diálogos Crear / Editar
    var mostrarDialogoEdificio by remember { mutableStateOf(false) }
    var edificioEditando by remember { mutableStateOf<Edificio?>(null) }

    var mostrarDialogoPiso by remember { mutableStateOf(false) }
    var pisoEditando by remember { mutableStateOf<Piso?>(null) }

    var mostrarDialogoAmbiente by remember { mutableStateOf(false) }
    var ambienteEditando by remember { mutableStateOf<Ambiente?>(null) }

    // Diálogo de confirmación de eliminación
    var tituloEliminar by remember { mutableStateOf<String?>(null) }
    var accionEliminar by remember { mutableStateOf<(() -> Unit)?>(null) }

    fun recargarEdificios() {
        scope.launch {
            cargando = true
            try {
                edificios = repository.listarEdificios(token)
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Error al cargar edificios: ${e.message}")
            } finally {
                cargando = false
            }
        }
    }

    fun recargarPisos(edificioId: Int) {
        scope.launch {
            cargando = true
            try {
                pisos = repository.listarPisos(edificioId, token)
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Error al cargar pisos: ${e.message}")
            } finally {
                cargando = false
            }
        }
    }

    fun recargarAmbientes(pisoId: Int) {
        scope.launch {
            cargando = true
            try {
                ambientes = repository.listarAmbientes(pisoId, token)
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Error al cargar ambientes: ${e.message}")
            } finally {
                cargando = false
            }
        }
    }

    LaunchedEffect(Unit) { recargarEdificios() }
    LaunchedEffect(edificioSel) { edificioSel?.id?.let { recargarPisos(it) } }
    LaunchedEffect(pisoSel) { pisoSel?.id?.let { recargarAmbientes(it) } }

    val tituloBarra = when {
        pisoSel != null -> "${edificioSel?.codigo} · ${pisoSel?.nombre}"
        edificioSel != null -> "${edificioSel?.codigo} · ${edificioSel?.nombre}"
        else -> "Edificios"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(tituloBarra, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        when {
                            pisoSel != null -> pisoSel = null
                            edificioSel != null -> edificioSel = null
                            else -> onBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        when {
                            pisoSel != null -> pisoSel?.id?.let { recargarAmbientes(it) }
                            edificioSel != null -> edificioSel?.id?.let { recargarPisos(it) }
                            else -> recargarEdificios()
                        }
                    }) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "Actualizar")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    when {
                        pisoSel != null -> {
                            ambienteEditando = null
                            mostrarDialogoAmbiente = true
                        }
                        edificioSel != null -> {
                            pisoEditando = null
                            mostrarDialogoPiso = true
                        }
                        else -> {
                            edificioEditando = null
                            mostrarDialogoEdificio = true
                        }
                    }
                }
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Agregar")
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (cargando) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                when {
                    // ==================== NIVEL 3: AMBIENTES ====================
                    pisoSel != null -> {
                        if (ambientes.isEmpty()) {
                            Text(
                                "No hay ambientes en este piso. Pulsa + para agregar.",
                                modifier = Modifier.align(Alignment.Center).padding(24.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(ambientes, key = { it.id ?: it.hashCode() }) { amb ->
                                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = if (amb.tipo.equals("TALLER", true)) Icons.Rounded.Build else Icons.Rounded.Place,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(amb.nombre, fontWeight = FontWeight.SemiBold)
                                                val sub = buildString {
                                                    append(amb.tipo)
                                                    if (!amb.especialidad.isNullOrBlank()) append(" · ${amb.especialidad}")
                                                    append(" · Código: ${amb.codigo ?: "Sin asignar"}")
                                                }
                                                Text(
                                                    sub,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            IconButton(onClick = {
                                                ambienteEditando = amb
                                                mostrarDialogoAmbiente = true
                                            }) {
                                                Icon(Icons.Rounded.Edit, contentDescription = "Editar")
                                            }
                                            IconButton(onClick = {
                                                tituloEliminar = "ambiente \"${amb.nombre}\""
                                                accionEliminar = {
                                                    scope.launch {
                                                        try {
                                                            amb.id?.let { repository.eliminarAmbiente(it, token) }
                                                            pisoSel?.id?.let { recargarAmbientes(it) }
                                                            snackbarHostState.showSnackbar("Ambiente eliminado")
                                                        } catch (e: Exception) {
                                                            snackbarHostState.showSnackbar("Error: ${e.message}")
                                                        }
                                                    }
                                                }
                                            }) {
                                                Icon(
                                                    Icons.Rounded.Delete,
                                                    contentDescription = "Eliminar",
                                                    tint = MaterialTheme.colorScheme.error
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ==================== NIVEL 2: PISOS ====================
                    edificioSel != null -> {
                        if (pisos.isEmpty()) {
                            Text(
                                "No hay pisos registrados. Pulsa + para agregar.",
                                modifier = Modifier.align(Alignment.Center).padding(24.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(pisos, key = { it.id ?: it.hashCode() }) { piso ->
                                    ElevatedCard(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { pisoSel = piso }
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Rounded.List,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(piso.nombre, fontWeight = FontWeight.SemiBold)
                                                Text(
                                                    "Piso #${piso.numeroPiso} · Toca para ver ambientes",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            IconButton(onClick = {
                                                pisoEditando = piso
                                                mostrarDialogoPiso = true
                                            }) {
                                                Icon(Icons.Rounded.Edit, contentDescription = "Editar")
                                            }
                                            IconButton(onClick = {
                                                tituloEliminar = "piso \"${piso.nombre}\""
                                                accionEliminar = {
                                                    scope.launch {
                                                        try {
                                                            piso.id?.let { repository.eliminarPiso(it, token) }
                                                            edificioSel?.id?.let { recargarPisos(it) }
                                                            snackbarHostState.showSnackbar("Piso eliminado")
                                                        } catch (e: Exception) {
                                                            snackbarHostState.showSnackbar("Error: ${e.message}")
                                                        }
                                                    }
                                                }
                                            }) {
                                                Icon(
                                                    Icons.Rounded.Delete,
                                                    contentDescription = "Eliminar",
                                                    tint = MaterialTheme.colorScheme.error
                                                )
                                            }
                                            Icon(Icons.Rounded.KeyboardArrowRight, contentDescription = null)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ==================== NIVEL 1: EDIFICIOS ====================
                    else -> {
                        if (edificios.isEmpty()) {
                            Text(
                                "No hay edificios registrados. Pulsa + para agregar.",
                                modifier = Modifier.align(Alignment.Center).padding(24.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(edificios, key = { it.id ?: it.hashCode() }) { ed ->
                                    ElevatedCard(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { edificioSel = ed }
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Rounded.Home,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("${ed.codigo} · ${ed.nombre}", fontWeight = FontWeight.SemiBold)
                                                if (!ed.descripcion.isNullOrBlank()) {
                                                    Text(
                                                        ed.descripcion,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                            IconButton(onClick = {
                                                edificioEditando = ed
                                                mostrarDialogoEdificio = true
                                            }) {
                                                Icon(Icons.Rounded.Edit, contentDescription = "Editar")
                                            }
                                            IconButton(onClick = {
                                                tituloEliminar = "edificio \"${ed.nombre}\""
                                                accionEliminar = {
                                                    scope.launch {
                                                        try {
                                                            ed.id?.let { repository.eliminarEdificio(it, token) }
                                                            recargarEdificios()
                                                            snackbarHostState.showSnackbar("Edificio eliminado")
                                                        } catch (e: Exception) {
                                                            snackbarHostState.showSnackbar("Error: ${e.message}")
                                                        }
                                                    }
                                                }
                                            }) {
                                                Icon(
                                                    Icons.Rounded.Delete,
                                                    contentDescription = "Eliminar",
                                                    tint = MaterialTheme.colorScheme.error
                                                )
                                            }
                                            Icon(Icons.Rounded.KeyboardArrowRight, contentDescription = null)
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

    // ==================== DIÁLOGO EDIFICIO ====================
    if (mostrarDialogoEdificio) {
        var codigo by remember(edificioEditando) { mutableStateOf(edificioEditando?.codigo ?: "") }
        var nombre by remember(edificioEditando) { mutableStateOf(edificioEditando?.nombre ?: "") }
        var descripcion by remember(edificioEditando) { mutableStateOf(edificioEditando?.descripcion ?: "") }

        AlertDialog(
            onDismissRequest = { mostrarDialogoEdificio = false },
            title = { Text(if (edificioEditando == null) "Nuevo Edificio" else "Editar Edificio") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = codigo,
                        onValueChange = { codigo = it },
                        label = { Text("Código (ej. ED-11)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = nombre,
                        onValueChange = { nombre = it },
                        label = { Text("Nombre (ej. Edificio 11)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = descripcion,
                        onValueChange = { descripcion = it },
                        label = { Text("Descripción (opcional)") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = codigo.isNotBlank() && nombre.isNotBlank(),
                    onClick = {
                        val req = EdificioRequest(
                            codigo = codigo.trim().uppercase(),
                            nombre = nombre.trim(),
                            descripcion = descripcion.trim().ifBlank { null }
                        )
                        mostrarDialogoEdificio = false
                        scope.launch {
                            try {
                                val actual = edificioEditando
                                if (actual?.id == null) {
                                    repository.crearEdificio(req, token)
                                    snackbarHostState.showSnackbar("Edificio creado")
                                } else {
                                    repository.actualizarEdificio(actual.id, req, token)
                                    snackbarHostState.showSnackbar("Edificio actualizado")
                                }
                                recargarEdificios()
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("Error al guardar: ${e.message}")
                            }
                        }
                    }
                ) { Text("Guardar") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoEdificio = false }) { Text("Cancelar") }
            }
        )
    }

    // ==================== DIÁLOGO PISO ====================
    if (mostrarDialogoPiso && edificioSel?.id != null) {
        var numeroTxt by remember(pisoEditando) { mutableStateOf(pisoEditando?.numeroPiso?.toString() ?: "") }
        var nombre by remember(pisoEditando) { mutableStateOf(pisoEditando?.nombre ?: "") }

        AlertDialog(
            onDismissRequest = { mostrarDialogoPiso = false },
            title = { Text(if (pisoEditando == null) "Nuevo Piso" else "Editar Piso") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = numeroTxt,
                        onValueChange = {
                            if (it.all { ch -> ch.isDigit() }) {
                                numeroTxt = it
                                if (pisoEditando == null && it.isNotBlank()) nombre = "Piso $it"
                            }
                        },
                        label = { Text("Número de piso (ej. 1)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = nombre,
                        onValueChange = { nombre = it },
                        label = { Text("Nombre (ej. Piso 1)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = (numeroTxt.toIntOrNull() ?: 0) > 0 && nombre.isNotBlank(),
                    onClick = {
                        val edId = edificioSel!!.id!!
                        val req = PisoRequest(edId, numeroTxt.toInt(), nombre.trim())
                        mostrarDialogoPiso = false
                        scope.launch {
                            try {
                                val actual = pisoEditando
                                if (actual?.id == null) {
                                    repository.crearPiso(req, token)
                                    snackbarHostState.showSnackbar("Piso creado")
                                } else {
                                    repository.actualizarPiso(actual.id, req, token)
                                    snackbarHostState.showSnackbar("Piso actualizado")
                                }
                                recargarPisos(edId)
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("Error al guardar piso: ${e.message}")
                            }
                        }
                    }
                ) { Text("Guardar") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoPiso = false }) { Text("Cancelar") }
            }
        )
    }

    // ==================== DIÁLOGO AMBIENTE ====================
    if (mostrarDialogoAmbiente && pisoSel?.id != null) {
        val tipos = listOf("AULA", "TALLER", "INVESTIGACION", "LABORATORIO", "AIP", "BANO", "SALA", "ADMIN", "DEPOSITO", "BIBLIOTECA", "TOPICO", "MAESTRANZA", "CONECTIVIDAD")
        var tipo by remember(ambienteEditando) { mutableStateOf(ambienteEditando?.tipo ?: "AULA") }
        var especialidad by remember(ambienteEditando) { mutableStateOf(ambienteEditando?.especialidad ?: "") }
        var codigo by remember(ambienteEditando) { mutableStateOf(ambienteEditando?.codigo ?: "") }
        var nombre by remember(ambienteEditando) { mutableStateOf(ambienteEditando?.nombre ?: "") }
        var expandirTipo by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { mostrarDialogoAmbiente = false },
            title = { Text(if (ambienteEditando == null) "Nuevo Ambiente" else "Editar Ambiente") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = tipo,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Tipo") },
                            trailingIcon = { Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { expandirTipo = true }
                        )
                        DropdownMenu(
                            expanded = expandirTipo,
                            onDismissRequest = { expandirTipo = false }
                        ) {
                            tipos.forEach { opcion ->
                                DropdownMenuItem(
                                    text = { Text(opcion) },
                                    onClick = {
                                        tipo = opcion
                                        expandirTipo = false
                                    }
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = especialidad,
                        onValueChange = { especialidad = it },
                        label = { Text("Especialidad (ej. Metales, Primaria, CyT)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = codigo,
                        onValueChange = { codigo = it },
                        label = { Text("Código (ej. 17, T1 o vacío si aún no tiene)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = nombre,
                        onValueChange = { nombre = it },
                        label = { Text("Nombre visible (ej. Taller de Metales T1)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = tipo.isNotBlank() && nombre.isNotBlank(),
                    onClick = {
                        val pId = pisoSel!!.id!!
                        val req = AmbienteRequest(
                            pisoId = pId,
                            tipo = tipo.trim().uppercase(),
                            especialidad = especialidad.trim().ifBlank { null },
                            codigo = codigo.trim().ifBlank { null },
                            nombre = nombre.trim()
                        )
                        mostrarDialogoAmbiente = false
                        scope.launch {
                            try {
                                val actual = ambienteEditando
                                if (actual?.id == null) {
                                    repository.crearAmbiente(req, token)
                                    snackbarHostState.showSnackbar("Ambiente creado")
                                } else {
                                    repository.actualizarAmbiente(actual.id, req, token)
                                    snackbarHostState.showSnackbar("Ambiente actualizado")
                                }
                                recargarAmbientes(pId)
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("Error al guardar ambiente: ${e.message}")
                            }
                        }
                    }
                ) { Text("Guardar") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoAmbiente = false }) { Text("Cancelar") }
            }
        )
    }

    // ==================== CONFIRMACIÓN ELIMINAR ====================
    if (tituloEliminar != null) {
        AlertDialog(
            onDismissRequest = { tituloEliminar = null },
            title = { Text("Confirmar eliminación") },
            text = { Text("¿Seguro que deseas eliminar el $tituloEliminar?") },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        val run = accionEliminar
                        tituloEliminar = null
                        accionEliminar = null
                        run?.invoke()
                    }
                ) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { tituloEliminar = null }) { Text("Cancelar") }
            }
        )
    }
}
