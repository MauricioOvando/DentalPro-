package com.example.dentalpro.ui

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

// ── Paleta ────────────────────────────────────────────────────────────────────
private val NavyDark   = Color(0xFF0D1B2A)
private val NavyMid    = Color(0xFF1B2B3D)
private val AccentBlue = Color(0xFF2563EB)
private val GreenOk    = Color(0xFF16A34A)
private val GreenPast  = Color(0xFFDCFCE7)
private val RedDark    = Color(0xFFDC2626)
private val RedPastel  = Color(0xFFFFEBEE)
private val OrangeWarn = Color(0xFFEA580C)
private val OrangePast = Color(0xFFFFF3E0)
private val White      = Color(0xFFFFFFFF)
private val BgGray     = Color(0xFFF1F5F9)
private val TextGray   = Color(0xFF64748B)
private val BorderColor= Color(0xFFE2E8F0)
private val TextDark   = Color(0xFF0F172A)

// ── Tipos de reporte ──────────────────────────────────────────────────────────
enum class TipoReporte(val titulo: String, val emoji: String, val descripcion: String) {
    VENTAS_MENSUALES("Ventas Mensuales",    "📊", "Resumen de ventas e ingresos por mes"),
    STOCK_BAJO      ("Alertas de Stock",    "⚠️", "Productos con stock bajo o agotado"),
    MAS_VENDIDOS    ("Más Vendidos",        "🏆", "Ranking de productos por unidades vendidas"),
    GENERAL         ("Reporte General",     "📋", "Resumen completo del negocio")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(onBack: () -> Unit) {

    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    var reporteSeleccionado by remember { mutableStateOf(TipoReporte.GENERAL) }
    var isLoading           by remember { mutableStateOf(false) }
    var isGeneratingPdf     by remember { mutableStateOf(false) }
    var snackMsg            by remember { mutableStateOf<String?>(null) }
    val snackHost           = remember { SnackbarHostState() }

    // Datos
    var resumen      by remember { mutableStateOf<ResumenGeneral?>(null) }
    var ventas       by remember { mutableStateOf<List<VentaMensual>>(emptyList()) }
    var topProductos by remember { mutableStateOf<List<ProductoVendido>>(emptyList()) }
    var alertas      by remember { mutableStateOf<List<StockAlerta>>(emptyList()) }

    fun cargarDatos() {
        scope.launch {
            isLoading = true
            when (reporteSeleccionado) {
                TipoReporte.GENERAL          -> { resumen = StatsRepository.getResumenGeneral(); ventas = StatsRepository.getVentasMensuales(12) }
                TipoReporte.VENTAS_MENSUALES -> ventas = StatsRepository.getVentasMensuales(12)
                TipoReporte.STOCK_BAJO       -> alertas = StatsRepository.getAlertasStock(10)
                TipoReporte.MAS_VENDIDOS     -> topProductos = StatsRepository.getProductosMasVendidos(10)
            }
            isLoading = false
        }
    }

    LaunchedEffect(reporteSeleccionado) { cargarDatos() }

    LaunchedEffect(snackMsg) {
        snackMsg?.let { snackHost.showSnackbar(it); snackMsg = null }
    }

    Scaffold(
        containerColor = BgGray,
        snackbarHost   = { SnackbarHost(snackHost) },
        topBar = {
            TopAppBar(
                title = { Text("Reportes", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = AccentBlue)
                    }
                },
                actions = {
                    IconButton(onClick = { cargarDatos() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Actualizar", tint = AccentBlue)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = White, titleContentColor = TextDark)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── Selector de tipo de reporte ───────────────────────────────────
            item {
                Text("Tipo de reporte", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextDark)
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TipoReporte.entries.chunked(2).forEach { fila ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            fila.forEach { tipo ->
                                val seleccionado = reporteSeleccionado == tipo
                                Card(
                                    onClick   = { reporteSeleccionado = tipo },
                                    modifier  = Modifier.weight(1f),
                                    shape     = RoundedCornerShape(12.dp),
                                    colors    = CardDefaults.cardColors(
                                        containerColor = if (seleccionado) AccentBlue else White
                                    ),
                                    elevation = CardDefaults.cardElevation(if (seleccionado) 4.dp else 1.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(tipo.emoji, fontSize = 22.sp)
                                        Text(
                                            tipo.titulo,
                                            fontSize   = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color      = if (seleccionado) White else TextDark
                                        )
                                        Text(
                                            tipo.descripcion,
                                            fontSize  = 10.sp,
                                            color     = if (seleccionado) White.copy(alpha = 0.8f) else TextGray,
                                            maxLines  = 2,
                                            overflow  = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                            if (fila.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }

            // ── Botón generar PDF ─────────────────────────────────────────────
            item {
                Button(
                    onClick = {
                        scope.launch {
                            isGeneratingPdf = true
                            try {
                                val uri = withContext(Dispatchers.IO) {
                                    generarPdf(
                                        context          = context,
                                        tipo             = reporteSeleccionado,
                                        resumen          = resumen,
                                        ventas           = ventas,
                                        topProductos     = topProductos,
                                        alertas          = alertas
                                    )
                                }
                                if (uri != null) {
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(uri, "application/pdf")
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Abrir PDF"))
                                    snackMsg = "PDF generado correctamente"
                                } else {
                                    snackMsg = "Error al generar el PDF"
                                }
                            } catch (e: Exception) {
                                snackMsg = "Error: ${e.message}"
                            }
                            isGeneratingPdf = false
                        }
                    },
                    modifier  = Modifier.fillMaxWidth().height(52.dp),
                    shape     = RoundedCornerShape(14.dp),
                    colors    = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                    enabled   = !isGeneratingPdf && !isLoading
                ) {
                    if (isGeneratingPdf) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = White)
                        Spacer(Modifier.width(8.dp))
                        Text("Generando PDF...", fontWeight = FontWeight.SemiBold)
                    } else {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Exportar a PDF", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // ── Vista previa del reporte ──────────────────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Vista previa", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextDark)
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = AccentBlue)
                    }
                }
            }

            // Vista previa según tipo seleccionado
            when (reporteSeleccionado) {

                TipoReporte.GENERAL -> {
                    val r = resumen
                    if (r != null) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Brush.linearGradient(listOf(NavyDark, NavyMid)))
                                    .padding(20.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Resumen General", fontSize = 13.sp, color = White.copy(alpha = 0.7f),
                                        fontWeight = FontWeight.SemiBold)
                                    Spacer(Modifier.height(4.dp))
                                    ReportRow("Ingresos totales",    "Bs. ${"%,d".format(r.ingresosTotales)}", White)
                                    ReportRow("Ingresos este mes",   "Bs. ${"%,d".format(r.ingresosEsteMes)}", GreenOk)
                                    ReportRow("Total pedidos",       "${r.totalPedidos}", White)
                                    ReportRow("Pedidos completados", "${r.pedidosCompletados}", GreenOk)
                                    ReportRow("Pedidos pendientes",  "${r.pedidosPendientes}", OrangeWarn)
                                    ReportRow("Total clientes",      "${r.totalClientes}", White)
                                    ReportRow("Total productos",     "${r.totalProductos}", White)
                                    ReportRow("Sin stock",           "${r.productosSinStock}", RedDark)
                                    ReportRow("Stock bajo",          "${r.productosStockBajo}", OrangeWarn)
                                    ReportRow("Solicitudes pend.",   "${r.solicitudesPendientes}", Color(0xFFB78BFA))
                                }
                            }
                        }
                    }
                }

                TipoReporte.VENTAS_MENSUALES -> {
                    if (ventas.isNotEmpty()) {
                        items(ventas.reversed()) { vm ->
                            Card(
                                shape     = RoundedCornerShape(12.dp),
                                colors    = CardDefaults.cardColors(containerColor = White),
                                elevation = CardDefaults.cardElevation(1.dp),
                                modifier  = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .background(AccentBlue.copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(vm.mes, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AccentBlue)
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("${vm.mes} ${vm.anio}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextDark)
                                        Text("${vm.cantidadPedidos} pedidos", fontSize = 11.sp, color = TextGray)
                                    }
                                    Text(
                                        "Bs. ${"%,d".format(vm.totalVentas)}",
                                        fontSize   = 15.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color      = if (vm.totalVentas > 0) GreenOk else TextGray
                                    )
                                }
                            }
                        }
                    } else if (!isLoading) {
                        item { EmptyState("No hay datos de ventas") }
                    }
                }

                TipoReporte.STOCK_BAJO -> {
                    if (alertas.isNotEmpty()) {
                        items(alertas) { alerta ->
                            Card(
                                shape     = RoundedCornerShape(12.dp),
                                colors    = CardDefaults.cardColors(
                                    containerColor = if (alerta.stock == 0L) RedPastel else OrangePast
                                ),
                                elevation = CardDefaults.cardElevation(1.dp),
                                modifier  = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(if (alerta.stock == 0L) "❌" else "⚠️", fontSize = 24.sp)
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(alerta.nombre, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                                            color = TextDark, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(alerta.categoria, fontSize = 11.sp, color = TextGray)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            if (alerta.stock == 0L) "Sin stock" else "${alerta.stock} uds",
                                            fontSize   = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color      = if (alerta.stock == 0L) RedDark else OrangeWarn
                                        )
                                        Text("Bs.${alerta.precio}", fontSize = 11.sp, color = TextGray)
                                    }
                                }
                            }
                        }
                    } else if (!isLoading) {
                        item { EmptyState("✅ Todos los productos tienen stock suficiente") }
                    }
                }

                TipoReporte.MAS_VENDIDOS -> {
                    if (topProductos.isNotEmpty()) {
                        items(topProductos.withIndex().toList()) { (index, pv) ->
                            Card(
                                shape     = RoundedCornerShape(12.dp),
                                colors    = CardDefaults.cardColors(containerColor = White),
                                elevation = CardDefaults.cardElevation(1.dp),
                                modifier  = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        when (index) { 0 -> "🥇"; 1 -> "🥈"; 2 -> "🥉"; else -> "${index + 1}." },
                                        fontSize = 20.sp
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(pv.nombre, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                                            color = TextDark, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text("${pv.cantidadVendida} unidades vendidas", fontSize = 11.sp, color = TextGray)
                                    }
                                    Text(
                                        "Bs.${"%,d".format(pv.ingresos)}",
                                        fontSize = 13.sp, fontWeight = FontWeight.Bold, color = GreenOk
                                    )
                                }
                            }
                        }
                    } else if (!isLoading) {
                        item { EmptyState("No hay datos de ventas aún") }
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

// ── Componentes auxiliares ────────────────────────────────────────────────────

@Composable
private fun ReportRow(label: String, value: String, valueColor: Color) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = valueColor)
    }
}

@Composable
private fun EmptyState(msg: String) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(msg, fontSize = 14.sp, color = Color(0xFF64748B), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

// ── Generación de PDF ─────────────────────────────────────────────────────────

private fun generarPdf(
    context: Context,
    tipo: TipoReporte,
    resumen: ResumenGeneral?,
    ventas: List<VentaMensual>,
    topProductos: List<ProductoVendido>,
    alertas: List<StockAlerta>
): Uri? {
    return try {
        val pdf      = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4
        val page     = pdf.startPage(pageInfo)
        val canvas   = page.canvas
        val fecha    = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())

        val paintTitle = Paint().apply { textSize = 20f; isFakeBoldText = true; color = android.graphics.Color.parseColor("#0D1B2A") }
        val paintSub   = Paint().apply { textSize = 13f; color = android.graphics.Color.parseColor("#64748B") }
        val paintHead  = Paint().apply { textSize = 11f; isFakeBoldText = true; color = android.graphics.Color.parseColor("#2563EB") }
        val paintBody  = Paint().apply { textSize = 11f; color = android.graphics.Color.parseColor("#0F172A") }
        val paintGray  = Paint().apply { textSize = 10f; color = android.graphics.Color.parseColor("#64748B") }
        val paintGreen = Paint().apply { textSize = 11f; isFakeBoldText = true; color = android.graphics.Color.parseColor("#16A34A") }
        val paintRed   = Paint().apply { textSize = 11f; isFakeBoldText = true; color = android.graphics.Color.parseColor("#DC2626") }
        val paintOrange= Paint().apply { textSize = 11f; isFakeBoldText = true; color = android.graphics.Color.parseColor("#EA580C") }
        val linePaint  = Paint().apply { color = android.graphics.Color.parseColor("#E2E8F0"); strokeWidth = 1f }

        var y = 50f

        // Logo / Encabezado
        canvas.drawText("DentalPro", 40f, y, paintTitle)
        y += 20f
        canvas.drawText("Reporte: ${tipo.titulo}", 40f, y, paintSub)
        y += 15f
        canvas.drawText("Generado: $fecha", 40f, y, paintGray)
        y += 10f
        canvas.drawLine(40f, y, 555f, y, linePaint)
        y += 20f

        when (tipo) {
            TipoReporte.GENERAL -> {
                val r = resumen ?: ResumenGeneral()
                fun row(label: String, value: String, paint: Paint = paintBody) {
                    canvas.drawText(label, 40f, y, paintGray)
                    canvas.drawText(value, 350f, y, paint)
                    y += 18f
                }
                canvas.drawText("RESUMEN FINANCIERO", 40f, y, paintHead); y += 20f
                row("Ingresos totales",    "Bs. ${"%,d".format(r.ingresosTotales)}", paintGreen)
                row("Ingresos este mes",   "Bs. ${"%,d".format(r.ingresosEsteMes)}", paintGreen)
                y += 10f
                canvas.drawText("PEDIDOS", 40f, y, paintHead); y += 20f
                row("Total pedidos",       "${r.totalPedidos}")
                row("Completados",         "${r.pedidosCompletados}", paintGreen)
                row("Pendientes",          "${r.pedidosPendientes}", paintOrange)
                y += 10f
                canvas.drawText("INVENTARIO", 40f, y, paintHead); y += 20f
                row("Total productos",     "${r.totalProductos}")
                row("Sin stock",           "${r.productosSinStock}", paintRed)
                row("Stock bajo (≤5)",     "${r.productosStockBajo}", paintOrange)
                y += 10f
                canvas.drawText("CLIENTES / SOLICITUDES", 40f, y, paintHead); y += 20f
                row("Total clientes",      "${r.totalClientes}")
                row("Solicitudes pend.",   "${r.solicitudesPendientes}", paintOrange)

                // Mini tabla ventas mensuales
                if (ventas.isNotEmpty()) {
                    y += 20f
                    canvas.drawLine(40f, y, 555f, y, linePaint); y += 14f
                    canvas.drawText("VENTAS MENSUALES (ÚLTIMOS MESES)", 40f, y, paintHead); y += 20f
                    ventas.takeLast(6).forEach { vm ->
                        canvas.drawText("${vm.mes} ${vm.anio}", 40f, y, paintBody)
                        canvas.drawText("${vm.cantidadPedidos} pedidos", 200f, y, paintGray)
                        canvas.drawText("Bs. ${"%,d".format(vm.totalVentas)}", 350f, y, paintGreen)
                        y += 18f
                    }
                }
            }

            TipoReporte.VENTAS_MENSUALES -> {
                canvas.drawText("MES", 40f, y, paintHead)
                canvas.drawText("AÑO", 130f, y, paintHead)
                canvas.drawText("PEDIDOS", 210f, y, paintHead)
                canvas.drawText("INGRESOS", 350f, y, paintHead)
                y += 16f
                canvas.drawLine(40f, y, 555f, y, linePaint); y += 14f
                ventas.reversed().forEach { vm ->
                    canvas.drawText(vm.mes, 40f, y, paintBody)
                    canvas.drawText("${vm.anio}", 130f, y, paintBody)
                    canvas.drawText("${vm.cantidadPedidos}", 210f, y, paintBody)
                    canvas.drawText("Bs. ${"%,d".format(vm.totalVentas)}", 350f, y, paintGreen)
                    y += 18f
                    if (y > 800f) return@forEach
                }
            }

            TipoReporte.STOCK_BAJO -> {
                canvas.drawText("PRODUCTO", 40f, y, paintHead)
                canvas.drawText("CATEGORÍA", 260f, y, paintHead)
                canvas.drawText("STOCK", 420f, y, paintHead)
                canvas.drawText("PRECIO", 490f, y, paintHead)
                y += 16f
                canvas.drawLine(40f, y, 555f, y, linePaint); y += 14f
                alertas.forEach { a ->
                    val nombre = if (a.nombre.length > 28) a.nombre.take(25) + "..." else a.nombre
                    canvas.drawText(nombre, 40f, y, paintBody)
                    canvas.drawText(a.categoria.take(18), 260f, y, paintGray)
                    canvas.drawText(if (a.stock == 0L) "Sin stock" else "${a.stock}", 420f, y,
                        if (a.stock == 0L) paintRed else paintOrange)
                    canvas.drawText("Bs.${a.precio}", 490f, y, paintBody)
                    y += 18f
                    if (y > 800f) return@forEach
                }
            }

            TipoReporte.MAS_VENDIDOS -> {
                canvas.drawText("#", 40f, y, paintHead)
                canvas.drawText("PRODUCTO", 70f, y, paintHead)
                canvas.drawText("CANT.", 380f, y, paintHead)
                canvas.drawText("INGRESOS", 440f, y, paintHead)
                y += 16f
                canvas.drawLine(40f, y, 555f, y, linePaint); y += 14f
                topProductos.forEachIndexed { i, pv ->
                    canvas.drawText("${i + 1}", 40f, y, paintBody)
                    val nombre = if (pv.nombre.length > 32) pv.nombre.take(29) + "..." else pv.nombre
                    canvas.drawText(nombre, 70f, y, paintBody)
                    canvas.drawText("${pv.cantidadVendida}", 380f, y, paintBody)
                    canvas.drawText("Bs.${"%,d".format(pv.ingresos)}", 440f, y, paintGreen)
                    y += 18f
                    if (y > 800f) return@forEachIndexed
                }
            }
        }

        // Pie de página
        val footerPaint = Paint().apply { textSize = 9f; color = android.graphics.Color.LTGRAY }
        canvas.drawText("DentalPro — Reporte generado automáticamente — $fecha", 40f, 820f, footerPaint)

        pdf.finishPage(page)

        // Guardar en caché
        val fileName = "DentalPro_${tipo.name}_${System.currentTimeMillis()}.pdf"
        val file     = File(context.cacheDir, fileName)
        file.outputStream().use { pdf.writeTo(it) }
        pdf.close()

        FileProvider.getUriForFile(context, "${context.packageName}.provider", file)

    } catch (e: Exception) {
        android.util.Log.e("PDF", "Error generando PDF: ${e.message}")
        null
    }
}