package com.example.myapplication.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.myapplication.data.model.*
import com.example.myapplication.data.repository.AppRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportesScreen(
    sesion: UsuarioSesion,
    repository: AppRepository,
    abrirNuevoAlEntrar: Boolean = false,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val rol = sesion.rol.uppercase().removePrefix("ROLE_")
    val puedeCrear = rol == "DOCENTE" || rol == "ADMIN"
    val puedeAtender = rol == "ENCARGADO" || rol == "ADMIN"

    var reportes by remember { mutableStateOf<List<Reporte>>(emptyList()) }
    var cargando by remember { mutableStateOf(false) }
    var filtroEstado by remember { mutableStateOf("TODOS") }

    var mostrarCrear by remember { mutableStateOf(abrirNuevoAlEntrar && puedeCrear) }
    var reporteAtender by remember { mutableStateOf<Reporte?>(null) }

    fun recargar() {
        scope.launch {
            cargando = true
            try {
                val todos = repository.listarReportes(sesion.token)
                reportes = if (rol == "DOCENTE") todos.filter { it.docenteId == sesion.id } else todos
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Error al cargar reportes: ${e.message}")
            } finally {
                cargando = false
            }
        }
    }

    LaunchedEffect(Unit) { recargar() }

    val estadosFiltro = listOf("TODOS", "POR_ATENDER", "EN_PROCESO", "ATENDIDO", "RECHAZADO")
    val filtrados = remember(reportes, filtroEstado) {
        if (filtroEstado == "TODOS") reportes
        else reportes.filter { it.estado.equals(filtroEstado, true) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (rol == "DOCENTE") "Mis Reportes" else "Reportes de Daños", fontWeight = FontWeight.Bold) },
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
            if (puedeCrear) {
                ExtendedFloatingActionButton(
                    onClick = { mostrarCrear = true },
                    icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                    text = { Text("Reportar daño") }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(estadosFiltro) { est ->
                    FilterChip(
                        selected = filtroEstado == est,
                        onClick = { filtroEstado = est },
                        label = { Text(est.replace("_", " ")) }
                    )
                }
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when {
                    cargando -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    filtrados.isEmpty() -> Text(
                        "No hay reportes en este estado",
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    else -> {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(filtrados, key = { it.id }) { rep ->
                                ReporteCard(
                                    reporte = rep,
                                    puedeAtender = puedeAtender,
                                    onAtender = { reporteAtender = rep }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // ==================== DIÁLOGO CREAR REPORTE (CASCADA) ====================
    if (mostrarCrear) {
        CrearReporteDialog(
            token = sesion.token,
            repository = repository,
            onDismiss = { mostrarCrear = false },
            onCreado = {
                mostrarCrear = false
                recargar()
                scope.launch { snackbarHostState.showSnackbar("Reporte registrado (POR ATENDER)") }
            },
            onError = { msg ->
                scope.launch { snackbarHostState.showSnackbar("Error: $msg") }
            }
        )
    }

    // ==================== DIÁLOGO ATENDER / CAMBIAR ESTADO ====================
    reporteAtender?.let { rep ->
        var estadoSel by remember(rep) { mutableStateOf(rep.estado) }
        val opciones = listOf("POR_ATENDER", "EN_PROCESO", "ATENDIDO", "RECHAZADO")

        AlertDialog(
            onDismissRequest = { reporteAtender = null },
            title = { Text("Atender ${rep.codigoTicket}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(rep.titulo, fontWeight = FontWeight.SemiBold)
                    Text(rep.descripcion, style = MaterialTheme.typography.bodySmall)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text("Selecciona nuevo estado:", fontWeight = FontWeight.Medium)
                    opciones.forEach { est ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { estadoSel = est }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = estadoSel == est, onClick = { estadoSel = est })
                            Spacer(Modifier.width(8.dp))
                            Text(est.replace("_", " "))
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val targetId = rep.id
                    reporteAtender = null
                    scope.launch {
                        try {
                            if (estadoSel == "EN_PROCESO") {
                                runCatching { repository.asignarEncargado(targetId, sesion.id, sesion.token) }
                            }
                            repository.actualizarEstadoReporte(targetId, estadoSel, sesion.token)
                            recargar()
                            snackbarHostState.showSnackbar("Estado actualizado a ${estadoSel.replace("_", " ")}")
                        } catch (e: Exception) {
                            snackbarHostState.showSnackbar("Error: ${e.message}")
                        }
                    }
                }) { Text("Guardar estado") }
            },
            dismissButton = {
                TextButton(onClick = { reporteAtender = null }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun ReporteCard(
    reporte: Reporte,
    puedeAtender: Boolean,
    onAtender: () -> Unit
) {
    val colorEstado = when (reporte.estado.uppercase()) {
        "POR_ATENDER" -> Color(0xFFD97706)
        "EN_PROCESO" -> Color(0xFF2563EB)
        "ATENDIDO" -> Color(0xFF16A34A)
        else -> Color(0xFFDC2626)
    }
    val colorPrioridad = when (reporte.prioridad.uppercase()) {
        "URGENTE" -> Color(0xFFDC2626)
        "ALTA" -> Color(0xFFEA580C)
        "MEDIA" -> Color(0xFF2563EB)
        else -> Color(0xFF64748B)
    }

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(reporte.codigoTicket, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(color = colorPrioridad.copy(alpha = 0.14f), shape = RoundedCornerShape(8.dp)) {
                        Text(
                            reporte.prioridad,
                            color = colorPrioridad,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Surface(color = colorEstado.copy(alpha = 0.14f), shape = RoundedCornerShape(8.dp)) {
                        Text(
                            reporte.estado.replace("_", " "),
                            color = colorEstado,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            Text(reporte.titulo, fontWeight = FontWeight.SemiBold)
            Text(
                reporte.descripcion,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "Ambiente ID: #${reporte.ambienteId}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (puedeAtender) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    FilledTonalButton(onClick = onAtender) {
                        Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Atender / Estado")
                    }
                }
            }
        }
    }
}

@Composable
private fun CrearReporteDialog(
    token: String,
    repository: AppRepository,
    onDismiss: () -> Unit,
    onCreado: () -> Unit,
    onError: (String) -> Unit
) {
    val scope = rememberCoroutineScope()

    var edificios by remember { mutableStateOf<List<Edificio>>(emptyList()) }
    var pisos by remember { mutableStateOf<List<Piso>>(emptyList()) }
    var ambientes by remember { mutableStateOf<List<Ambiente>>(emptyList()) }

    var edSel by remember { mutableStateOf<Edificio?>(null) }
    var pisoSel by remember { mutableStateOf<Piso?>(null) }
    var ambSel by remember { mutableStateOf<Ambiente?>(null) }

    var titulo by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var prioridad by remember { mutableStateOf("MEDIA") }
    var enviando by remember { mutableStateOf(false) }

    var expEd by remember { mutableStateOf(false) }
    var expPiso by remember { mutableStateOf(false) }
    var expAmb by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        runCatching { repository.listarEdificios(token) }
            .onSuccess { edificios = it.filter { ed -> ed.activo } }
            .onFailure { onError(it.message ?: "Error al cargar edificios") }
    }

    AlertDialog(
        onDismissRequest = { if (!enviando) onDismiss() },
        title = { Text("Nuevo Reporte de Daño") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // 1. Edificio
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = edSel?.let { "${it.codigo} · ${it.nombre}" } ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("1. Edificio") },
                        trailingIcon = { Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(modifier = Modifier.matchParentSize().clickable { expEd = true })
                    DropdownMenu(expanded = expEd, onDismissRequest = { expEd = false }) {
                        edificios.forEach { ed ->
                            DropdownMenuItem(
                                text = { Text("${ed.codigo} · ${ed.nombre}") },
                                onClick = {
                                    edSel = ed
                                    pisoSel = null
                                    ambSel = null
                                    pisos = emptyList()
                                    ambientes = emptyList()
                                    expEd = false
                                    ed.id?.let { edId ->
                                        scope.launch {
                                            runCatching { repository.listarPisos(edId, token) }
                                                .onSuccess { pisos = it.filter { p -> p.activo } }
                                        }
                                    }
                                }
                            )
                        }
                    }
                }

                // 2. Piso
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = pisoSel?.nombre ?: "",
                        onValueChange = {},
                        readOnly = true,
                        enabled = edSel != null,
                        label = { Text("2. Piso") },
                        trailingIcon = { Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (edSel != null) {
                        Box(modifier = Modifier.matchParentSize().clickable { expPiso = true })
                    }
                    DropdownMenu(expanded = expPiso, onDismissRequest = { expPiso = false }) {
                        pisos.forEach { p ->
                            DropdownMenuItem(
                                text = { Text(p.nombre) },
                                onClick = {
                                    pisoSel = p
                                    ambSel = null
                                    ambientes = emptyList()
                                    expPiso = false
                                    p.id?.let { pId ->
                                        scope.launch {
                                            runCatching { repository.listarAmbientes(pId, token) }
                                                .onSuccess { ambientes = it.filter { a -> a.activo } }
                                        }
                                    }
                                }
                            )
                        }
                    }
                }

                // 3. Ambiente
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = ambSel?.nombre ?: "",
                        onValueChange = {},
                        readOnly = true,
                        enabled = pisoSel != null,
                        label = { Text("3. Ambiente / Aula") },
                        trailingIcon = { Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (pisoSel != null) {
                        Box(modifier = Modifier.matchParentSize().clickable { expAmb = true })
                    }
                    DropdownMenu(expanded = expAmb, onDismissRequest = { expAmb = false }) {
                        ambientes.forEach { a ->
                            DropdownMenuItem(
                                text = { Text("${a.nombre} (${a.tipo})") },
                                onClick = {
                                    ambSel = a
                                    expAmb = false
                                }
                            )
                        }
                    }
                }

                // 4. Urgencia
                Text("Urgencia:", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("BAJA", "MEDIA", "ALTA", "URGENTE").forEach { p ->
                        FilterChip(
                            selected = prioridad == p,
                            onClick = { prioridad = p },
                            label = { Text(p, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                // 5. Detalle
                OutlinedTextField(
                    value = titulo,
                    onValueChange = { titulo = it },
                    label = { Text("Título del daño") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = descripcion,
                    onValueChange = { descripcion = it },
                    label = { Text("Descripción del problema") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                enabled = !enviando && ambSel?.id != null && titulo.trim().length >= 4 && descripcion.trim().length >= 8,
                onClick = {
                    enviando = true
                    scope.launch {
                        try {
                            repository.crearReporte(
                                CrearReporteRequest(
                                    ambienteId = ambSel!!.id!!,
                                    titulo = titulo.trim(),
                                    descripcion = descripcion.trim(),
                                    prioridad = prioridad,
                                    estado = "POR_ATENDER"
                                ),
                                token
                            )
                            onCreado()
                        } catch (e: Exception) {
                            enviando = false
                            onError(e.message ?: "No se pudo registrar el reporte")
                        }
                    }
                }
            ) { Text(if (enviando) "Enviando..." else "Enviar reporte") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !enviando) { Text("Cancelar") }
        }
    )
}
