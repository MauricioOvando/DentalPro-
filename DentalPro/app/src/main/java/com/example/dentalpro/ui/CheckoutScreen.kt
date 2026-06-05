package com.example.dentalpro.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
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

// ─── Colores ──────────────────────────────────────────────────────────────────
private val GreenDark    = Color(0xFF1B5E20)
private val GreenMid     = Color(0xFF2E7D32)
private val GreenPastel  = Color(0xFFE8F5E9)
private val BlueAccent   = Color(0xFF2563EB)
private val BluePastel   = Color(0xFFEFF6FF)
private val White        = Color(0xFFFFFFFF)
private val GrayBg       = Color(0xFFF5F5F5)
private val GrayText     = Color(0xFF757575)
private val GrayBorder   = Color(0xFFE2E8F0)
private val OrangeWarn   = Color(0xFFFF6F00)
private val RedOff       = Color(0xFFDC2626)

// ─────────────────────────────────────────────────────────────────────────────
// CheckoutScreen
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    cartViewModel: CartViewModel,
    onBack: () -> Unit,
    onOrderPlaced: () -> Unit
) {
    val items = cartViewModel.items
    val total = cartViewModel.total

    var showQRDialog   by remember { mutableStateOf(false) }
    var orderConfirmed by remember { mutableStateOf(false) }
    var pedidoId       by remember { mutableStateOf("") }

    // Diálogo de éxito + subir comprobante
    if (orderConfirmed) {
        OrderSuccessDialog(
            total    = total,
            pedidoId = pedidoId,
            onDismiss = {
                cartViewModel.vaciarCarrito()
                onOrderPlaced()
            }
        )
    }

    // Diálogo QR
    if (showQRDialog) {
        QRPaymentDialog(
            total     = total,
            items     = items,
            onConfirm = { id ->
                pedidoId       = id
                showQRDialog   = false
                orderConfirmed = true
            },
            onDismiss = { showQRDialog = false }
        )
    }

    Scaffold(
        containerColor = GrayBg,
        topBar = {
            TopAppBar(
                title = { Text("Confirmar Pedido", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = White)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Resumen productos ─────────────────────────────────────────────
            Card(
                shape     = RoundedCornerShape(12.dp),
                colors    = CardDefaults.cardColors(containerColor = White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Resumen (${items.size} productos)",
                        fontSize   = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(10.dp))
                    items.forEach { item ->
                        Row(
                            modifier              = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier          = Modifier.weight(1f)
                            ) {
                                Text("🦷", fontSize = 20.sp)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    item.producto.nombre,
                                    fontSize   = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines   = 1
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Text("x${item.cantidad}", fontSize = 12.sp, color = GrayText)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Bs. ${item.producto.precio * item.cantidad}",
                                fontSize   = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color      = GreenDark
                            )
                        }
                    }
                }
            }

            // ── Método de pago ────────────────────────────────────────────────
            Card(
                shape     = RoundedCornerShape(12.dp),
                colors    = CardDefaults.cardColors(containerColor = White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Método de Pago", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(GreenPastel)
                            .border(1.5.dp, GreenMid, RoundedCornerShape(10.dp))
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier          = Modifier.size(40.dp).background(GreenMid, RoundedCornerShape(8.dp)),
                                contentAlignment  = Alignment.Center
                            ) { Text("📱", fontSize = 20.sp) }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("Pago por QR", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = GreenDark)
                                Text("Escanea el QR para pagar", fontSize = 11.sp, color = GrayText)
                            }
                            Spacer(Modifier.weight(1f))
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = GreenMid)
                        }
                    }
                }
            }

            // ── Total ─────────────────────────────────────────────────────────
            Card(
                shape     = RoundedCornerShape(12.dp),
                colors    = CardDefaults.cardColors(containerColor = White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Subtotal:", color = GrayText, fontSize = 14.sp)
                        Text("Bs. ${"%.2f".format(total)}", fontSize = 14.sp)
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Envío:", color = GrayText, fontSize = 14.sp)
                        Text("Gratis", color = GreenMid, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total:", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(
                            "Bs. ${"%.2f".format(total)}",
                            fontWeight = FontWeight.Bold,
                            fontSize   = 16.sp,
                            color      = GreenDark
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick  = { showQRDialog = true },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = RoundedCornerShape(12.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = GreenMid),
                enabled  = items.isNotEmpty()
            ) {
                Text("Ver QR y Confirmar Pedido", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// QRPaymentDialog  — muestra el QR real del admin, botón guardar y botón confirmar
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun QRPaymentDialog(
    total     : Double,
    items     : List<CartItem>,
    onConfirm : (pedidoId: String) -> Unit,
    onDismiss : () -> Unit
) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    // ── Estado QR ─────────────────────────────────────────────────────────────
    var qrConfig     by remember { mutableStateOf<QrConfig?>(null) }
    var loadingQr    by remember { mutableStateOf(true) }

    // ── Estado acciones ───────────────────────────────────────────────────────
    var guardandoQr      by remember { mutableStateOf(false) }
    var confirmando      by remember { mutableStateOf(false) }
    var feedbackMsg      by remember { mutableStateOf<Pair<Boolean, String>?>(null) } // (esError, msg)

    // ── Permiso galería ───────────────────────────────────────────────────────
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* solo pedimos, no bloqueamos */ }

    // ── Cargar QR al abrir ────────────────────────────────────────────────────
    LaunchedEffect(Unit) {
        loadingQr = true
        qrConfig  = QrRepository.getQrConfig()
        loadingQr = false

        val perm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            Manifest.permission.READ_MEDIA_IMAGES
        else
            Manifest.permission.READ_EXTERNAL_STORAGE
        permissionLauncher.launch(perm)
    }

    Dialog(onDismissRequest = { if (!confirmando) onDismiss() }) {
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
                // ── Título ────────────────────────────────────────────────────
                Text(
                    "Pago por QR",
                    fontSize   = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color      = GreenDark
                )
                Text(
                    "Escanea el código con tu app bancaria",
                    fontSize  = 12.sp,
                    color     = GrayText,
                    textAlign = TextAlign.Center
                )

                // ── Imagen QR ─────────────────────────────────────────────────
                Box(
                    modifier         = Modifier
                        .size(210.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .border(2.dp, GreenMid, RoundedCornerShape(14.dp))
                        .background(GrayBg),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        loadingQr -> {
                            CircularProgressIndicator(color = GreenMid)
                        }
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
                        else -> {
                            AsyncImage(
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
                }

                // ── Monto ─────────────────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(GreenPastel, RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Text("Monto a pagar:", fontSize = 13.sp, color = GrayText)
                        Text(
                            "Bs. ${"%.2f".format(total)}",
                            fontSize   = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color      = GreenDark
                        )
                    }
                }

                // ── Feedback msg ──────────────────────────────────────────────
                feedbackMsg?.let { (esError, msg) ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (esError) Color(0xFFFFEBEE) else GreenPastel,
                                RoundedCornerShape(8.dp)
                            )
                            .padding(10.dp)
                    ) {
                        Text(
                            msg,
                            fontSize = 12.sp,
                            color    = if (esError) RedOff else GreenDark,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // ── Botón GUARDAR QR en galería ───────────────────────────────
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
                                feedbackMsg = if (result.isSuccess) {
                                    Pair(false, "✅ QR guardado en tu galería")
                                } else {
                                    Pair(true, "❌ No se pudo guardar: ${result.exceptionOrNull()?.message}")
                                }
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

                // ── Botones Cancelar / Confirmar ──────────────────────────────
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier              = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick  = onDismiss,
                        enabled  = !confirmando,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape    = RoundedCornerShape(10.dp)
                    ) { Text("Cancelar") }

                    Button(
                        onClick  = {
                            scope.launch {
                                confirmando = true
                                feedbackMsg = null
                                val currentUser = FirebaseAuth.getInstance().currentUser
                                if (currentUser != null) {
                                    val id = FirestoreRepository.guardarPedidoYDescontarStockConId(
                                        usuarioId = currentUser.uid,
                                        items     = items,
                                        total     = total
                                    )
                                    confirmando = false
                                    if (id != null) {
                                        onConfirm(id)
                                    } else {
                                        feedbackMsg = Pair(true, "❌ Error al guardar el pedido. Intenta de nuevo.")
                                    }
                                } else {
                                    confirmando = false
                                    feedbackMsg = Pair(true, "❌ Debes iniciar sesión para continuar.")
                                }
                            }
                        },
                        enabled  = !confirmando && !loadingQr,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape    = RoundedCornerShape(10.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = GreenMid)
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
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// OrderSuccessDialog  — pedido confirmado + subir comprobante
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun OrderSuccessDialog(
    total     : Double,
    pedidoId  : String,
    onDismiss : () -> Unit
) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    var subiendo       by remember { mutableStateOf(false) }
    var comprobanteUrl by remember { mutableStateOf<String?>(null) }
    var feedbackMsg    by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

    // ── Selector de imagen para comprobante ───────────────────────────────────
    val imagePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null && pedidoId.isNotBlank()) {
            scope.launch {
                subiendo    = true
                feedbackMsg = null
                val result = QrRepository.subirComprobanteYGuardar(
                    context      = context,
                    uri          = uri,
                    tipo         = ComprobanteDestino.PEDIDO,
                    documentId   = pedidoId
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

    // ── Permiso galería ───────────────────────────────────────────────────────
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) imagePickerLauncher.launch("image/*")
    }

    Dialog(onDismissRequest = { if (!subiendo) onDismiss() }) {
        Card(
            shape  = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = White)
        ) {
            Column(
                modifier            = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // ── Icono éxito ───────────────────────────────────────────────
                Text("✅", fontSize = 56.sp)

                Text(
                    "¡Pedido Confirmado!",
                    fontSize   = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color      = GreenDark,
                    textAlign  = TextAlign.Center
                )
                Text(
                    "Tu pedido por Bs. ${"%.2f".format(total)} ha sido registrado.\n" +
                            "Sube tu comprobante de pago para agilizar la verificación.",
                    fontSize  = 13.sp,
                    color     = GrayText,
                    textAlign = TextAlign.Center,
                    lineHeight = 19.sp
                )

                // ── Feedback msg ──────────────────────────────────────────────
                feedbackMsg?.let { (esError, msg) ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (esError) Color(0xFFFFEBEE) else GreenPastel,
                                RoundedCornerShape(8.dp)
                            )
                            .padding(10.dp)
                    ) {
                        Text(
                            msg,
                            fontSize   = 12.sp,
                            color      = if (esError) RedOff else GreenDark,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // ── Vista previa comprobante subido ───────────────────────────
                if (!comprobanteUrl.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(2.dp, GreenMid, RoundedCornerShape(12.dp))
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
                        // Badge "Subido"
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .background(GreenMid, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("✓ Subido", fontSize = 10.sp, color = White, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // ── Botón subir / reemplazar comprobante ──────────────────────
                Button(
                    onClick  = {
                        val perm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                            Manifest.permission.READ_MEDIA_IMAGES
                        else
                            Manifest.permission.READ_EXTERNAL_STORAGE
                        permissionLauncher.launch(perm)
                    },
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

                // ── Aviso opcional ────────────────────────────────────────────
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
                                "Puedes finalizar sin subir el comprobante, pero recomendamos adjuntarlo para agilizar tu pedido.",
                                fontSize   = 11.sp,
                                color      = OrangeWarn,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                // ── Botón Finalizar ───────────────────────────────────────────
                Button(
                    onClick  = onDismiss,
                    enabled  = !subiendo,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = GreenMid)
                ) {
                    Text("Finalizar", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}