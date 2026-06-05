package com.example.dentalpro.ui

import android.R
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ── Colores ───────────────────────────────────────────────────────────────────
private val NavyDark    = Color(0xFF0D1B2A)
private val NavyMid     = Color(0xFF1B2B3D)
private val AccentBlue  = Color(0xFF2563EB)
private val GreenOk     = Color(0xFF16A34A)
private val GreenPastel = Color(0xFFDCFCE7)
private val GreenDark   = Color(0xFF14532D)
private val RedDark     = Color(0xFFDC2626)
private val RedPastel   = Color(0xFFFFEBEE)
private val OrangeWarn  = Color(0xFFEA580C)
private val OrangePast  = Color(0xFFFFF3E0)
private val PurpleDark  = Color(0xFF6A1B9A)
private val PurplePast  = Color(0xFFF3E5F5)
private val BluePast    = Color(0xFFEFF6FF)
private val GrayDark    = Color(0xFF374151)
private val GrayPastel  = Color(0xFFF1F5F9)
private val White       = Color(0xFFFFFFFF)
private val TextGray    = Color(0xFF64748B)
private val BorderColor = Color(0xFFE2E8F0)
private val TextDark    = Color(0xFF0F172A)

// ── Filtros disponibles ───────────────────────────────────────────────────────
private val FILTROS = listOf("Todas", "Pendiente", "Pagar", "Verificando", "Completado", "Rechazado", "Vencido")

// ─────────────────────────────────────────────────────────────────────────────
// AdminSolicitudesScreen
// Panel del administrador para gestionar solicitudes de productos sin stock.
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminSolicitudesScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()

    var solicitudes   by remember { mutableStateOf<List<SolicitudFirestore>>(emptyList()) }
    var isLoading     by remember { mutableStateOf(true) }
    var filtroActivo  by remember { mutableStateOf("Todas") }
    val snackbarHost  = remember { SnackbarHostState() }

    // Diálogos
    var aceptarTarget  by remember { mutableStateOf<SolicitudFirestore?>(null) }
    var rechazarTarget by remember { mutableStateOf<SolicitudFirestore?>(null) }
    var confirmarPago  by remember { mutableStateOf<SolicitudFirestore?>(null) }
    var notaAdmin      by remember { mutableStateOf("") }
    var precioAdmin    by remember { mutableStateOf("") }
    var motivoRechazo  by remember { mutableStateOf("") }

    val filtradas = remember(solicitudes, filtroActivo) {
        if (filtroActivo == "Todas") solicitudes
        else solicitudes.filter { sol ->
            estadoVisual(sol.estado).label == filtroActivo
        }
    }

    fun reload() {
        scope.launch {
            isLoading = true
            SolicitudRepository.vencerSolicitudesPasadas()
            solicitudes = SolicitudRepository.getAllSolicitudes()
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { reload() }

    // ── Diálogo ACEPTAR ───────────────────────────────────────────────────────
    aceptarTarget?.let { sol ->
        AlertDialog(
            onDismissRequest = { aceptarTarget = null; notaAdmin = ""; precioAdmin = "" },
            containerColor   = White,
            shape            = RoundedCornerShape(20.dp),
            title = {
                Text("Aceptar solicitud", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextDark)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Confirma que puedes conseguir:",
                        fontSize = 14.sp, color = TextGray
                    )
                    Text(
                        "\"${sol.nombreProducto}\"",
                        fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextDark
                    )
                    Text(
                        "Cliente: ${sol.nombreUsuario}\nCantidad: ${sol.cantidad} unid.",
                        fontSize = 13.sp, color = TextGray, lineHeight = 20.sp
                    )
                    HorizontalDivider(color = BorderColor)
                    // ── Precio que define el admin ────────────────────────────
                    Text(
                        "Precio Unit. (Bs.) *",
                        fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextDark
                    )
                    OutlinedTextField(
                        value         = precioAdmin,
                        onValueChange = { v -> if (v.length <= 10 && v.all { it.isDigit() }) precioAdmin = v },
                        placeholder   = { Text("Ej: 150", fontSize = 13.sp, color = TextDark) },
                        modifier      = Modifier.fillMaxWidth(),
                        shape         = RoundedCornerShape(10.dp),
                        singleLine    = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        ),textStyle = LocalTextStyle.current.copy(
                            color = TextDark),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = AccentBlue,
                            unfocusedBorderColor = BorderColor
                        )
                    )
                    // ── Instrucciones de pago ─────────────────────────────────
                    Text(
                        "Instrucciones de pago para el cliente:",
                        fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextDark
                    )
                    OutlinedTextField(
                        value         = notaAdmin,
                        onValueChange = { if (it.length <= 400) notaAdmin = it },
                        placeholder   = {
                            Text(
                                "Ej: Transferir al QR adjunto o pagar en tienda con referencia #${sol.id.take(6).uppercase()}...",
                                fontSize = 12.sp, color = TextDark
                            )
                        },
                        modifier  = Modifier.fillMaxWidth(),
                        shape     = RoundedCornerShape(10.dp),
                        minLines  = 3,
                        maxLines  = 5,
                        colors    = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = AccentBlue,
                            unfocusedBorderColor = BorderColor
                        )
                    )
                    Text(
                        "${notaAdmin.length}/400",
                        fontSize = 11.sp,
                        color    = TextGray,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = aceptarTarget
                        val nota   = notaAdmin.trim()
                        val precio = precioAdmin.trim().toLongOrNull() ?: 0L
                        if (precio <= 0L) return@Button   // precio requerido
                        aceptarTarget = null
                        notaAdmin     = ""
                        precioAdmin   = ""
                        scope.launch {
                            if (target != null) {
                                val ok = SolicitudRepository.aceptarSolicitud(target.id, nota, precio)
                                snackbarHost.showSnackbar(
                                    if (ok) "✅ Solicitud aceptada. El cliente fue notificado."
                                    else    "Error al aceptar. Intenta de nuevo."
                                )
                                if (ok) reload()
                            }
                        }
                    },
                    colors   = ButtonDefaults.buttonColors(containerColor = GreenOk),
                    shape    = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    enabled  = precioAdmin.trim().toLongOrNull()?.let { it > 0 } ?: false
                ) { Text("Aceptar y notificar cliente", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                OutlinedButton(
                    onClick  = { aceptarTarget = null; notaAdmin = ""; precioAdmin = "" },
                    shape    = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Cancelar", color = RedDark) }
            }
        )
    }

    // ── Diálogo RECHAZAR ──────────────────────────────────────────────────────
    rechazarTarget?.let { sol ->
        AlertDialog(
            onDismissRequest = { rechazarTarget = null; motivoRechazo = "" },
            containerColor   = White,
            shape            = RoundedCornerShape(20.dp),
            title = {
                Text("Rechazar solicitud", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Vas a rechazar la solicitud de:",
                        fontSize = 14.sp, color = TextGray
                    )
                    Text(
                        "\"${sol.nombreProducto}\" — ${sol.nombreUsuario}",
                        fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextDark
                    )
                    HorizontalDivider(color = BorderColor)
                    Text(
                        "Motivo del rechazo (opcional):",
                        fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextDark
                    )
                    OutlinedTextField(
                        value         = motivoRechazo,
                        onValueChange = { if (it.length <= 300) motivoRechazo = it },
                        placeholder   = {
                            Text(
                                "Ej: No encontramos el producto en los proveedores actuales...",
                                fontSize = 12.sp
                            )
                        },
                        modifier  = Modifier.fillMaxWidth(),
                        shape     = RoundedCornerShape(10.dp),
                        minLines  = 3,
                        maxLines  = 4,
                        colors    = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = RedDark,
                            unfocusedBorderColor = BorderColor
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = rechazarTarget
                        val motivo = motivoRechazo.trim()
                        rechazarTarget = null
                        motivoRechazo  = ""
                        scope.launch {
                            if (target != null) {
                                val ok = SolicitudRepository.rechazarSolicitud(target.id, motivo)
                                snackbarHost.showSnackbar(
                                    if (ok) "Solicitud rechazada. El cliente fue notificado."
                                    else    "Error al rechazar. Intenta de nuevo."
                                )
                                if (ok) reload()
                            }
                        }
                    },
                    colors   = ButtonDefaults.buttonColors(containerColor = RedDark),
                    shape    = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Rechazar solicitud", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                OutlinedButton(
                    onClick  = { rechazarTarget = null; motivoRechazo = "" },
                    shape    = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Cancelar") }
            }
        )
    }

    // ── Diálogo CONFIRMAR PAGO ────────────────────────────────────────────────
    confirmarPago?.let { sol ->
        AlertDialog(
            onDismissRequest = { confirmarPago = null },
            containerColor   = White,
            shape            = RoundedCornerShape(20.dp),
            title = {
                Text("Confirmar pago recibido", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "¿Ya recibiste el pago de:",
                        fontSize = 14.sp, color = TextGray
                    )
                    Text(
                        "${sol.nombreUsuario} — \"${sol.nombreProducto}\"",
                        fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextDark
                    )
                    Text(
                        "Monto: Bs. ${sol.precioReferencia * sol.cantidad}   •   Cant: ${sol.cantidad} unid.",
                        fontSize = 13.sp, color = AccentBlue
                    )
                    Text(
                        "Al confirmar, el pedido quedará como COMPLETADO y el cliente será notificado.",
                        fontSize = 13.sp, color = TextGray, lineHeight = 19.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = confirmarPago
                        confirmarPago = null
                        scope.launch {
                            if (target != null) {
                                val ok = SolicitudRepository.confirmarPago(target.id)
                                snackbarHost.showSnackbar(
                                    if (ok) "🎉 Pago confirmado. Pedido completado."
                                    else    "Error al confirmar. Intenta de nuevo."
                                )
                                if (ok) reload()
                            }
                        }
                    },
                    colors   = ButtonDefaults.buttonColors(containerColor = GreenOk),
                    shape    = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Confirmar pago", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                OutlinedButton(
                    onClick  = { confirmarPago = null },
                    shape    = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Cancelar") }
            }
        )
    }

    // ── UI principal ──────────────────────────────────────────────────────────
    Scaffold(
        containerColor = GrayPastel,
        snackbarHost   = { SnackbarHost(snackbarHost) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Solicitudes especiales",
                        fontWeight = FontWeight.Bold,
                        fontSize   = 18.sp,
                        color = Color.Black
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = Color.Black)
                    }
                },
                actions = {
                    IconButton(onClick = { reload() }) {
                        Icon(Icons.Default.Refresh, "Actualizar", tint = Color.Black)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = White)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ── Stats rápidos ─────────────────────────────────────────────────
            if (!isLoading) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(White)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val pendientes = solicitudes.count { it.estado == EstadoSolicitud.PENDIENTE }
                    val porPagar   = solicitudes.count { it.estado == EstadoSolicitud.PAGO_PENDIENTE }
                    val pagadas    = solicitudes.count { it.estado == EstadoSolicitud.PAGADO }
                    AdminStatChip(Modifier.weight(1f), "⏳", "$pendientes", "Pendientes", OrangeWarn, OrangePast)
                    AdminStatChip(Modifier.weight(1f), "💳", "$porPagar",  "Por pagar",  AccentBlue, BluePast)
                    AdminStatChip(Modifier.weight(1f), "🔍", "$pagadas",   "Verificar",  PurpleDark, PurplePast)
                }
                HorizontalDivider(color = BorderColor)
            }

            // ── Filtros ───────────────────────────────────────────────────────
            LazyRow(
                modifier            = Modifier
                    .fillMaxWidth()
                    .background(White)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(FILTROS) { filtro ->
                    val activo = filtro == filtroActivo
                    FilterChip(
                        selected = activo,
                        onClick  = { filtroActivo = filtro },
                        label    = { Text(filtro, fontSize = 12.sp) },
                        colors   = FilterChipDefaults.filterChipColors(
                            selectedContainerColor      = AccentBlue,
                            selectedLabelColor          = White
                        )
                    )
                }
            }
            HorizontalDivider(color = BorderColor)

            // ── Lista ─────────────────────────────────────────────────────────
            when {
                isLoading -> {
                    Box(
                        modifier         = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator(color = AccentBlue) }
                }
                filtradas.isEmpty() -> {
                    Box(
                        modifier         = Modifier
                            .fillMaxSize()
                            .background(GrayPastel),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Text("📋", fontSize = 56.sp)
                            Text(
                                if (filtroActivo == "Todas") "Sin solicitudes aún"
                                else "Sin solicitudes en \"$filtroActivo\"",
                                fontSize   = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color      = GrayDark,
                                textAlign  = TextAlign.Center
                            )
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier       = Modifier
                            .fillMaxSize()
                            .background(GrayPastel),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filtradas, key = { it.id }) { sol ->
                            AdminSolicitudCard(
                                solicitud    = sol,
                                onAceptar    = { aceptarTarget  = sol; notaAdmin = "" },
                                onRechazar   = { rechazarTarget = sol; motivoRechazo = "" },
                                onConfirmarPago = { confirmarPago = sol }
                            )
                        }
                        item { Spacer(Modifier.height(16.dp)) }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Card individual para el admin
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun AdminSolicitudCard(
    solicitud: SolicitudFirestore,
    onAceptar: () -> Unit,
    onRechazar: () -> Unit,
    onConfirmarPago: () -> Unit
) {
    val (bgColor, textColor, emoji, label) = estadoVisual(solicitud.estado)
    val fmt = SimpleDateFormat("dd/MM/yy HH:mm", Locale("es"))

    // Días restantes si está pendiente
    val diasRestantes = if (solicitud.estado == EstadoSolicitud.PENDIENTE) {
        val diff = solicitud.fechaLimite.seconds - System.currentTimeMillis() / 1000
        (diff / 86400).coerceAtLeast(0)
    } else -1L

    Card(
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Cabecera ──────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        solicitud.nombreProducto,
                        fontSize   = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color      = TextDark,
                        maxLines   = 2
                    )
                    Text(
                        solicitud.categoria,
                        fontSize = 12.sp,
                        color    = TextGray
                    )
                }
                Box(
                    modifier = Modifier
                        .background(bgColor, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        "$emoji $label",
                        fontSize   = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color      = textColor
                    )
                }
            }

            HorizontalDivider(color = BorderColor)

            // ── Datos del cliente ────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(GrayPastel, RoundedCornerShape(10.dp))
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("👤", fontSize = 18.sp)
                Column {
                    Text(
                        solicitud.nombreUsuario,
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = TextDark
                    )
                    Text(
                        solicitud.emailUsuario,
                        fontSize = 12.sp,
                        color    = TextGray
                    )
                }
            }

            // ── Info del pedido ───────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                LabelValue("Cantidad", "${solicitud.cantidad} unid.")
                if (solicitud.precioAdmin > 0L) {
                    LabelValue("Precio admin", "Bs. ${solicitud.precioAdmin}")
                    LabelValue("Total",        "Bs. ${solicitud.precioAdmin * solicitud.cantidad}")
                } else {
                    LabelValue("Precio", "Pendiente")
                }
                LabelValue(
                    "Solicitado",
                    SimpleDateFormat("dd/MM/yy", Locale("es"))
                        .format(Date(solicitud.fechaSolicitud.seconds * 1000))
                )
            }

            // ── Tiempo restante para pendientes ───────────────────────────────
            if (diasRestantes >= 0) {
                val colorDias = when {
                    diasRestantes <= 1 -> RedDark
                    diasRestantes <= 3 -> OrangeWarn
                    else               -> GreenOk
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (diasRestantes <= 1) RedPastel else OrangePast,
                            RoundedCornerShape(8.dp)
                        )
                        .padding(8.dp)
                ) {
                    Text("⏰", fontSize = 14.sp)
                    Text(
                        if (diasRestantes == 0L)
                            "¡Vence HOY! Responde antes de que se marque como vencida."
                        else
                            "Tiempo para responder: $diasRestantes día${if (diasRestantes == 1L) "" else "s"}",
                        fontSize   = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color      = colorDias
                    )
                }
            }

            // ── Nota del cliente ──────────────────────────────────────────────
            if (solicitud.nota.isNotBlank()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF8FAFF), RoundedCornerShape(10.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        "💬 Nota del cliente:",
                        fontSize   = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = TextGray
                    )
                    Text(
                        solicitud.nota,
                        fontSize = 13.sp,
                        color    = TextDark,
                        lineHeight = 19.sp
                    )
                }
            }

            // ── Nota del admin (ya guardada) ──────────────────────────────────
            if (solicitud.notaAdmin.isNotBlank() &&
                solicitud.estado in listOf(
                    EstadoSolicitud.PAGO_PENDIENTE,
                    EstadoSolicitud.PAGADO,
                    EstadoSolicitud.COMPLETADO
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(GreenPastel, RoundedCornerShape(10.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        "📋 Instrucciones enviadas al cliente:",
                        fontSize   = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = GreenDark
                    )
                    Text(
                        solicitud.notaAdmin,
                        fontSize = 13.sp,
                        color    = TextDark,
                        lineHeight = 19.sp
                    )
                }
            }

            // ── Motivo de rechazo ─────────────────────────────────────────────
            if (solicitud.estado == EstadoSolicitud.RECHAZADO &&
                solicitud.motivoRechazo.isNotBlank()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(RedPastel, RoundedCornerShape(10.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        "❌ Motivo enviado:",
                        fontSize   = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = RedDark
                    )
                    Text(
                        solicitud.motivoRechazo,
                        fontSize = 13.sp,
                        color    = TextDark,
                        lineHeight = 19.sp
                    )
                }
            }

            // ── Acciones ──────────────────────────────────────────────────────
            when (solicitud.estado) {
                EstadoSolicitud.PENDIENTE -> {
                    // Admin debe aceptar o rechazar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick  = onRechazar,
                            modifier = Modifier.weight(1f),
                            shape    = RoundedCornerShape(10.dp),
                            colors   = ButtonDefaults.outlinedButtonColors(
                                contentColor = RedDark
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp, RedDark.copy(alpha = 0.5f)
                            )
                        ) {
                            Text("❌  Rechazar", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                        Button(
                            onClick  = onAceptar,
                            modifier = Modifier.weight(1f),
                            shape    = RoundedCornerShape(10.dp),
                            colors   = ButtonDefaults.buttonColors(containerColor = GreenOk)
                        ) {
                            Text("✅  Aceptar", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = White)
                        }
                    }
                }

                EstadoSolicitud.PAGADO -> {
                    // El cliente dice que pagó, admin debe confirmar
                    val context = LocalContext.current
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(PurplePast, RoundedCornerShape(10.dp))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("🔍", fontSize = 16.sp)
                            Text(
                                "El cliente notificó que ya realizó el pago. ¿Lo verificaste?",
                                fontSize   = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color      = PurpleDark
                            )
                        }

                        // ── Comprobante de pago ───────────────────────────────
                        if (solicitud.comprobanteUrl.isNotBlank()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(GrayPastel, RoundedCornerShape(10.dp))
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    "📎 Comprobante de pago:",
                                    fontSize   = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color      = TextGray
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .border(1.5.dp, PurpleDark.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                ) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(solicitud.comprobanteUrl)
                                            .crossfade(true)
                                            .memoryCachePolicy(CachePolicy.DISABLED)
                                            .diskCachePolicy(CachePolicy.DISABLED)
                                            .build(),
                                        contentDescription = "Comprobante de pago",
                                        contentScale       = ContentScale.Fit,
                                        modifier           = Modifier.fillMaxSize()
                                    )
                                }
                                solicitud.fechaComprobante?.let { ts ->
                                    Text(
                                        "Subido el ${SimpleDateFormat("dd/MM/yy HH:mm", Locale("es")).format(Date(ts.seconds * 1000))}",
                                        fontSize = 11.sp,
                                        color    = TextGray
                                    )
                                }
                            }
                        } else {
                            // Sin comprobante
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFFFF8E1), RoundedCornerShape(10.dp))
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("⚠️", fontSize = 14.sp)
                                Text(
                                    "El cliente no subió comprobante. Verifica el pago manualmente.",
                                    fontSize   = 12.sp,
                                    color      = OrangeWarn,
                                    lineHeight = 17.sp
                                )
                            }
                        }

                        Button(
                            onClick  = onConfirmarPago,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            shape  = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GreenOk)
                        ) {
                            Text(
                                "💰  Confirmar pago recibido",
                                fontWeight = FontWeight.Bold,
                                fontSize   = 14.sp
                            )
                        }
                    }
                }

                EstadoSolicitud.COMPLETADO -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(GreenPastel, RoundedCornerShape(10.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("🎉", fontSize = 16.sp)
                        Text(
                            "Pedido completado",
                            fontSize   = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color      = GreenDark
                        )
                    }
                }
            }
        }
    }
}

// ── Helpers visuales ──────────────────────────────────────────────────────────
@Composable
private fun AdminStatChip(
    modifier: Modifier,
    emoji: String,
    value: String,
    label: String,
    color: Color,
    bg: Color
) {
    Card(
        modifier  = modifier,
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = bg),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(emoji, fontSize = 18.sp)
            Text(
                value,
                fontSize   = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color      = color
            )
            Text(label, fontSize = 10.sp, color = TextGray)
        }
    }
}

@Composable
private fun LabelValue(label: String, value: String) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(label, fontSize = 10.sp, color = TextGray)
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextDark)
    }
}