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

//  Colores
private val BlueDeep   = Color(0xFF1565C0)
private val BlueMid    = Color(0xFF1E88E5)
private val BlueLight  = Color(0xFFE3F2FD)
private val OrangeWarn = Color(0xFFEF6C00)
private val OrangePast = Color(0xFFFFF3E0)
private val GreenOk    = Color(0xFF2E7D32)
private val GreenPast  = Color(0xFFE8F5E9)
private val White      = Color(0xFFFFFFFF)
private val GrayBg     = Color(0xFFF5F7FA)
private val GrayText   = Color(0xFC78909C)
private val TextDark   = Color(0xFF1A2332)
private val BorderGray = Color(0xFFE0E0E0)

// SolicitudScreen
// Se muestra cuando el producto no tiene stock y el cliente quiere solicitarlo.
// Parámetros:
//   producto    – el ProductoFirestore con stock == 0
//   onBack      – navegar atrás
//   onSuccess   – solicitud enviada correctamente
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SolicitudScreen(
    producto: ProductoFirestore,
    onBack: () -> Unit,
    onSuccess: () -> Unit
) {
    val scope       = rememberCoroutineScope()
    val currentUser = FirebaseAuth.getInstance().currentUser

    var cantidad    by remember { mutableIntStateOf(1) }
    var cantidadText by remember { mutableStateOf("1") }
    var nota        by remember { mutableStateOf("") }
    var isLoading   by remember { mutableStateOf(false) }
    var errorMsg    by remember { mutableStateOf<String?>(null) }
    var successDone by remember { mutableStateOf(false) }

    // Cargamos datos del usuario para incluirlos en la solicitud
    var nombreUsuario by remember { mutableStateOf("") }
    var emailUsuario  by remember { mutableStateOf(currentUser?.email ?: "") }

    LaunchedEffect(Unit) {
        currentUser?.let { user ->
            val u = FirestoreRepository.getUsuario(user.uid)
            nombreUsuario = u?.nombre ?: user.displayName ?: ""
            emailUsuario  = u?.email ?: user.email ?: ""
        }
    }

    if (successDone) {
        //  Pantalla de éxito
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(GrayBg),
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
                    color      = TextDark,
                    textAlign  = TextAlign.Center
                )
                Text(
                    "El administrador revisará tu solicitud en los próximos 7 días. " +
                            "Te notificaremos aquí mismo cuando haya una respuesta.",
                    fontSize  = 14.sp,
                    color     = GrayText,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
                Spacer(Modifier.height(8.dp))
                // Info de plazos
                Card(
                    shape  = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = BlueLight)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        InfoRow("📋", "Tu solicitud está pendiente de revisión")
                        InfoRow("⏰", "Plazo de respuesta: 7 días")
                        InfoRow("🔔", "Revisa tus notificaciones para ver actualizaciones")
                        InfoRow("💳", "Si es aceptada, deberás confirmar tu pago")
                    }
                }
                Button(
                    onClick = onSuccess,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape  = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BlueDeep)
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
                    Text(
                        "Solicitar producto",
                        fontWeight = FontWeight.Bold,
                        fontSize   = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = White)
            )
        },
        containerColor = GrayBg
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            //  Banner "Sin stock"
            Card(
                shape  = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = OrangePast)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("⚠️", fontSize = 28.sp)
                    Column {
                        Text(
                            "Producto sin stock",
                            fontSize   = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color      = OrangeWarn
                        )
                        Text(
                            "Puedes solicitarlo. El admin verificará si puede conseguirlo.",
                            fontSize = 12.sp,
                            color    = Color(0xFF8D4E00),
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            //  Tarjeta del producto
            Card(
                shape  = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(BlueLight, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) { Text("🦷", fontSize = 30.sp) }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            producto.nombre,
                            fontSize   = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color      = TextDark
                        )
                        Text(
                            producto.categoria,
                            fontSize = 12.sp,
                            color    = GrayText
                        )
                        Text(
                            "Precio referencial: Bs. ${producto.precio}",
                            fontSize   = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color      = BlueMid
                        )
                    }
                }
            }

            //  Cantidad
            Card(
                shape  = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Cantidad solicitada",
                            fontSize   = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color      = TextDark
                        )
                        Text(
                            "¿Cuántas unidades necesitas?",
                            fontSize = 12.sp,
                            color    = GrayText
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
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
                                containerColor = BlueLight
                            )
                        ) {
                            Text("−", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = BlueDeep)
                        }

                        // Campo editable por teclado — límite 1000
                        OutlinedTextField(
                            value         = cantidadText,
                            onValueChange = { v: String ->
                                if (v.length <= 4 && v.all { ch -> ch.isDigit() }) {
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
                                color      = TextDark
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape           = RoundedCornerShape(10.dp),
                            colors          = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor   = BlueMid,
                                unfocusedBorderColor = BorderGray
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
                                containerColor         = BlueLight,
                                disabledContainerColor = Color(0xFFEEEEEE)
                            )
                        ) {
                            Text(
                                "+",
                                fontSize   = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color      = if (cantidad >= 1000) GrayText else BlueDeep
                            )
                        }
                    }
                }
            }

            //  Nota opcional
            Card(
                shape  = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Nota adicional (opcional)",
                        fontSize   = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = TextDark
                    )
                    Text(
                        "Puedes especificar marca, modelo, urgencia u otros detalles.",
                        fontSize = 12.sp,
                        color    = GrayText
                    )
                    OutlinedTextField(
                        value         = nota,
                        onValueChange = { if (it.length <= 300) nota = it },
                        placeholder   = {
                            Text(
                                "Ej: Necesito la marca X, es urgente para el consultorio...",
                                fontSize = 13.sp
                            )
                        },
                        modifier  = Modifier.fillMaxWidth(),
                        shape     = RoundedCornerShape(10.dp),
                        minLines  = 3,
                        maxLines  = 5,
                        colors    = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = BlueMid,
                            unfocusedBorderColor = BorderGray
                        )
                    )
                    Text(
                        "${nota.length}/300",
                        fontSize = 11.sp,
                        color    = GrayText,
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }

            //  Info de plazos
            Card(
                shape  = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = BlueLight)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        "¿Cómo funciona?",
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color      = BlueDeep
                    )
                    InfoRow("1️⃣", "Envías la solicitud con la cantidad y nota")
                    InfoRow("2️⃣", "El admin tiene 7 días para responder")
                    InfoRow("3️⃣", "Si es aceptada, recibirás instrucciones de pago")
                    InfoRow("4️⃣", "Realizas el pago y notificas desde la app")
                    InfoRow("5️⃣", "El admin confirma y tu pedido queda completado")
                }
            }

            //  Error
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

            //  Botón enviar
            Button(
                onClick = {
                    val uid = currentUser?.uid
                    if (uid == null) {
                        errorMsg = "Debes iniciar sesión para enviar una solicitud."
                        return@Button
                    }
                    isLoading = true
                    errorMsg  = null
                    scope.launch {
                        val ok = SolicitudRepository.crearSolicitud(
                            usuarioId     = uid,
                            nombreUsuario = nombreUsuario,
                            emailUsuario  = emailUsuario,
                            producto      = producto,
                            cantidad      = cantidad,
                            nota          = nota.trim()
                        )
                        isLoading = false
                        if (ok) {
                            successDone = true
                        } else {
                            errorMsg = "No se pudo enviar la solicitud. Intenta de nuevo."
                        }
                    }
                },
                enabled  = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape  = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BlueDeep)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier    = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color       = White
                    )
                } else {
                    Text(
                        "Enviar solicitud",
                        fontSize   = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun InfoRow(emoji: String, text: String) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(emoji, fontSize = 14.sp)
        Text(
            text,
            fontSize   = 13.sp,
            color      = Color(0xFF1A2332),
            lineHeight = 19.sp,
            modifier   = Modifier.weight(1f)
        )
    }
}