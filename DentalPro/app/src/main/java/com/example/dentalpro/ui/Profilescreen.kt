package com.example.dentalpro.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

private val GreenDark    = Color(0xFF1B5E20)
private val GreenMid     = Color(0xFF2E7D32)
private val GreenPastel  = Color(0xFFE8F5E9)
private val White        = Color(0xFFFFFFFF)
private val GrayBg       = Color(0xFFF5F5F5)
private val GrayText     = Color(0xFF070707)
private val OrangeOff    = Color(0xFFFF6F00)
private val OrangePastel = Color(0xFFFFF3E0)
private val RedPastel    = Color(0xFFFFEBEE)
private val RedDark      = Color(0xFFE53935)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(onLogout: () -> Unit) {
    val scope       = rememberCoroutineScope()
    val auth        = FirebaseAuth.getInstance()
    val db          = Firebase.firestore
    val currentUser = auth.currentUser

    var usuario          by remember { mutableStateOf<UsuarioFirestore?>(null) }
    var pedidos          by remember { mutableStateOf<List<PedidoFirestore>>(emptyList()) }
    var isLoading        by remember { mutableStateOf(true) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showEditDialog   by remember { mutableStateOf(false) }
    var snackMsg         by remember { mutableStateOf<String?>(null) }
    val snackbarHost     = remember { SnackbarHostState() }

    fun reloadUser() {
        scope.launch {
            currentUser?.let { user ->
                usuario = FirestoreRepository.getUsuario(user.uid)
                pedidos = FirestoreRepository.getPedidosUsuario(user.uid)
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { reloadUser() }

    LaunchedEffect(snackMsg) {
        snackMsg?.let { snackbarHost.showSnackbar(it); snackMsg = null }
    }

    // ── Diálogo Cerrar Sesión ─────────────────────────────────────────────────
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Cerrar sesion") },
            text  = { Text("Estas seguro que deseas salir?") },
            confirmButton = {
                TextButton(onClick = {
                    auth.signOut()
                    showLogoutDialog = false
                    onLogout()
                }) { Text("Salir", color = RedDark) }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text("Cancelar") }
            }
        )
    }

    // ── Diálogo Editar Perfil ─────────────────────────────────────────────────
    if (showEditDialog && usuario != null) {
        EditProfileDialog(
            usuario   = usuario!!,
            onDismiss = { showEditDialog = false },
            onSave    = { nuevoNombre, nuevoTelefono ->
                scope.launch {
                    try {
                        val uid = currentUser?.uid ?: return@launch
                        db.collection("usuarios").document(uid)
                            .update(mapOf("nombre" to nuevoNombre, "telefono" to nuevoTelefono))
                            .addOnSuccessListener {
                                showEditDialog = false
                                snackMsg = "✅ Perfil actualizado correctamente"
                                scope.launch { reloadUser() }
                            }
                            .addOnFailureListener {
                                snackMsg = "Error al guardar los cambios"
                            }
                    } catch (e: Exception) {
                        snackMsg = "Error: ${e.message}"
                    }
                }
            }
        )
    }

    Scaffold(
        containerColor = GrayBg,
        snackbarHost   = { SnackbarHost(snackbarHost) },
        topBar = {
            TopAppBar(
                title = { Text("Mi Perfil", color = Color.Black, fontWeight = FontWeight.Bold) },
                actions = {
                    // Botón editar perfil
                    IconButton(onClick = { if (!isLoading) showEditDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar perfil", tint = GreenMid)
                    }
                    IconButton(onClick = { showLogoutDialog = true }) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Cerrar sesion", tint = RedDark)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = White)
            )
        }
    ) { innerPadding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = GreenMid)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Tarjeta usuario
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = White),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size(64.dp).background(GreenMid, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = usuario?.nombre?.firstOrNull()?.uppercase() ?: "U",
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = White
                                    )
                                }
                                Spacer(Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        usuario?.nombre ?: "Usuario",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF212121)
                                    )
                                    Text(
                                        usuario?.email ?: currentUser?.email ?: "",
                                        fontSize = 13.sp,
                                        color = GrayText
                                    )
                                    val tel = usuario?.telefono
                                    if (!tel.isNullOrBlank()) {
                                        Text(tel, fontSize = 13.sp, color = GrayText)
                                    } else {
                                        Text(
                                            "Sin teléfono  •  Toca ✏️ para agregar",
                                            fontSize = 12.sp,
                                            color = OrangeOff
                                        )
                                    }
                                }
                                // Botón editar inline
                                IconButton(onClick = { showEditDialog = true }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Editar", tint = GreenMid, modifier = Modifier.size(20.dp))
                                }
                            }
                            Spacer(Modifier.height(16.dp))
                            HorizontalDivider(color = Color(0xFFE0E0E0))
                            Spacer(Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                InfoChip("Pedidos", pedidos.size.toString())
                                VerticalDivider(modifier = Modifier.height(40.dp), color = Color(0xFFE0E0E0))
                                InfoChip("Gastado", "Bs. ${pedidos.sumOf { it.total }}")
                                VerticalDivider(modifier = Modifier.height(40.dp), color = Color(0xFFE0E0E0))
                                InfoChip("Rol", usuario?.rol?.replaceFirstChar { it.uppercase() } ?: "Cliente")
                            }
                        }
                    }
                }

                // Titulo historial
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Historial de pedidos", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color(0xFF212121))
                        if (pedidos.isNotEmpty()) {
                            Text("${pedidos.size} pedidos", fontSize = 12.sp, color = GrayText)
                        }
                    }
                }

                if (pedidos.isEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = White),
                            elevation = CardDefaults.cardElevation(1.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("📋", fontSize = 40.sp)
                                Text("Sin pedidos aún", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF212121))
                                Text("Tus compras aparecerán aquí", fontSize = 13.sp, color = GrayText)
                            }
                        }
                    }
                } else {
                    items(pedidos, key = { it.id }) { pedido ->
                        PedidoCard(pedido = pedido)
                    }
                }

                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

// ── Diálogo de edición de perfil ─────────────────────────────────────────────
@Composable
fun EditProfileDialog(
    usuario: UsuarioFirestore,
    onDismiss: () -> Unit,
    onSave: (nombre: String, telefono: String) -> Unit
) {
    var nombre    by remember { mutableStateOf(usuario.nombre) }
    var telefono  by remember { mutableStateOf(usuario.telefono) }
    var isSaving  by remember { mutableStateOf(false) }
    var errorMsg  by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape  = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFFFF))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Título
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier.size(40.dp).background(GreenPastel, CircleShape),
                        contentAlignment = Alignment.Center
                    ) { Text("✏️", fontSize = 20.sp) }
                    Column {
                        Text("Editar Perfil", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color(0xFF212121))
                        Text("Actualiza tu información personal", fontSize = 11.sp, color = GrayText)
                    }
                }

                HorizontalDivider(color = Color(0xFFE0E0E0))

                // Campo correo (solo lectura)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Correo electrónico", fontSize = 11.sp, color = GrayText, fontWeight = FontWeight.Medium)
                    OutlinedTextField(
                        value         = usuario.email,
                        onValueChange = {},
                        modifier      = Modifier.fillMaxWidth(),
                        shape         = RoundedCornerShape(10.dp),
                        singleLine    = true,
                        enabled       = false,
                        colors        = OutlinedTextFieldDefaults.colors(
                            disabledBorderColor       = Color(0xFFE0E0E0),
                            disabledTextColor         = GrayText,
                            disabledContainerColor    = Color(0xFFF5F5F5)
                        )
                    )
                    Text("El correo no se puede modificar", fontSize = 10.sp, color = GrayText)
                }

                // Campo nombre
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Nombre completo *", fontSize = 11.sp, color = GrayText, fontWeight = FontWeight.Medium)
                    OutlinedTextField(
                        value         = nombre,
                        onValueChange = { nombre = it; errorMsg = null },
                        placeholder   = { Text("Tu nombre completo", fontSize = 13.sp) },
                        modifier      = Modifier.fillMaxWidth(),
                        shape         = RoundedCornerShape(10.dp),
                        singleLine    = true,
                        colors        = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = GreenMid,
                            unfocusedBorderColor = Color(0xFFE0E0E0)
                        )
                    )
                }

                // Campo teléfono
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Teléfono / WhatsApp", fontSize = 11.sp, color = GrayText, fontWeight = FontWeight.Medium)
                    OutlinedTextField(
                        value         = telefono,
                        onValueChange = { telefono = it },
                        placeholder   = { Text("+591 70000000", fontSize = 13.sp) },
                        modifier      = Modifier.fillMaxWidth(),
                        shape         = RoundedCornerShape(10.dp),
                        singleLine    = true,
                        colors        = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = GreenMid,
                            unfocusedBorderColor = Color(0xFFE0E0E0)
                        )
                    )
                    Text("Agregar teléfono permite recibir notificaciones por WhatsApp", fontSize = 10.sp, color = OrangeOff)
                }

                // Error
                errorMsg?.let { msg ->
                    Text(
                        msg,
                        color    = RedDark,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(RedPastel, RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    )
                }

                // Botones
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick  = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape    = RoundedCornerShape(10.dp)
                    ) { Text("Cancelar") }

                    Button(
                        onClick = {
                            if (nombre.isBlank()) {
                                errorMsg = "El nombre no puede estar vacío"
                            } else {
                                isSaving = true
                                onSave(nombre.trim(), telefono.trim())
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape    = RoundedCornerShape(10.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = GreenMid),
                        enabled  = !isSaving
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = White)
                        } else {
                            Text("Guardar", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// ── Tarjeta de pedido ─────────────────────────────────────────────────────────
@Composable
private fun PedidoCard(pedido: PedidoFirestore) {
    var expanded by remember { mutableStateOf(false) }

    val estadoColor = when (pedido.estado) {
        "verificado" -> Color(0xFF1565C0)
        "completado" -> GreenMid
        else         -> OrangeOff
    }
    val estadoBg = when (pedido.estado) {
        "verificado" -> Color(0xFFE3F2FD)
        "completado" -> GreenPastel
        else         -> OrangePastel
    }

    val totalUnidades = pedido.items.sumOf { anyToLong(it["cantidad"]) }

    fun formatFecha(ts: com.google.firebase.Timestamp): String {
        return try {
            SimpleDateFormat("dd MMM yyyy  HH:mm", Locale.getDefault()).format(ts.toDate())
        } catch (e: Exception) { "" }
    }

    Card(
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Pedido #${pedido.id.take(8).uppercase()}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF212121))
                    Spacer(Modifier.height(2.dp))
                    Text(formatFecha(pedido.fecha), fontSize = 11.sp, color = GrayText)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier.background(estadoBg, RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(pedido.estado.replaceFirstChar { it.uppercase() }, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = estadoColor)
                    }
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = GrayText,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Productos siempre visibles
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                pedido.items.forEach { item ->
                    val nombre   = item["nombre"]?.toString() ?: "Producto"
                    val cantidad = anyToLong(item["cantidad"])
                    val precio   = anyToLong(item["precio"])

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("🦷", fontSize = 13.sp)
                            Text(nombre, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF212121), maxLines = 1)
                        }
                        Text("x$cantidad", fontSize = 12.sp, color = GrayText, fontWeight = FontWeight.Medium)
                    }
                }
            }

            // Fila inferior: pago + total
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.background(GreenPastel, RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                        Text("📱 QR", fontSize = 11.sp, color = GreenDark, fontWeight = FontWeight.Medium)
                    }
                    Box(modifier = Modifier.background(Color(0xFFF3E5F5), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                        Text("$totalUnidades ud${if (totalUnidades != 1L) "s" else ""}", fontSize = 11.sp, color = Color(0xFF7B1FA2), fontWeight = FontWeight.Medium)
                    }
                }
                Text("Bs. ${pedido.total}", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = GreenDark)
            }

            // Detalle expandible con precios
            AnimatedVisibility(visible = expanded, enter = expandVertically(), exit = shrinkVertically()) {
                Column(modifier = Modifier.fillMaxWidth().background(Color(0xFFF9FBF9))) {
                    HorizontalDivider(color = Color(0xFFE0E0E0))
                    Spacer(Modifier.height(10.dp))
                    Text("Detalle de precios", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = GrayText, modifier = Modifier.padding(horizontal = 16.dp))
                    Spacer(Modifier.height(8.dp))
                    pedido.items.forEach { item ->
                        val nombre   = item["nombre"]?.toString() ?: "Producto"
                        val cantidad = anyToLong(item["cantidad"])
                        val precio   = anyToLong(item["precio"])
                        val subtotal = precio * cantidad
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(nombre, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF212121))
                                Text("x$cantidad  •  Bs. $precio c/u", fontSize = 10.sp, color = GrayText)
                            }
                            Text("Bs. $subtotal", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = GreenDark)
                        }
                    }
                    HorizontalDivider(color = Color(0xFFE0E0E0), modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total pagado", fontSize = 13.sp, color = GrayText)
                        Text("Bs. ${pedido.total}", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = GreenDark)
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoChip(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = GreenDark)
        Text(label, fontSize = 11.sp, color = GrayText)
    }
}

private fun anyToLong(value: Any?): Long = when (value) {
    is Long   -> value
    is Int    -> value.toLong()
    is Double -> value.toLong()
    is String -> value.toLongOrNull() ?: 0L
    else      -> 0L
}