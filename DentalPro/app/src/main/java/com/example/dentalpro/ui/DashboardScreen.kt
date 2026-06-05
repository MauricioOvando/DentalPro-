package com.example.dentalpro.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

// ── Paleta ────────────────────────────────────────────────────────────────────
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
private val PurpleAcc   = Color(0xFF7C3AED)
private val PurplePast  = Color(0xFFF3E8FF)
private val White       = Color(0xFFFFFFFF)
private val BgGray      = Color(0xFFF1F5F9)
private val TextGray    = Color(0xFF64748B)
private val BorderColor = Color(0xFFE2E8F0)
private val TextDark    = Color(0xFF0F172A)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(onBack: () -> Unit) {

    val scope   = rememberCoroutineScope()
    var resumen by remember { mutableStateOf<ResumenGeneral?>(null) }
    var ventas  by remember { mutableStateOf<List<VentaMensual>>(emptyList()) }
    var topProductos by remember { mutableStateOf<List<ProductoVendido>>(emptyList()) }
    var alertasStock by remember { mutableStateOf<List<StockAlerta>>(emptyList()) }
    var categorias   by remember { mutableStateOf<List<EstadisticaCategoria>>(emptyList()) }
    var isLoading    by remember { mutableStateOf(true) }

    fun cargar() {
        scope.launch {
            isLoading = true
            resumen      = StatsRepository.getResumenGeneral()
            ventas       = StatsRepository.getVentasMensuales(6)
            topProductos = StatsRepository.getProductosMasVendidos(5)
            alertasStock = StatsRepository.getAlertasStock(5)
            categorias   = StatsRepository.getEstadisticasPorCategoria()
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { cargar() }

    Scaffold(
        containerColor = BgGray,
        topBar = {
            TopAppBar(
                title = { Text("Dashboard", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = AccentBlue)
                    }
                },
                actions = {
                    IconButton(onClick = { cargar() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Actualizar", tint = AccentBlue)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = White,
                    titleContentColor = TextDark
                )
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator(color = AccentBlue)
                    Text("Cargando estadísticas...", color = TextGray, fontSize = 14.sp)
                }
            }
        } else {
            val r = resumen ?: ResumenGeneral()

            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                // ── Header ────────────────────────────────────────────────────
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Brush.linearGradient(listOf(NavyDark, NavyMid)))
                            .padding(20.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Resumen General", fontSize = 11.sp,
                                color = White.copy(alpha = 0.6f), letterSpacing = 1.sp)
                            Text("Ingresos Totales", fontSize = 14.sp, color = White.copy(alpha = 0.8f))
                            Text(
                                "Bs. ${"%,d".format(r.ingresosTotales)}",
                                fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = White
                            )
                            Spacer(Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                MiniStat("Este mes", "Bs. ${"%,d".format(r.ingresosEsteMes)}", GreenOk)
                                MiniStat("Pedidos", "${r.totalPedidos}", AccentBlue)
                                MiniStat("Clientes", "${r.totalClientes}", PurpleAcc)
                            }
                        }
                    }
                }

                // ── Tarjetas KPI ──────────────────────────────────────────────
                item {
                    Text("Indicadores", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextDark)
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        KpiCard(Modifier.weight(1f), "Pendientes",    "${r.pedidosPendientes}",     OrangeWarn,  OrangePast,  "🕐")
                        KpiCard(Modifier.weight(1f), "Completados",   "${r.pedidosCompletados}",    GreenOk,     GreenPastel, "✅")
                    }
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        KpiCard(Modifier.weight(1f), "Sin stock",     "${r.productosSinStock}",     RedDark,     RedPastel,   "❌")
                        KpiCard(Modifier.weight(1f), "Stock bajo",    "${r.productosStockBajo}",    OrangeWarn,  OrangePast,  "⚠️")
                    }
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        KpiCard(Modifier.weight(1f), "Solicitudes",   "${r.solicitudesPendientes}", PurpleAcc,   PurplePast,  "📋")
                        KpiCard(Modifier.weight(1f), "Productos",     "${r.totalProductos}",        AccentBlue,  Color(0xFFEFF6FF), "📦")
                    }
                }

                // ── Gráfico de barras: ventas mensuales ───────────────────────
                if (ventas.isNotEmpty()) {
                    item {
                        SectionTitle("Ventas últimos 6 meses")
                    }
                    item {
                        Card(
                            shape     = RoundedCornerShape(16.dp),
                            colors    = CardDefaults.cardColors(containerColor = White),
                            elevation = CardDefaults.cardElevation(2.dp),
                            modifier  = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                val maxVenta = ventas.maxOfOrNull { it.totalVentas } ?: 1L
                                Row(
                                    modifier = Modifier.fillMaxWidth().height(140.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    ventas.forEach { vm ->
                                        val ratio = if (maxVenta > 0) vm.totalVentas.toFloat() / maxVenta.toFloat() else 0f
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Bottom
                                        ) {
                                            if (vm.totalVentas > 0) {
                                                Text(
                                                    "Bs.${vm.totalVentas / 1000}k",
                                                    fontSize = 8.sp,
                                                    color    = TextGray,
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                            Spacer(Modifier.height(2.dp))
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height((120 * ratio).coerceAtLeast(4f).dp)
                                                    .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                                    .background(
                                                        Brush.verticalGradient(listOf(AccentBlue, Color(0xFF93C5FD)))
                                                    )
                                            )
                                        }
                                    }
                                }
                                // Etiquetas de mes
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    ventas.forEach { vm ->
                                        Text(
                                            vm.mes,
                                            modifier  = Modifier.weight(1f),
                                            textAlign = TextAlign.Center,
                                            fontSize  = 10.sp,
                                            color     = TextGray,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Productos más vendidos ─────────────────────────────────────
                if (topProductos.isNotEmpty()) {
                    item { SectionTitle("Top productos vendidos") }
                    item {
                        Card(
                            shape     = RoundedCornerShape(16.dp),
                            colors    = CardDefaults.cardColors(containerColor = White),
                            elevation = CardDefaults.cardElevation(2.dp),
                            modifier  = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
                                val maxCantidad = topProductos.maxOfOrNull { it.cantidadVendida } ?: 1
                                topProductos.forEachIndexed { index, pv ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        // Posición
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .background(
                                                    when (index) {
                                                        0 -> Color(0xFFFFD700)
                                                        1 -> Color(0xFFC0C0C0)
                                                        2 -> Color(0xFFCD7F32)
                                                        else -> BgGray
                                                    },
                                                    CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                "${index + 1}",
                                                fontSize   = 11.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color      = if (index < 3) White else TextGray
                                            )
                                        }
                                        // Barra + nombre
                                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                            Text(pv.nombre, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                                                color = TextDark, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            val barRatio = pv.cantidadVendida.toFloat() / maxCantidad.toFloat()
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth(barRatio.coerceAtLeast(0.05f))
                                                    .height(6.dp)
                                                    .clip(RoundedCornerShape(3.dp))
                                                    .background(
                                                        when (index) {
                                                            0 -> Color(0xFFFFD700)
                                                            1 -> Color(0xFFC0C0C0)
                                                            2 -> Color(0xFFCD7F32)
                                                            else -> AccentBlue.copy(alpha = 0.5f)
                                                        }
                                                    )
                                            )
                                        }
                                        // Cantidad + ingresos
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("${pv.cantidadVendida} uds", fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold, color = AccentBlue)
                                            Text("Bs.${pv.ingresos}", fontSize = 10.sp, color = TextGray)
                                        }
                                    }
                                    if (index < topProductos.lastIndex) HorizontalDivider(color = BorderColor)
                                }
                            }
                        }
                    }
                }

                // ── Ventas por categoría ──────────────────────────────────────
                if (categorias.isNotEmpty()) {
                    item { SectionTitle("Ventas por categoría") }
                    item {
                        Card(
                            shape     = RoundedCornerShape(16.dp),
                            colors    = CardDefaults.cardColors(containerColor = White),
                            elevation = CardDefaults.cardElevation(2.dp),
                            modifier  = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
                                categorias.take(6).forEachIndexed { index, cat ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        val catColor = listOf(AccentBlue, GreenOk, OrangeWarn, PurpleAcc, RedDark, Color(0xFF0891B2))
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .background(catColor[index % catColor.size], CircleShape)
                                        )
                                        Text(
                                            cat.categoria,
                                            modifier = Modifier.weight(1f),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = TextDark,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("${cat.unidadesVendidas} uds", fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold, color = catColor[index % catColor.size])
                                            Text("Bs.${cat.totalVentas}", fontSize = 10.sp, color = TextGray)
                                        }
                                    }
                                    if (index < categorias.take(6).lastIndex) HorizontalDivider(color = BorderColor)
                                }
                            }
                        }
                    }
                }

                // ── Alertas de stock ──────────────────────────────────────────
                if (alertasStock.isNotEmpty()) {
                    item { SectionTitle("⚠️ Alertas de stock") }
                    items(alertasStock) { alerta ->
                        Card(
                            shape     = RoundedCornerShape(12.dp),
                            colors    = CardDefaults.cardColors(
                                containerColor = if (alerta.stock == 0L) RedPastel else OrangePast
                            ),
                            elevation = CardDefaults.cardElevation(1.dp),
                            modifier  = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(if (alerta.stock == 0L) "❌" else "⚠️", fontSize = 22.sp)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(alerta.nombre, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                                        color = TextDark, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(alerta.categoria, fontSize = 11.sp, color = TextGray)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        if (alerta.stock == 0L) "Sin stock" else "Stock: ${alerta.stock}",
                                        fontSize   = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color      = if (alerta.stock == 0L) RedDark else OrangeWarn
                                    )
                                    Text("Bs.${alerta.precio}", fontSize = 11.sp, color = TextGray)
                                }
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

// ── Componentes auxiliares ────────────────────────────────────────────────────

@Composable
private fun SectionTitle(title: String) {
    Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF0F172A))
}

@Composable
private fun MiniStat(label: String, value: String, color: Color) {
    Column {
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = color)
        Text(label, fontSize = 10.sp, color = Color.White.copy(alpha = 0.6f))
    }
}

@Composable
private fun KpiCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    color: Color,
    bgColor: Color,
    emoji: String
) {
    Card(
        modifier  = modifier,
        shape     = RoundedCornerShape(14.dp),
        colors    = CardDefaults.cardColors(containerColor = Color(0xFFFFFFFF)),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(bgColor, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(emoji, fontSize = 20.sp)
            }
            Column {
                Text(value, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = color)
                Text(label, fontSize = 11.sp, color = Color(0xFF64748B))
            }
        }
    }
}