package com.example.dentalpro.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

// ── Colores ───────────────────────────────────────────────────────────────────
private val BlueDeep    = Color(0xFF1565C0)
private val BlueMid     = Color(0xFF1E88E5)
private val BlueLight   = Color(0xFFE3F2FD)
private val GreenOk     = Color(0xFF2E7D32)
private val GreenPast   = Color(0xFFE8F5E9)
private val RedDark     = Color(0xFFDC2626)
private val RedPastel   = Color(0xFFFFEBEE)
private val OrangeWarn  = Color(0xFFEF6C00)
private val OrangePast  = Color(0xFFFFF3E0)
private val PurpleDark  = Color(0xFF6A1B9A)
private val PurplePast  = Color(0xFFF3E5F5)
private val GrayDark    = Color(0xFF455A64)
private val GrayPastel  = Color(0xFFECEFF1)
private val White       = Color(0xFFFFFFFF)
private val GrayBg      = Color(0xFFF5F7FA)
private val GrayText    = Color(0xFF78909C)
private val TextDark    = Color(0xFF1A2332)
private val BorderColor = Color(0xFFE2E8F0)
private val BlueAccent  = Color(0xFF2563EB)
private val BluePastel  = Color(0xFFEFF6FF)

// ─────────────────────────────────────────────────────────────────────────────
// NotificacionesScreen
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificacionesScreen(onBack: () -> Unit) {
    val scope       = rememberCoroutineScope()
    val currentUser = FirebaseAuth.getInstance().currentUser

    var solicitudes  by remember { mutableStateOf<List<SolicitudFirestore>>(emptyList()) }
    var isLoading    by remember { mutableStateOf(true) }
    val snackbarHost = remember { SnackbarHostState() }

    // Solicitud objetivo para el diálogo QR+comprobante
    var pagoTarget by remember { mutableStateOf<SolicitudFirestore?>(null) }

    fun reload() {
        scope.launch {
            isLoading = true
            val uid = currentUser?.uid ?: return@launch
            SolicitudRepository.vencerSolicitudesPasadas()
            solicitudes = SolicitudRepository.getSolicitudesUsuario(uid)
            solicitudes.filter { !it.leidoPorCliente }.forEach {
                SolicitudRepository.marcarLeida(it.id)
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { reload() }

    // ── Diálogo QR + comprobante de pago ─────────────────────────────────────
    pagoTarget?.let { sol ->
        SolicitudQrPagoDialog(
            solicitud  = sol,
            onPagado   = {
                pagoTarget = null
                scope.launch {
                    snackbarHost.showSnackbar("✅ Pago notificado. El admin lo verificará.")
                    reload()
                }
            },
            onDismiss  = { pagoTarget = null }
        )
    }

    Scaffold(
        containerColor = GrayBg,
        snackbarHost   = { SnackbarHost(snackbarHost) },
        topBar = {
            TopAppBar(
                title = {
                    Text("Mis solicitudes", color = BlueDeep, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = BlueDeep)
                    }
                },
                actions = {
                    IconButton(onClick = { reload() }) {
                        Icon(Icons.Default.Refresh, "Actualizar", tint = BlueDeep)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = White)
            )
        }
    ) { padding ->
        when {
            isLoading -> {
                Box(
                    modifier         = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = BlueDeep)
                }
            }

            solicitudes.isEmpty() -> {
                Box(
                    modifier         = Modifier
                        .fillMaxSize()
                        .background(GrayBg)
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Text("🔔", fontSize = 64.sp)
                        Text(
                            "Sin solicitudes",
                            fontSize   = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color      = TextDark
                        )
                        Text(
                            "Cuando solicites un producto sin stock, aparecerá aquí con su estado.",
                            fontSize  = 14.sp,
                            color     = GrayText,
                            textAlign = TextAlign.Center,
                            lineHeight = 21.sp
                        )
                    }
                }
            }

            else -> {
                LazyColumn(
                    modifier       = Modifier
                        .fillMaxSize()
                        .background(GrayBg)
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(solicitudes, key = { it.id }) { sol ->
                        SolicitudClienteCard(
                            solicitud       = sol,
                            onNotificarPago = { pagoTarget = sol },
                            onCancelar      = {
                                scope.launch {
                                    val ok = SolicitudRepository.cancelarSolicitud(sol.id)
                                    if (ok) {
                                        snackbarHost.showSnackbar("Solicitud cancelada")
                                        reload()
                                    } else {
                                        snackbarHost.showSnackbar("Error al cancelar. Intenta de nuevo.")
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SolicitudQrPagoDialog — muestra el QR, botón guardar, botón "Ya pagué"
// y luego permite subir el comprobante, igual que en el checkout normal.
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun SolicitudQrPagoDialog(
    solicitud : SolicitudFirestore,
    onPagado  : () -> Unit,
    onDismiss : () -> Unit
) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    val totalSolicitud = solicitud.precioAdmin * solicitud.cantidad

    // ── Estado ────────────────────────────────────────────────────────────────
    var qrConfig       by remember { mutableStateOf<QrConfig?>(null) }
    var loadingQr      by remember { mutableStateOf(true) }
    var guardandoQr    by remember { mutableStateOf(false) }
    var confirmando    by remember { mutableStateOf(false) }
    var subiendo       by remember { mutableStateOf(false) }
    var comprobanteUrl by remember { mutableStateOf<String?>(null) }
    var feedbackMsg    by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

    // Tras confirmar el pago, mostramos la pantalla de éxito+comprobante
    var pedidoConfirmado by remember { mutableStateOf(false) }

    // ── Permiso galería ───────────────────────────────────────────────────────
    val imagePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                subiendo    = true
                feedbackMsg = null
                val result = QrRepository.subirComprobanteYGuardar(
                    context    = context,
                    uri        = uri,
                    tipo       = ComprobanteDestino.SOLICITUD,
                    documentId = solicitud.id
                )
                subiendo = false
                if (result.isSuccess) {
                    comprobanteUrl = result.getOrNull()
                    feedbackMsg    = Pair(false, "✅ Comprobante subido correctamente")
                } else {
                    feedbackMsg = Pair(true, "❌ Error al subir: ${result.exceptionOrNull()?.message}")
                }
            }
        }
    }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) imagePickerLauncher.launch("image/*")
        else         imagePickerLauncher.launch("image/*")   // intentar igual
    }

    fun pedirPermiso() {
        val perm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            Manifest.permission.READ_MEDIA_IMAGES
        else
            Manifest.permission.READ_EXTERNAL_STORAGE
        permLauncher.launch(perm)
    }

    // ── Cargar QR ─────────────────────────────────────────────────────────────
    LaunchedEffect(Unit) {
        loadingQr = true
        qrConfig  = QrRepository.getQrConfig()
        loadingQr = false
        // pedir permiso galería al abrir
        val perm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            Manifest.permission.READ_MEDIA_IMAGES
        else
            Manifest.permission.READ_EXTERNAL_STORAGE
        permLauncher.launch(perm)
    }

    Dialog(onDismissRequest = { if (!confirmando && !subiendo) onDismiss() }) {
        Card(
            shape  = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = White)
        ) {
            Column(
                modifier            = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {

                if (!pedidoConfirmado) {
                    // ══════════════════════════════════════════════════════════
                    // PASO 1 — Mostrar QR y confirmar pago
                    // ══════════════════════════════════════════════════════════

                    Text(
                        "Pago por QR",
                        fontSize   = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color      = GreenOk
                    )
                    Text(
                        "Escanea el código con tu app bancaria",
                        fontSize  = 12.sp,
                        color     = TextDark,
                        textAlign = TextAlign.Center
                    )

                    // Imagen QR
                    Box(
                        modifier         = Modifier
                            .size(210.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .border(2.dp, GreenOk, RoundedCornerShape(14.dp))
                            .background(GrayBg),
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            loadingQr -> CircularProgressIndicator(color = GreenOk)
                            qrConfig?.imageUrl.isNullOrBlank() -> {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("📷", fontSize = 40.sp)
                                    Text(
                                        "QR no configurado\npor el administrador",
                                        fontSize  = 12.sp,
                                        color     = GrayText,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                            else -> AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(qrConfig!!.imageUrl)
                                    .crossfade(true)
                                    .memoryCachePolicy(CachePolicy.DISABLED)
                                    .diskCachePolicy(CachePolicy.DISABLED)
                                    .build(),
                                contentDescription = "QR de pago",
                                contentScale       = ContentScale.Fit,
                                modifier           = Modifier
                                    .size(210.dp)
                                    .clip(RoundedCornerShape(14.dp))
                            )
                        }
                    }

                    // Monto
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(GreenPast, RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    solicitud.nombreProducto,
                                    fontSize   = 12.sp,
                                    color      = TextDark,
                                    maxLines   = 1
                                )
                                Text(
                                    "${solicitud.cantidad} unid. × Bs. ${solicitud.precioAdmin}",
                                    fontSize = 11.sp,
                                    color    = GrayText
                                )
                            }
                            Text(
                                "Bs. $totalSolicitud",
                                fontSize   = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color      = TextDark
                            )
                        }
                    }

                    // Instrucciones del admin
                    if (solicitud.notaAdmin.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(BlueLight, RoundedCornerShape(10.dp))
                                .padding(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    "📋 Instrucciones del admin:",
                                    fontSize   = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color      = BlueDeep
                                )
                                Text(
                                    solicitud.notaAdmin,
                                    fontSize   = 13.sp,
                                    color      = TextDark,
                                    lineHeight = 19.sp
                                )
                            }
                        }
                    }

                    // Feedback
                    feedbackMsg?.let { (esError, msg) ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (esError) RedPastel else GreenPast,
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(10.dp)
                        ) {
                            Text(
                                msg,
                                fontSize   = 12.sp,
                                color      = if (esError) RedDark else GreenOk,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Botón Guardar QR en galería
                    if (!qrConfig?.imageUrl.isNullOrBlank()) {
                        OutlinedButton(
                            onClick  = {
                                scope.launch {
                                    guardandoQr = true
                                    feedbackMsg = null
                                    val result = QrRepository.guardarQrEnGaleria(
                                        context  = context,
                                        imageUrl = qrConfig!!.imageUrl
                                    )
                                    guardandoQr = false
                                    feedbackMsg = if (result.isSuccess)
                                        Pair(false, "✅ QR guardado en tu galería")
                                    else
                                        Pair(true, "❌ No se pudo guardar: ${result.exceptionOrNull()?.message}")
                                }
                            },
                            enabled  = !guardandoQr && !confirmando,
                            modifier = Modifier.fillMaxWidth().height(46.dp),
                            shape    = RoundedCornerShape(10.dp),
                            colors   = ButtonDefaults.outlinedButtonColors(contentColor = BlueAccent),
                            border   = androidx.compose.foundation.BorderStroke(1.5.dp, BlueAccent)
                        ) {
                            if (guardandoQr) {
                                Row(
                                    verticalAlignment     = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier    = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color       = BlueAccent
                                    )
                                    Text("Guardando...", fontSize = 14.sp)
                                }
                            } else {
                                Row(
                                    verticalAlignment     = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("💾", fontSize = 16.sp)
                                    Text(
                                        "Guardar QR en galería",
                                        fontSize   = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    // Botones Cancelar / Ya pagué
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier              = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick  = onDismiss,
                            enabled  = !confirmando,
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape    = RoundedCornerShape(10.dp)
                        ) { Text("Cancelar", fontWeight = FontWeight.Bold, color = RedDark) }

                        Button(
                            onClick  = {
                                scope.launch {
                                    confirmando = true
                                    feedbackMsg = null
                                    val ok = SolicitudRepository.notificarPago(solicitud.id)
                                    confirmando = false
                                    if (ok) {
                                        pedidoConfirmado = true
                                    } else {
                                        feedbackMsg = Pair(true, "❌ Error al notificar. Intenta de nuevo.")
                                    }
                                }
                            },
                            enabled  = !confirmando && !loadingQr,
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape    = RoundedCornerShape(10.dp),
                            colors   = ButtonDefaults.buttonColors(containerColor = GreenOk)
                        ) {
                            if (confirmando) {
                                CircularProgressIndicator(
                                    modifier    = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color       = White
                                )
                            } else {
                                Text("Ya pagué", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }

                } else {
                    // ══════════════════════════════════════════════════════════
                    // PASO 2 — Pago notificado, subir comprobante
                    // ══════════════════════════════════════════════════════════

                    Text("✅", fontSize = 56.sp)
                    Text(
                        "¡Pago notificado!",
                        fontSize   = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color      = GreenOk,
                        textAlign  = TextAlign.Center
                    )
                    Text(
                        "El administrador verificará tu pago.\nSube el comprobante para agilizar la verificación.",
                        fontSize   = 13.sp,
                        color      = GrayText,
                        textAlign  = TextAlign.Center,
                        lineHeight = 19.sp
                    )

                    // Feedback
                    feedbackMsg?.let { (esError, msg) ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (esError) RedPastel else GreenPast,
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(10.dp)
                        ) {
                            Text(
                                msg,
                                fontSize   = 12.sp,
                                color      = if (esError) RedDark else GreenOk,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Vista previa comprobante subido
                    if (!comprobanteUrl.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(2.dp, GreenOk, RoundedCornerShape(12.dp))
                        ) {
                            AsyncImage(
                                model              = ImageRequest.Builder(context)
                                    .data(comprobanteUrl)
                                    .crossfade(true)
                                    .memoryCachePolicy(CachePolicy.DISABLED)
                                    .diskCachePolicy(CachePolicy.DISABLED)
                                    .build(),
                                contentDescription = "Comprobante de pago",
                                contentScale       = ContentScale.Crop,
                                modifier           = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .background(GreenOk, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("✓ Subido", fontSize = 10.sp, color = White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Botón subir / reemplazar comprobante
                    Button(
                        onClick  = { pedirPermiso() },
                        enabled  = !subiendo,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape    = RoundedCornerShape(12.dp),
                        colors   = ButtonDefaults.buttonColors(
                            containerColor = if (comprobanteUrl.isNullOrBlank()) BlueAccent
                            else Color(0xFF0284C7)
                        )
                    ) {
                        if (subiendo) {
                            Row(
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier    = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color       = White
                                )
                                Text("Subiendo comprobante...", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            }
                        } else {
                            Row(
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("📎", fontSize = 18.sp)
                                Text(
                                    if (comprobanteUrl.isNullOrBlank()) "Subir comprobante de pago"
                                    else "Reemplazar comprobante",
                                    fontSize   = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // Aviso si aún no subió comprobante
                    if (comprobanteUrl.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFFFF8E1), RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("⚠️", fontSize = 13.sp)
                                Text(
                                    "Puedes finalizar sin subir el comprobante, pero recomendamos adjuntarlo para agilizar la verificación.",
                                    fontSize   = 11.sp,
                                    color      = OrangeWarn,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }

                    // Botón Finalizar
                    Button(
                        onClick  = onPagado,
                        enabled  = !subiendo,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape    = RoundedCornerShape(12.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = GreenOk)
                    ) {
                        Text("Finalizar", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Card individual de solicitud para el cliente
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun SolicitudClienteCard(
    solicitud: SolicitudFirestore,
    onNotificarPago: () -> Unit,
    onCancelar: () -> Unit
) {
    val (bgColor, textColor, emoji, label) = estadoVisual(solicitud.estado)
    val fmt = SimpleDateFormat("dd MMM yyyy", Locale("es"))

    val diasRestantes = if (solicitud.estado == EstadoSolicitud.PENDIENTE) {
        val diff = solicitud.fechaLimite.seconds - System.currentTimeMillis() / 1000
        (diff / 86400).coerceAtLeast(0)
    } else -1L

    // Estado para el diálogo de confirmación de cancelación
    var mostrarDialogoCancelar by remember { mutableStateOf(false) }

    // Diálogo de confirmación
    if (mostrarDialogoCancelar) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoCancelar = false },
            shape            = RoundedCornerShape(16.dp),
            title = {
                Text(
                    "¿Cancelar solicitud?",
                    fontWeight = FontWeight.Bold,
                    fontSize   = 16.sp,
                    color      = White
                )
            },
            text = {
                Text(
                    "Se cancelará tu solicitud de \"${solicitud.nombreProducto}\". Esta acción no se puede deshacer.",
                    fontSize   = 13.sp,
                    color      = GrayText,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        mostrarDialogoCancelar = false
                        onCancelar()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedDark),
                    shape  = RoundedCornerShape(10.dp)
                ) {
                    Text("Sí, cancelar", fontWeight = FontWeight.Bold, color = White)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { mostrarDialogoCancelar = false },
                    shape   = RoundedCornerShape(10.dp)
                ) {
                    Text("No, volver", fontWeight = FontWeight.Medium)
                }
            }
        )
    }

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
            // ── Cabecera: producto + badge estado ─────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(BlueAccent, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) { Text("🦷", fontSize = 26.sp) }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        solicitud.nombreProducto,
                        fontSize   = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = TextDark,
                        maxLines   = 2
                    )
                    Text(solicitud.categoria, fontSize = 12.sp, color = GrayText)
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

            // ── Info rápida ───────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                InfoChip("Cant.", "${solicitud.cantidad} unid.")
                if (solicitud.precioAdmin > 0L) {
                    InfoChip("Precio admin", "Bs. ${solicitud.precioAdmin}")
                    InfoChip("Total", "Bs. ${solicitud.precioAdmin * solicitud.cantidad}")
                } else {
                    InfoChip("Precio", "Pendiente")
                }
                InfoChip("Solicitado", fmt.format(Date(solicitud.fechaSolicitud.seconds * 1000)))
            }

            // ── Contador de días para pendientes ──────────────────────────────
            if (diasRestantes >= 0) {
                val colorDias = when {
                    diasRestantes <= 1 -> RedDark
                    diasRestantes <= 3 -> OrangeWarn
                    else               -> GreenOk
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("⏳", fontSize = 14.sp)
                    Text(
                        if (diasRestantes == 0L) "Vence hoy"
                        else "El admin tiene $diasRestantes día${if (diasRestantes == 1L) "" else "s"} para responder",
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
                        .background(GrayPastel, RoundedCornerShape(10.dp))
                        .padding(10.dp)
                ) {
                    Text("Tu nota:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = GrayText)
                    Text(solicitud.nota, fontSize = 13.sp, color = TextDark, lineHeight = 19.sp)
                }
            }

            // ── Nota del admin ────────────────────────────────────────────────
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
                        .background(GreenPast, RoundedCornerShape(10.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        "📋 Instrucciones de pago:",
                        fontSize   = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color      = GreenOk
                    )
                    Text(solicitud.notaAdmin, fontSize = 13.sp, color = TextDark, lineHeight = 20.sp)
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
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        "❌ Motivo del rechazo:",
                        fontSize   = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color      = RedDark
                    )
                    Text(solicitud.motivoRechazo, fontSize = 13.sp, color = TextDark, lineHeight = 20.sp)
                }
            }

            // ── Botón "Ya pagué" → abre el diálogo QR ─────────────────────────
            if (solicitud.estado == EstadoSolicitud.PAGO_PENDIENTE) {
                Button(
                    onClick  = onNotificarPago,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = GreenOk)
                ) {
                    Text(
                        "💳  Ver QR y confirmar pago",
                        fontSize   = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // ── Botón cancelar solicitud (solo en estados cancelables) ─────────
            if (solicitud.estado in listOf(
                    EstadoSolicitud.PENDIENTE,
                    EstadoSolicitud.PAGO_PENDIENTE
                )
            ) {
                OutlinedButton(
                    onClick  = { mostrarDialogoCancelar = true },
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = ButtonDefaults.outlinedButtonColors(contentColor = RedDark),
                    border   = androidx.compose.foundation.BorderStroke(1.5.dp, RedDark)
                ) {
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("✕", fontSize = 14.sp, color = RedDark, fontWeight = FontWeight.Bold)
                        Text(
                            "Cancelar solicitud",
                            fontSize   = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color      = RedDark
                        )
                    }
                }
            }

            // ── Completado ────────────────────────────────────────────────────
            if (solicitud.estado == EstadoSolicitud.COMPLETADO) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(GreenPast, RoundedCornerShape(10.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("🎉", fontSize = 18.sp)
                    Text(
                        "¡Pedido completado! El admin confirmó tu pago.",
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color      = GreenOk
                    )
                }
            }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────
data class EstadoUI(
    val bgColor: Color,
    val textColor: Color,
    val emoji: String,
    val label: String
)

fun estadoVisual(estado: String): EstadoUI = when (estado) {
    EstadoSolicitud.PENDIENTE      -> EstadoUI(OrangePast,  OrangeWarn, "⏳", "Pendiente")
    EstadoSolicitud.PAGO_PENDIENTE -> EstadoUI(BlueLight,   BlueDeep,   "💳", "Pagar")
    EstadoSolicitud.ACEPTADO       -> EstadoUI(BlueLight,   BlueDeep,   "✅", "Aceptado")
    EstadoSolicitud.PAGADO         -> EstadoUI(PurplePast,  PurpleDark, "🔍", "Verificando")
    EstadoSolicitud.COMPLETADO     -> EstadoUI(GreenPast,   GreenOk,    "🎉", "Completado")
    EstadoSolicitud.RECHAZADO      -> EstadoUI(RedPastel,   RedDark,    "❌", "Rechazado")
    EstadoSolicitud.VENCIDO        -> EstadoUI(GrayPastel,  GrayDark,   "⌛", "Vencido")
    else                           -> EstadoUI(GrayPastel,  GrayDark,   "❓", estado)
}

@Composable
private fun InfoChip(label: String, value: String) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(label, fontSize = 10.sp, color = TextDark)
        Text(
            value,
            fontSize   = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color      = TextDark
        )
    }
}