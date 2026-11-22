package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.myapplication.data.model.Reporte
import com.example.myapplication.data.model.UsuarioSesion
import com.example.myapplication.data.repository.AppRepository
import com.example.myapplication.ui.screens.GestionUsuariosScreen
import com.example.myapplication.ui.screens.InfraestructuraScreen
import com.example.myapplication.ui.screens.ReportesScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ReportaTheme {
                MyApp()
            }
        }
    }
}

@Composable
fun ReportaTheme(content: @Composable () -> Unit) {
    val colorScheme = lightColorScheme(
        primary = Color(0xFF1E5FBC),
        primaryContainer = Color(0xFFE3F2FD),
        secondary = Color(0xFF475569),
        background = Color(0xFFF8FAFC),
        surface = Color(0xFFFFFFFF),
        error = Color(0xFFDC2626)
    )
    MaterialTheme(colorScheme = colorScheme, content = content)
}

@Composable
fun MyApp() {
    val repository = remember { AppRepository() }
    val scope = rememberCoroutineScope()

    var screen by remember { mutableStateOf("login") }
    var sesion by remember { mutableStateOf<UsuarioSesion?>(null) }
    var abrirNuevoReporte by remember { mutableStateOf(false) }

    when (screen) {
        "login" -> LoginScreen(
            repository = repository,
            onLoginSuccess = { nuevaSesion ->
                sesion = nuevaSesion
                screen = "dashboard"
            }
        )

        "dashboard" -> sesion?.let { s ->
            DashboardScreen(
                sesion = s,
                repository = repository,
                onLogout = {
                    sesion = null
                    screen = "login"
                },
                onAbrirUsuarios = { screen = "usuarios" },
                onAbrirEdificios = { screen = "edificios" },
                onAbrirReportes = { crear ->
                    abrirNuevoReporte = crear
                    screen = "reportes"
                }
            )
        }

        "usuarios" -> sesion?.let { s ->
            GestionUsuariosScreen(
                token = s.token,
                adminIdActual = s.id,
                repository = repository,
                onBack = { screen = "dashboard" }
            )
        }

        "edificios" -> sesion?.let { s ->
            InfraestructuraScreen(
                token = s.token,
                repository = repository,
                onBack = { screen = "dashboard" }
            )
        }

        "reportes" -> sesion?.let { s ->
            ReportesScreen(
                sesion = s,
                repository = repository,
                abrirNuevoAlEntrar = abrirNuevoReporte,
                onBack = {
                    abrirNuevoReporte = false
                    screen = "dashboard"
                }
            )
        }
    }
}

@Composable
fun LoginScreen(
    repository: AppRepository,
    onLoginSuccess: (UsuarioSesion) -> Unit
) {
    val scope = rememberCoroutineScope()
    var correo by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var cargando by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.Build,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(64.dp)
        )
        Spacer(Modifier.height(12.dp))
        Text("REPORTA", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text("Sistema de Reporte de Daños", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(32.dp))

        OutlinedTextField(
            value = correo,
            onValueChange = { correo = it },
            label = { Text("Correo electrónico") },
            leadingIcon = { Icon(Icons.Rounded.Email, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Contraseña") },
            leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null) },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        error?.let {
            Spacer(Modifier.height(10.dp))
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.height(20.dp))
        Button(
            onClick = {
                cargando = true
                error = null
                scope.launch {
                    try {
                        val res = repository.login(correo.trim(), password)
                        onLoginSuccess(
                            UsuarioSesion(
                                id = res.usuarioId,
                                correo = res.correo,
                                nombreCompleto = res.correo,
                                rol = res.rol,
                                token = res.token
                            )
                        )
                    } catch (e: Exception) {
                        error = "Credenciales inválidas o sin conexión (${e.message})"
                    } finally {
                        cargando = false
                    }
                }
            },
            enabled = correo.isNotBlank() && password.isNotBlank() && !cargando,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            if (cargando) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
            else Text("Ingresar")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    sesion: UsuarioSesion,
    repository: AppRepository,
    onLogout: () -> Unit,
    onAbrirUsuarios: () -> Unit,
    onAbrirEdificios: () -> Unit,
    onAbrirReportes: (abrirCrear: Boolean) -> Unit
) {
    val rol = sesion.rol.uppercase().removePrefix("ROLE_")
    var reportes by remember { mutableStateOf<List<Reporte>>(emptyList()) }

    LaunchedEffect(Unit) {
        runCatching { repository.listarReportes(sesion.token) }
            .onSuccess { lista ->
                reportes = if (rol == "DOCENTE") lista.filter { it.docenteId == sesion.id } else lista
            }
    }

    val porAtender = reportes.count { it.estado.equals("POR_ATENDER", true) }
    val enProceso = reportes.count { it.estado.equals("EN_PROCESO", true) }
    val atendidos = reportes.count { it.estado.equals("ATENDIDO", true) }
    val urgentes = reportes.count { it.prioridad.equals("URGENTE", true) && !it.estado.equals("ATENDIDO", true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("REPORTA", fontWeight = FontWeight.Bold)
                        Text("${sesion.correo} · $rol", style = MaterialTheme.typography.bodySmall)
                    }
                },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Rounded.ExitToApp, contentDescription = "Cerrar sesión")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Resumen de Reportes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                KpiCard("Por atender", porAtender, Color(0xFFD97706), Modifier.weight(1f))
                KpiCard("En proceso", enProceso, Color(0xFF2563EB), Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                KpiCard("Atendidos", atendidos, Color(0xFF16A34A), Modifier.weight(1f))
                KpiCard("Urgentes", urgentes, Color(0xFFDC2626), Modifier.weight(1f))
            }

            Spacer(Modifier.height(8.dp))
            Text("Módulos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            if (rol == "DOCENTE" || rol == "ADMIN") {
                MenuCard("Reportar nuevo daño", "Elige edificio, piso, aula y urgencia", Icons.Rounded.Warning) {
                    onAbrirReportes(true)
                }
            }

            MenuCard(
                titulo = if (rol == "DOCENTE") "Mis Reportes" else "Atender Reportes",
                subtitulo = if (rol == "DOCENTE") "Consulta el estado de tus solicitudes" else "Filtra y actualiza estados de daños",
                icono = Icons.Rounded.List
            ) { onAbrirReportes(false) }

            if (rol == "ADMIN") {
                MenuCard("Edificios, Pisos y Ambientes", "CRUD completo de infraestructura y aulas", Icons.Rounded.Home, onAbrirEdificios)
                MenuCard("Gestión de Usuarios", "Crear, editar, roles y estado de cuentas", Icons.Rounded.Person, onAbrirUsuarios)
            }
        }
    }
}

@Composable
private fun KpiCard(titulo: String, valor: Int, color: Color, modifier: Modifier = Modifier) {
    ElevatedCard(modifier = modifier) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(valor.toString(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = color)
            Text(titulo, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun MenuCard(titulo: String, subtitulo: String, icono: ImageVector, onClick: () -> Unit) {
    ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icono, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(titulo, fontWeight = FontWeight.SemiBold)
                Text(subtitulo, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Rounded.KeyboardArrowRight, contentDescription = null)
        }
    }
}
