package com.example.dentalpro.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

private val NavyDark    = Color(0xFF0D1B2A)
private val NavyMid     = Color(0xFF1B2B3D)
private val AccentBlue  = Color(0xFF2563EB)
private val GreenOk     = Color(0xFF16A34A)
private val GreenPastel = Color(0xFFDCFCE7)
private val RedDark     = Color(0xFFDC2626)
private val RedPastel   = Color(0xFFFFEBEE)
private val OrangeWarn  = Color(0xFFEA580C)
private val OrangePast  = Color(0xFFFFF3E0)
private val White       = Color(0xFFFFFFFF)
private val BgGray      = Color(0xFFF1F5F9)
private val TextGray    = Color(0xFF64748B)
private val BorderColor = Color(0xFFE2E8F0)
private val TextDark    = Color(0xFF0F172A)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUsersScreen(onBack: () -> Unit) {
    val scope      = rememberCoroutineScope()
    val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    var usuarios     by remember { mutableStateOf<List<UsuarioFirestore>>(emptyList()) }
    var filtered     by remember { mutableStateOf<List<UsuarioFirestore>>(emptyList()) }
    var searchQuery  by remember { mutableStateOf("") }
    var isLoading    by remember { mutableStateOf(true) }
    var selectedUser by remember { mutableStateOf<UsuarioFirestore?>(null) }
    var confirmUser  by remember { mutableStateOf<UsuarioFirestore?>(null) }
    var snackbarMsg  by remember { mutableStateOf<String?>(null) }
    val snackbarHost = remember { SnackbarHostState() }

    fun reload() {
        scope.launch {
            isLoading = true
            usuarios  = FirestoreRepository.getTodosLosUsuarios()
            filtered  = usuarios
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { reload() }

    LaunchedEffect(searchQuery) {
        filtered = if (searchQuery.isBlank()) usuarios
        else usuarios.filter {
            it.nombre.contains(searchQuery, ignoreCase = true) ||
                    it.email.contains(searchQuery, ignoreCase = true)
        }
    }

    LaunchedEffect(snackbarMsg) {
        snackbarMsg?.let { snackbarHost.showSnackbar(it); snackbarMsg = null }
    }

    // Diálogo confirmar cambio de rol
    confirmUser?.let { usuario ->
        val nuevoRol = if (usuario.rol == "admin") "cliente" else "admin"
        val accion   = if (nuevoRol == "admin") "promover a Administrador" else "quitar rol de Administrador"
        AlertDialog(
            onDismissRequest = { confirmUser = null },
            containerColor   = White,
            shape            = RoundedCornerShape(20.dp),
            title = { Text("Cambiar rol", fontWeight = FontWeight.Bold) },
            text  = { Text("Deseas $accion a ${usuario.nombre}?", fontSize = 14.sp, color = TextGray) },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            val ok = FirestoreRepository.cambiarRolUsuario(usuario.id, nuevoRol)
                            confirmUser  = null
                            selectedUser = null
                            snackbarMsg  = if (ok) "Rol actualizado" else "Error al cambiar rol"
                            if (ok) reload()
                        }
                    },
                    colors   = ButtonDefaults.buttonColors(
                        containerColor = if (nuevoRol == "admin") AccentBlue else OrangeWarn
                    ),
                    shape    = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (nuevoRol == "admin") "Promover a Admin" else "Quitar Admin", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick  = { confirmUser = null },
                    shape    = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Cancelar") }
            }
        )
    }

    // Pantalla detalle usuario (como en la imagen)
    if (selectedUser != null) {
        UserDetailSheet(
            usuario    = selectedUser!!,
            isSelf     = selectedUser!!.id == currentUid,
            onBack     = { selectedUser = null },
            onToggleRol = { confirmUser = selectedUser }
        )
        return
    }

    // Pantalla lista usuarios
    Scaffold(
        containerColor = BgGray,
        snackbarHost   = { SnackbarHost(snackbarHost) }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {

            // Header oscuro
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.linearGradient(listOf(NavyDark, NavyMid)))
                    .padding(horizontal = 4.dp, vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = White)
                    }
                    Text("Usuarios", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = White, modifier = Modifier.weight(1f))
                }
            }

            // Barra búsqueda
            Column(
                modifier = Modifier.fillMaxWidth().background(White).padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value         = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder   = { Text("Buscar usuarios...", fontSize = 13.sp, color = TextGray) },
                    leadingIcon   = { Icon(Icons.Default.Search, contentDescription = null, tint = TextGray, modifier = Modifier.size(18.dp)) },
                    modifier      = Modifier.fillMaxWidth().height(48.dp),
                    shape         = RoundedCornerShape(10.dp),
                    singleLine    = true,
                    colors        = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor      = AccentBlue,
                        unfocusedBorderColor    = BorderColor,
                        unfocusedContainerColor = BgGray,
                        focusedContainerColor   = White
                    )
                )
            }

            HorizontalDivider(color = BorderColor)

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AccentBlue)
                }
            } else {
                LazyColumn(
                    modifier       = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(filtered, key = { it.id }) { usuario ->
                        UserRow(
                            usuario   = usuario,
                            isSelf    = usuario.id == currentUid,
                            onClick   = { selectedUser = usuario }
                        )
                        HorizontalDivider(color = BorderColor, modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }
            }
        }
    }
}

// ── Fila de usuario en la lista ───────────────────────────────────────────────
@Composable
fun UserRow(
    usuario: UsuarioFirestore,
    isSelf: Boolean,
    onClick: () -> Unit
) {
    val isAdmin = usuario.rol == "admin"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .background(White)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(if (isAdmin) AccentBlue else Color(0xFF94A3B8)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                usuario.nombre.firstOrNull()?.uppercase() ?: "U",
                fontSize   = 18.sp,
                fontWeight = FontWeight.Bold,
                color      = White
            )
        }

        // Info
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    usuario.nombre,
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = TextDark,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis
                )
                if (isSelf) {
                    Box(
                        modifier = Modifier
                            .background(GreenPastel, RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text("Yo", fontSize = 9.sp, color = GreenOk, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Text(usuario.email, fontSize = 12.sp, color = TextGray, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }

        // Badge rol
        Box(
            modifier = Modifier
                .background(
                    if (isAdmin) GreenPastel else BgGray,
                    RoundedCornerShape(20.dp)
                )
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                if (isAdmin) "Admin" else "Cliente",
                fontSize   = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color      = if (isAdmin) GreenOk else TextGray
            )
        }
    }
}

// ── Pantalla detalle usuario (igual a la imagen) ──────────────────────────────
@Composable
fun UserDetailSheet(
    usuario: UsuarioFirestore,
    isSelf: Boolean,
    onBack: () -> Unit,
    onToggleRol: () -> Unit
) {
    val isAdmin    = usuario.rol == "admin"
    val fechaStr   = try {
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(usuario.fecha.toDate())
    } catch (e: Exception) { "N/A" }

    Column(
        modifier = Modifier.fillMaxSize().background(BgGray)
    ) {
        // Header oscuro
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(NavyDark, NavyMid)))
                .padding(horizontal = 4.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = White)
                }
                Text("Detalle de Usuario", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = White)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Tarjeta perfil
            Card(
                shape  = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Badge estado arriba
                    Box(
                        modifier = Modifier
                            .align(Alignment.End)
                            .background(GreenPastel, RoundedCornerShape(20.dp))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text("Activo", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = GreenOk)
                    }

                    // Avatar grande
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(if (isAdmin) AccentBlue else Color(0xFF94A3B8)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            usuario.nombre.firstOrNull()?.uppercase() ?: "U",
                            fontSize   = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color      = White
                        )
                    }

                    Text(usuario.nombre, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextDark)
                    Text(usuario.email, fontSize = 13.sp, color = TextGray)
                }
            }

            // Tarjeta info detallada
            Card(
                shape  = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    UserDetailRow(icon = "👤", label = "Rol", value = if (isAdmin) "Administrador" else "Cliente")
                    HorizontalDivider(color = BorderColor, modifier = Modifier.padding(vertical = 8.dp))
                    UserDetailRow(icon = "📅", label = "Fecha de registro", value = fechaStr)
                    if (usuario.telefono.isNotBlank()) {
                        HorizontalDivider(color = BorderColor, modifier = Modifier.padding(vertical = 8.dp))
                        UserDetailRow(icon = "📞", label = "Telefono", value = usuario.telefono)
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            // Botones acción
            if (!isSelf) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Button(
                        onClick  = onToggleRol,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape    = RoundedCornerShape(14.dp),
                        colors   = ButtonDefaults.buttonColors(
                            containerColor = if (isAdmin) OrangeWarn else AccentBlue
                        )
                    ) {
                        Text(
                            if (isAdmin) "Quitar rol Admin" else "Promover a Admin",
                            fontSize   = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                Card(
                    shape  = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = GreenPastel),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text("Esta es tu propia cuenta", fontSize = 13.sp, color = GreenOk, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

@Composable
fun UserDetailRow(icon: String, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(icon, fontSize = 18.sp)
        Column {
            Text(label, fontSize = 11.sp, color = TextGray)
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextDark)
        }
    }
}