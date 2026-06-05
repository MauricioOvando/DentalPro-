package com.example.dentalpro.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

private val NavyDark      = Color(0xFF132B46)
private val NavyMid       = Color(0xFF1B5C93)
private val AccentBlue    = Color(0xFF2563EB)
private val GreenOk       = Color(0xFF16A34A)
private val GreenPastel   = Color(0xFFDCFCE7)
private val GreenDark     = Color(0xFF14532D)
private val RedDark       = Color(0xFFDC2626)
private val RedPastel     = Color(0xFFFFEBEE)
private val OrangeWarn    = Color(0xFFEA580C)
private val OrangePastel  = Color(0xFFFFF3E0)
private val WhatsAppGreen = Color(0xFF25D366)
private val GmailRed      = Color(0xFFEA4335)
private val White         = Color(0xFFFFFFFF)
private val BgGray        = Color(0xFFF1F5F9)
private val TextGray      = Color(0xFF64748B)
private val BorderColor   = Color(0xFFE2E8F0)
private val TextDark      = Color(0xFF0F172A)

// Modelo extendido de pedido con datos del usuario
data class PedidoConUsuario(
    val pedido: PedidoFirestore,
    val usuario: UsuarioFirestore?
)

// ── Helper: mensaje de confirmación ──────────────────────────────────────────
private fun buildMensajeConfirmacion(
    nombreCliente: String,
    referencia: String,
    items: List<Map<String, Any>>,
    total: Long
): String {
    val sb = StringBuilder()
    sb.appendLine("Hola $nombreCliente,")
    sb.appendLine()
    sb.appendLine("✅ Tu pedido en DentalPro ha sido CONFIRMADO.")
    sb.appendLine()
    sb.appendLine("📋 Referencia: #$referencia")
    sb.appendLine()
    sb.appendLine("🛒 Detalle de tu pedido:")
    items.forEach { item ->
        val nombre   = item["nombre"]?.toString() ?: "Producto"
        val cantidad = when (val v = item["cantidad"]) {
            is Long -> v; is Int -> v.toLong(); is Double -> v.toLong(); else -> 1L
        }
        val precio = when (val v = item["precio"]) {
            is Long -> v; is Int -> v.toLong(); is Double -> v.toLong(); else -> 0L
        }
        val productoId = item["productoId"]?.toString()?.take(8)?.uppercase() ?: "—"
        sb.appendLine("  • $nombre")
        sb.appendLine("    Código: $productoId  |  Cant: $cantidad  |  Bs. ${precio * cantidad}")
    }
    sb.appendLine()
    sb.appendLine("💰 Total pagado: Bs. $total")
    sb.appendLine()
    sb.appendLine("Pronto recibirás tu pedido. ¡Gracias por confiar en DentalPro! 🦷")
    return sb.toString().trim()
}

// ─────────────────────────────────────────────────────────────────────────────
// AdminOrdersScreen
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminOrdersScreen(onBack: () -> Unit) {
    val scope       = rememberCoroutineScope()
    val context     = LocalContext.current

    var pedidos      by remember { mutableStateOf<List<PedidoConUsuario>>(emptyList()) }
    var isLoading    by remember { mutableStateOf(true) }
    var filtroEstado by remember { mutableStateOf("todos") }
    var snackbarMsg  by remember { mutableStateOf<String?>(null) }
    val snackbarHost = remember { SnackbarHostState() }

    fun reload() {
        scope.launch {
            isLoading = true
            val todosPedidos = FirestoreRepository.getTodosPedidos()
            pedidos = todosPedidos.map { pedido ->
                val usuario = FirestoreRepository.getUsuario(pedido.usuarioId)
                PedidoConUsuario(pedido, usuario)
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { reload() }

    LaunchedEffect(snackbarMsg) {
        snackbarMsg?.let { snackbarHost.showSnackbar(it); snackbarMsg = null }
    }

    val pedidosFiltrados = pedidos.filter {
        filtroEstado == "todos" || it.pedido.estado == filtroEstado
    }

    Scaffold(
        containerColor = BgGray,
        snackbarHost   = { SnackbarHost(snackbarHost) }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {

            // ── Header ────────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.linearGradient(listOf(NavyDark, NavyMid)))
                    .padding(horizontal = 4.dp, vertical = 16.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = White)
                    }
                    Text(
                        "Pedidos",
                        fontSize   = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color      = White,
                        modifier   = Modifier.weight(1f)
                    )
                    IconButton(onClick = { reload() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Recargar", tint = White)
                    }
                }
            }

            // ── Filtros por estado ────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(White)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "todos"      to "Todos",
                    "pendiente"  to "Pendientes",
                    "verificado" to "Verificados",
                    "completado" to "Completados"
                ).forEach { (valor, etiqueta) ->
                    val selected = filtroEstado == valor
                    FilterChip(
                        selected = selected,
                        onClick  = { filtroEstado = valor },
                        label    = {
                            Text(
                                etiqueta,
                                fontSize   = 11.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AccentBlue,
                            selectedLabelColor     = White,
                            containerColor         = BgGray,
                            labelColor             = TextGray
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
                }
            }

            HorizontalDivider(color = BorderColor)

            // ── Contenido ─────────────────────────────────────────────────────
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(color = AccentBlue)
                        Text("Cargando pedidos...", fontSize = 13.sp, color = TextGray)
                    }
                }
            } else if (pedidosFiltrados.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("📋", fontSize = 48.sp)
                        Text("No hay pedidos", fontSize = 15.sp, color = TextGray)
                    }
                }
            } else {
                LazyColumn(
                    modifier            = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                    contentPadding      = PaddingValues(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Contadores resumen
                    item {
                        Row(
                            modifier              = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MiniStat(
                                modifier = Modifier.weight(1f),
                                label    = "Pendientes",
                                value    = pedidos.count { it.pedido.estado == "pendiente" }.toString(),
                                color    = OrangeWarn
                            )
                            MiniStat(
                                modifier = Modifier.weight(1f),
                                label    = "Verificados",
                                value    = pedidos.count { it.pedido.estado == "verificado" }.toString(),
                                color    = AccentBlue
                            )
                            MiniStat(
                                modifier = Modifier.weight(1f),
                                label    = "Completados",
                                value    = pedidos.count { it.pedido.estado == "completado" }.toString(),
                                color    = GreenOk
                            )
                        }
                    }

                    items(pedidosFiltrados, key = { it.pedido.id }) { item ->
                        AdminOrderCard(
                            item       = item,
                            onVerify   = {
                                scope.launch {
                                    val ok = FirestoreRepository.actualizarEstadoPedido(item.pedido.id, "verificado")
                                    snackbarMsg = if (ok) "Pedido verificado correctamente" else "Error al verificar"
                                    if (ok) reload()
                                }
                            },
                            onComplete = {
                                scope.launch {
                                    val ok = FirestoreRepository.actualizarEstadoPedido(item.pedido.id, "completado")
                                    snackbarMsg = if (ok) "Pedido completado" else "Error al actualizar"
                                    if (ok) reload()
                                }
                            },
                            onWhatsApp = { telefono, nombreCliente, pedidoItems, totalPedido, referencia ->
                                val numero  = telefono.replace(Regex("[^0-9]"), "")
                                val prefijo = if (!numero.startsWith("591")) "591$numero" else numero
                                val mensaje = buildMensajeConfirmacion(nombreCliente, referencia, pedidoItems, totalPedido)
                                val url = "https://wa.me/$prefijo?text=${Uri.encode(mensaje)}"
                                try {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                } catch (e: Exception) {
                                    snackbarMsg = "No se pudo abrir WhatsApp"
                                }
                            },
                            onEmail = { email, nombreCliente, pedidoItems, totalPedido, referencia ->
                                val asunto = "DentalPro – Pedido #$referencia Confirmado ✅"
                                val cuerpo = buildMensajeConfirmacion(nombreCliente, referencia, pedidoItems, totalPedido)
                                val intent = Intent(Intent.ACTION_SENDTO).apply {
                                    data = Uri.parse("mailto:")
                                    putExtra(Intent.EXTRA_EMAIL,   arrayOf(email))
                                    putExtra(Intent.EXTRA_SUBJECT, asunto)
                                    putExtra(Intent.EXTRA_TEXT,    cuerpo)
                                }
                                try {
                                    context.startActivity(Intent.createChooser(intent, "Enviar correo con…"))
                                } catch (e: Exception) {
                                    snackbarMsg = "No se encontró app de correo"
                                }
                            }
                        )
                    }

                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }
}

// ── Mini estadísticas ─────────────────────────────────────────────────────────
@Composable
fun MiniStat(modifier: Modifier = Modifier, label: String, value: String, color: Color) {
    Card(
        modifier  = modifier,
        shape     = RoundedCornerShape(10.dp),
        colors    = CardDefaults.cardColors(containerColor = White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(
            modifier            = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = color)
            Text(label, fontSize = 10.sp, color = TextGray)
        }
    }
}

// ── Tarjeta de pedido ─────────────────────────────────────────────────────────
@Composable
fun AdminOrderCard(
    item       : PedidoConUsuario,
    onVerify   : () -> Unit,
    onComplete : () -> Unit,
    onWhatsApp : (telefono: String, nombre: String, items: List<Map<String, Any>>, total: Long, referencia: String) -> Unit,
    onEmail    : (email: String, nombre: String, items: List<Map<String, Any>>, total: Long, referencia: String) -> Unit
) {
    val pedido   = item.pedido
    val usuario  = item.usuario
    var expanded by remember { mutableStateOf(false) }

    // ── Estado para el diálogo de comprobante a pantalla completa ─────────────
    var mostrarComprobante by remember { mutableStateOf(false) }

    val fechaStr = try {
        SimpleDateFormat("dd MMM yyyy  HH:mm", Locale.getDefault()).format(pedido.fecha.toDate())
    } catch (e: Exception) { "" }

    val estadoColor = when (pedido.estado) {
        "pendiente"  -> OrangeWarn
        "verificado" -> AccentBlue
        "completado" -> GreenOk
        else         -> TextGray
    }
    val estadoBg = when (pedido.estado) {
        "pendiente"  -> OrangePastel
        "verificado" -> Color(0xFFEFF6FF)
        "completado" -> GreenPastel
        else         -> BgGray
    }

    val referencia    = pedido.id.take(8).uppercase()
    val tieneTelefono = !usuario?.telefono.isNullOrBlank()
    val tieneEmail    = !usuario?.email.isNullOrBlank()
    val tieneComprobante = pedido.comprobanteUrl.isNotBlank()

    // ── Diálogo comprobante a pantalla completa ───────────────────────────────
    if (mostrarComprobante && tieneComprobante) {
        ComprobanteDialog(
            imageUrl  = pedido.comprobanteUrl,
            referencia = referencia,
            onDismiss = { mostrarComprobante = false }
        )
    }

    Card(
        shape     = RoundedCornerShape(14.dp),
        colors    = CardDefaults.cardColors(containerColor = White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {

            // ── Cabecera ──────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "Pedido #$referencia",
                            fontSize   = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color      = TextDark
                        )
                        // Badge comprobante
                        if (tieneComprobante) {
                            Box(
                                modifier = Modifier
                                    .background(GreenPastel, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    "📎 Comprobante",
                                    fontSize   = 9.sp,
                                    color      = GreenDark,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(fechaStr, fontSize = 11.sp, color = TextGray)
                    Text(
                        usuario?.nombre ?: "Cliente desconocido",
                        fontSize   = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color      = AccentBlue
                    )
                }
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(estadoBg, RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            pedido.estado.replaceFirstChar { it.uppercase() },
                            fontSize   = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color      = estadoColor
                        )
                    }
                    Icon(
                        if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint     = TextGray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // ── Total siempre visible ─────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .background(GreenPastel, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text("QR", fontSize = 10.sp, color = GreenDark, fontWeight = FontWeight.Bold)
                    }
                    Box(
                        modifier = Modifier
                            .background(BgGray, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            "${pedido.items.size} producto${if (pedido.items.size != 1) "s" else ""}",
                            fontSize = 10.sp,
                            color    = TextGray
                        )
                    }
                }
                Text(
                    "Bs. ${pedido.total}",
                    fontSize   = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color      = TextDark
                )
            }

            // ── Detalle expandible ────────────────────────────────────────────
            AnimatedVisibility(
                visible = expanded,
                enter   = expandVertically(),
                exit    = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BgGray)
                ) {
                    HorizontalDivider(color = BorderColor)
                    Spacer(Modifier.height(10.dp))

                    // ── Datos del cliente ─────────────────────────────────────
                    Column(
                        modifier            = Modifier.padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            "Datos del cliente",
                            fontSize   = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color      = TextGray
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("👤", fontSize = 13.sp)
                            Text(usuario?.nombre ?: "Desconocido", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextDark)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("📧", fontSize = 13.sp)
                            Text(usuario?.email ?: "-", fontSize = 12.sp, color = TextGray)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("📞", fontSize = 13.sp)
                            Text(
                                if (tieneTelefono) usuario!!.telefono
                                else "Sin teléfono — se puede notificar por Gmail",
                                fontSize = 12.sp,
                                color    = if (tieneTelefono) TextGray else OrangeWarn
                            )
                        }
                    }

                    HorizontalDivider(
                        color    = BorderColor,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )

                    // ── Comprobante de pago ───────────────────────────────────
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "Comprobante de pago",
                            fontSize   = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color      = TextGray
                        )

                        if (tieneComprobante) {
                            // Vista previa clickeable
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(2.dp, GreenOk, RoundedCornerShape(12.dp))
                                    .clickable { mostrarComprobante = true }
                            ) {
                                val context = LocalContext.current
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(pedido.comprobanteUrl)
                                        .crossfade(true)
                                        .memoryCachePolicy(CachePolicy.DISABLED)
                                        .diskCachePolicy(CachePolicy.DISABLED)
                                        .build(),
                                    contentDescription = "Comprobante de pago",
                                    contentScale       = ContentScale.Crop,
                                    modifier           = Modifier.fillMaxSize()
                                )
                                // Overlay "Toca para ampliar"
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .fillMaxWidth()
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(Color.Transparent, Color(0xCC000000))
                                            )
                                        )
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "🔍 Toca para ampliar",
                                        fontSize   = 11.sp,
                                        color      = White,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                // Badge "Subido"
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp)
                                        .background(GreenOk, RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        "✓ Subido",
                                        fontSize   = 10.sp,
                                        color      = White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        } else {
                            // Sin comprobante
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(OrangePastel, RoundedCornerShape(10.dp))
                                    .padding(14.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment     = Alignment.CenterVertically
                                ) {
                                    Text("⚠️", fontSize = 18.sp)
                                    Column {
                                        Text(
                                            "Sin comprobante",
                                            fontSize   = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color      = OrangeWarn
                                        )
                                        Text(
                                            "El cliente aún no ha subido su comprobante de pago.",
                                            fontSize   = 11.sp,
                                            color      = OrangeWarn,
                                            lineHeight = 16.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(
                        color    = BorderColor,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )

                    // ── Productos del pedido ──────────────────────────────────
                    Text(
                        "Productos del pedido",
                        fontSize   = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = TextGray,
                        modifier   = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(Modifier.height(6.dp))

                    pedido.items.forEach { itemMap ->
                        val nombre   = itemMap["nombre"]?.toString() ?: "Producto"
                        val cantidad = when (val v = itemMap["cantidad"]) {
                            is Long -> v; is Int -> v.toLong(); is Double -> v.toLong(); else -> 1L
                        }
                        val precio = when (val v = itemMap["precio"]) {
                            is Long -> v; is Int -> v.toLong(); is Double -> v.toLong(); else -> 0L
                        }
                        val productoId = itemMap["productoId"]?.toString()?.take(8)?.uppercase() ?: "—"

                        Row(
                            modifier              = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 5.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier              = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment     = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier         = Modifier
                                        .size(34.dp)
                                        .background(White, RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) { Text("🦷", fontSize = 16.sp) }
                                Column {
                                    Text(nombre, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextDark)
                                    Text(
                                        "Cód: $productoId  •  x$cantidad  •  Bs. $precio c/u",
                                        fontSize = 10.sp,
                                        color    = TextGray
                                    )
                                }
                            }
                            Text(
                                "Bs. ${precio * cantidad}",
                                fontSize   = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color      = TextDark
                            )
                        }
                    }

                    HorizontalDivider(
                        color    = BorderColor,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )

                    // ── Total ─────────────────────────────────────────────────
                    Row(
                        modifier              = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total pagado", fontSize = 13.sp, color = TextGray)
                        Text(
                            "Bs. ${pedido.total}",
                            fontSize   = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color      = TextDark
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    // ── Botones de acción ─────────────────────────────────────
                    Column(
                        modifier            = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // WhatsApp
                        if (pedido.estado != "completado" && tieneTelefono) {
                            Button(
                                onClick  = {
                                    onWhatsApp(
                                        usuario!!.telefono,
                                        usuario.nombre,
                                        pedido.items,
                                        pedido.total,
                                        referencia
                                    )
                                },
                                modifier = Modifier.fillMaxWidth().height(44.dp),
                                shape    = RoundedCornerShape(10.dp),
                                colors   = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen)
                            ) {
                                Text("💬", fontSize = 16.sp)
                                Spacer(Modifier.width(8.dp))
                                Text("Notificar por WhatsApp", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            }
                        } else if (pedido.estado != "completado" && tieneEmail) {
                            Button(
                                onClick  = {
                                    onEmail(
                                        usuario!!.email,
                                        usuario.nombre,
                                        pedido.items,
                                        pedido.total,
                                        referencia
                                    )
                                },
                                modifier = Modifier.fillMaxWidth().height(44.dp),
                                shape    = RoundedCornerShape(10.dp),
                                colors   = ButtonDefaults.buttonColors(containerColor = GmailRed)
                            ) {
                                Text("📧", fontSize = 16.sp)
                                Spacer(Modifier.width(8.dp))
                                Text("Notificar por Gmail", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        // Opción adicional correo si tiene ambos
                        if (pedido.estado != "completado" && tieneTelefono && tieneEmail) {
                            OutlinedButton(
                                onClick  = {
                                    onEmail(
                                        usuario!!.email,
                                        usuario.nombre,
                                        pedido.items,
                                        pedido.total,
                                        referencia
                                    )
                                },
                                modifier = Modifier.fillMaxWidth().height(40.dp),
                                shape    = RoundedCornerShape(10.dp),
                                colors   = ButtonDefaults.outlinedButtonColors(contentColor = GmailRed)
                            ) {
                                Text("📧", fontSize = 14.sp)
                                Spacer(Modifier.width(6.dp))
                                Text("También notificar por correo", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            }
                        }

                        // Verificar pago (solo si está pendiente)
                        if (pedido.estado == "pendiente") {
                            Button(
                                onClick  = onVerify,
                                modifier = Modifier.fillMaxWidth().height(44.dp),
                                shape    = RoundedCornerShape(10.dp),
                                colors   = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                            ) {
                                Text("Verificar pago", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Completar (solo si está verificado)
                        if (pedido.estado == "verificado") {
                            Button(
                                onClick  = onComplete,
                                modifier = Modifier.fillMaxWidth().height(44.dp),
                                shape    = RoundedCornerShape(10.dp),
                                colors   = ButtonDefaults.buttonColors(containerColor = GreenOk)
                            ) {
                                Text("Marcar como completado", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Completado
                        if (pedido.estado == "completado") {
                            Box(
                                modifier         = Modifier
                                    .fillMaxWidth()
                                    .background(GreenPastel, RoundedCornerShape(10.dp))
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "✅ Pedido completado",
                                    fontSize   = 13.sp,
                                    color      = GreenOk,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ComprobanteDialog — imagen a pantalla completa
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun ComprobanteDialog(
    imageUrl  : String,
    referencia: String,
    onDismiss : () -> Unit
) {
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape  = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = TextDark)
        ) {
            Column(
                modifier            = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Título
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text(
                        "Comprobante #$referencia",
                        fontSize   = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color      = White
                    )
                    TextButton(onClick = onDismiss) {
                        Text("✕", fontSize = 16.sp, color = White)
                    }
                }

                // Imagen a pantalla completa dentro del diálogo
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 200.dp, max = 460.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(BgGray)
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(imageUrl)
                            .crossfade(true)
                            .memoryCachePolicy(CachePolicy.DISABLED)
                            .diskCachePolicy(CachePolicy.DISABLED)
                            .build(),
                        contentDescription = "Comprobante de pago",
                        contentScale       = ContentScale.Fit,
                        modifier           = Modifier.fillMaxWidth()
                    )
                }

                // Info extra
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1E293B), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Text("📎", fontSize = 14.sp)
                        Text(
                            "Comprobante enviado por el cliente para el pedido #$referencia",
                            fontSize   = 11.sp,
                            color      = Color(0xFF94A3B8),
                            lineHeight = 16.sp
                        )
                    }
                }

                // Botón cerrar
                Button(
                    onClick  = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape    = RoundedCornerShape(10.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                ) {
                    Text("Cerrar", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}