package com.example.dentalpro.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

private val PBlueDeep   = Color(0xFF1565C0)
private val PBlueMid    = Color(0xFF1E88E5)
private val PBlueLight  = Color(0xFFE3F2FD)
private val PWhite      = Color(0xFFFFFFFF)
private val PGrayBg     = Color(0xFFF5F7FA)
private val PGrayText   = Color(0xFF78909C)
private val PTextDark   = Color(0xFF1A2332)
private val PBorderGray = Color(0xFFE0E0E0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PedidoEspecialScreen(onBack: () -> Unit) {
    val scope       = rememberCoroutineScope()
    val currentUser = FirebaseAuth.getInstance().currentUser

    var nombreProducto by remember { mutableStateOf("") }
    var cantidad       by remember { mutableIntStateOf(1) }
    var cantidadText   by remember { mutableStateOf("1") }
    var isLoading      by remember { mutableStateOf(false) }
    var errorMsg       by remember { mutableStateOf<String?>(null) }
    var successDone    by remember { mutableStateOf(false) }

    var nombreUsuario by remember { mutableStateOf("") }
    var emailUsuario  by remember { mutableStateOf(currentUser?.email ?: "") }

    LaunchedEffect(Unit) {
        currentUser?.let { user ->
            val u = FirestoreRepository.getUsuario(user.uid)
            nombreUsuario = u?.nombre ?: user.displayName ?: ""
            emailUsuario  = u?.email  ?: user.email ?: ""
        }
    }

    // ── Pantalla de éxito ─────────────────────────────────────────────────────
    if (successDone) {
        Box(
            modifier         = Modifier.fillMaxSize().background(PGrayBg),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(32.dp)
            ) {
                Text("✅", fontSize = 72.sp)
                Text(
                    "¡Solicitud enviada!",
                    fontSize   = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color      = PTextDark,
                    textAlign  = TextAlign.Center
                )
                Text(
                    "El administrador revisará tu pedido especial en los próximos 7 días. " +
                            "Te notificaremos cuando haya una respuesta.",
                    fontSize   = 14.sp,
                    color      = PTextDark,
                    textAlign  = TextAlign.Center,
                    lineHeight = 22.sp
                )
                Spacer(Modifier.height(8.dp))
                Card(
                    shape  = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = PBlueLight)
                ) {
                    Column(
                        modifier            = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PedEspInfoRow("📋", "Solicitud pendiente de revisión")
                        PedEspInfoRow("⏰", "Plazo de respuesta: 7 días")
                        PedEspInfoRow("🔔", "Revisa tus notificaciones para ver actualizaciones")
                        PedEspInfoRow("💳", "Si es aceptada, recibirás instrucciones de pago")
                    }
                }
                Button(
                    onClick  = onBack,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape    = RoundedCornerShape(14.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = PBlueDeep)
                ) {
                    Text("Entendido", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Pedido especial", fontWeight = FontWeight.Bold, color = PTextDark, fontSize = 18.sp)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = PTextDark)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PWhite)
            )
        },
        containerColor = PGrayBg
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ── Banner explicativo ────────────────────────────────────────────
            Card(
                shape  = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = PBlueLight)
            ) {
                Row(
                    modifier              = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("📦", fontSize = 28.sp)
                    Column {
                        Text(
                            "¿No encuentras lo que buscas?",
                            fontSize   = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color      = PBlueDeep
                        )
                        Text(
                            "Dinos el nombre del producto y la cantidad. El admin te enviará el precio si puede conseguirlo.",
                            fontSize   = 12.sp,
                            color      = Color(0xFF1A3A6B),
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // ── Nombre del producto ───────────────────────────────────────────
            Card(
                shape     = RoundedCornerShape(14.dp),
                colors    = CardDefaults.cardColors(containerColor = PWhite),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(
                    modifier            = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Producto solicitado *",
                        fontSize   = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = PTextDark
                    )
                    OutlinedTextField(
                        value         = nombreProducto,
                        onValueChange = { if (it.length <= 100) nombreProducto = it },
                        textStyle = LocalTextStyle.current.copy(
                            color = PTextDark),
                        placeholder   = {
                            Text(
                                "Ej: Resina Z250, Fresas de diamante, etc.",
                                fontSize = 13.sp,
                                color = PTextDark
                            )
                        },
                        modifier   = Modifier.fillMaxWidth(),
                        shape      = RoundedCornerShape(10.dp),
                        singleLine = true,
                        colors     = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = PBlueMid,
                            unfocusedBorderColor = PBorderGray
                        )
                    )
                }
            }

            // ── Categoría ─────────────────────────────────────────────────────
            // (eliminado: el admin asignará categoría al aceptar)

            // ── Cantidad ──────────────────────────────────────────────────────
            Card(
                shape     = RoundedCornerShape(14.dp),
                colors    = CardDefaults.cardColors(containerColor = PWhite),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Row(
                    modifier              = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Cantidad",
                            fontSize   = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color      = PTextDark
                        )
                        Text("¿Cuántas unidades?", fontSize = 12.sp, color = PTextDark)
                    }
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FilledTonalIconButton(
                            onClick  = {
                                if (cantidad > 1) {
                                    cantidad--
                                    cantidadText = cantidad.toString()
                                }
                            },
                            modifier = Modifier.size(36.dp),
                            colors   = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = PBlueLight
                            )
                        ) {
                            Text("−", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = PBlueDeep)
                        }

                        OutlinedTextField(
                            value         = cantidadText,
                            onValueChange = { v ->
                                if (v.length <= 4 && v.all { it.isDigit() }) {
                                    cantidadText = v
                                    val parsed = v.toIntOrNull() ?: return@OutlinedTextField
                                    cantidad = parsed.coerceIn(1, 1000)
                                }
                            },
                            modifier        = Modifier.width(72.dp),
                            singleLine      = true,
                            textStyle       = LocalTextStyle.current.copy(
                                fontSize   = 18.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign  = TextAlign.Center,
                                color      = PTextDark
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape           = RoundedCornerShape(10.dp),
                            colors          = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor   = PBlueMid,
                                unfocusedBorderColor = PBorderGray
                            )
                        )

                        FilledTonalIconButton(
                            onClick  = {
                                if (cantidad < 1000) {
                                    cantidad++
                                    cantidadText = cantidad.toString()
                                }
                            },
                            enabled  = cantidad < 1000,
                            modifier = Modifier.size(36.dp),
                            colors   = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor         = PBlueLight,
                                disabledContainerColor = Color(0xFFEEEEEE)
                            )
                        ) {
                            Text(
                                "+",
                                fontSize   = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color      = if (cantidad >= 1000) PGrayText else PBlueDeep
                            )
                        }
                    }
                }
            }

            // ── Cómo funciona ─────────────────────────────────────────────────
            Card(
                shape  = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = PBlueLight)
            ) {
                Column(
                    modifier            = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        "¿Cómo funciona?",
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color      = PBlueDeep
                    )
                    PedEspInfoRow("1️⃣", "Escribes el nombre del producto y la cantidad")
                    PedEspInfoRow("2️⃣", "El admin tiene 7 días para responder")
                    PedEspInfoRow("3️⃣", "Si lo consigue, el admin te envía el precio e instrucciones de pago")
                    PedEspInfoRow("4️⃣", "Pagas y notificas desde la app")
                    PedEspInfoRow("5️⃣", "El admin confirma y tu pedido queda completado")
                }
            }

            // ── Error ─────────────────────────────────────────────────────────
            errorMsg?.let { msg ->
                Text(
                    msg,
                    color    = Color(0xFFE53935),
                    fontSize = 13.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFFEBEE), RoundedCornerShape(10.dp))
                        .padding(12.dp)
                )
            }

            // ── Botón enviar ──────────────────────────────────────────────────
            Button(
                onClick = {
                    if (nombreProducto.isBlank()) {
                        errorMsg = "Escribe el nombre del producto que necesitas."
                        return@Button
                    }
                    val uid = currentUser?.uid
                    if (uid == null) {
                        errorMsg = "Debes iniciar sesión para enviar una solicitud."
                        return@Button
                    }
                    isLoading = true
                    errorMsg  = null
                    scope.launch {
                        val productoVirtual = ProductoFirestore(
                            id          = "pedido_especial_${System.currentTimeMillis()}",
                            nombre      = nombreProducto.trim(),
                            categoria   = "Pedido especial",
                            descripcion = "",
                            precio      = 0L,
                            stock       = 0L
                        )
                        val ok = SolicitudRepository.crearSolicitud(
                            usuarioId     = uid,
                            nombreUsuario = nombreUsuario,
                            emailUsuario  = emailUsuario,
                            producto      = productoVirtual,
                            cantidad      = cantidad,
                            nota          = ""
                        )
                        isLoading = false
                        if (ok) successDone = true
                        else errorMsg = "No se pudo enviar la solicitud. Intenta de nuevo."
                    }
                },
                enabled  = !isLoading,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = PBlueDeep)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier    = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color       = PWhite
                    )
                } else {
                    Text("Enviar solicitud", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun PedEspInfoRow(emoji: String, text: String) {
    Row(
        verticalAlignment     = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(emoji, fontSize = 14.sp)
        Text(
            text,
            fontSize   = 13.sp,
            color      = PTextDark,
            lineHeight = 19.sp,
            modifier   = Modifier.weight(1f)
        )
    }
}