package com.example.dentalpro.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

private val BlueDeep   = Color(0xFF1565C0)
private val BlueMid    = Color(0xFF1E88E5)
private val BlueLight  = Color(0xFFE3F2FD)
private val White      = Color(0xFFFFFFFF)
private val GrayBg     = Color(0xFFF5F7FA)
private val GrayText   = Color(0xFF78909C)
private val TextDark   = Color(0xFF1A2332)
private val GreenOk    = Color(0xFF2E7D32)
private val RedOff     = Color(0xFFE53935)
private val OrangeWarn = Color(0xFFEF6C00)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    producto: ProductoFirestore,
    cartViewModel: CartViewModel,
    onBack: () -> Unit,
    onGoToCart: () -> Unit,
    onSolicitar: () -> Unit = {}
) {
    var cantidad      by remember { mutableIntStateOf(1) }
    var cantidadText  by remember { mutableStateOf("1") }
    var agregado      by remember { mutableStateOf(false) }

    val tieneDescuento = producto.descuento > 0
    val hayStock       = producto.stock > 0
    val stockMax       = producto.stock.toInt().coerceAtLeast(1)
    val enLimiteStock  = hayStock && cantidad >= stockMax

    // Sincroniza cantidadText → cantidad al perder foco o confirmar
    fun aplicarCantidadTexto() {
        val parsed = cantidadText.toIntOrNull() ?: 1
        val clamped = parsed.coerceIn(1, if (hayStock) stockMax else parsed.coerceAtLeast(1))
        cantidad     = clamped
        cantidadText = clamped.toString()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalle", fontWeight = FontWeight.SemiBold, fontSize = 16.sp,color = Color.Black) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = Color.Black)
                    }
                },
                actions = {
                    BadgedBox(
                        badge = {
                            if (cartViewModel.totalItems > 0) {
                                Badge { Text("${cartViewModel.totalItems}") }
                            }
                        }
                    ) {
                        IconButton(onClick = onGoToCart) {
                            Icon(Icons.Default.ShoppingCart, contentDescription = "Carrito", tint = Color.Black)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = White)
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp, color = White) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // ── Selector de cantidad ──────────────────────────────────
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Cantidad", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextDark)
                            if (hayStock) {
                                Text(
                                    "Disponible: ${(stockMax - cantidad).coerceAtLeast(0)} restantes",
                                    fontSize = 10.sp,
                                    color    = if (enLimiteStock) OrangeWarn else TextDark
                                )
                            }
                        }
                        Row(
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            FilledTonalIconButton(
                                onClick  = {
                                    if (cantidad > 1) {
                                        cantidad--
                                        cantidadText = cantidad.toString()
                                    }
                                },
                                modifier = Modifier.size(34.dp),
                                colors   = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = BlueLight
                                )
                            ) {
                                Text("−", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = BlueDeep)
                            }

                            // Campo editable por teclado
                            OutlinedTextField(
                                value         = cantidadText,
                                onValueChange = { v: String ->
                                    if (v.length <= 3 && v.all { ch -> ch.isDigit() }) {
                                        cantidadText = v
                                        val parsed = v.toIntOrNull() ?: return@OutlinedTextField
                                        val clamped = parsed.coerceIn(1, if (hayStock) stockMax else parsed.coerceAtLeast(1))
                                        cantidad = clamped
                                    }
                                },
                                modifier        = Modifier.width(62.dp),
                                singleLine      = true,
                                textStyle       = LocalTextStyle.current.copy(
                                    fontSize   = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign  = TextAlign.Center,
                                    color      = if (enLimiteStock) OrangeWarn else TextDark
                                ),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape  = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor   = BlueDeep,
                                    unfocusedBorderColor = BlueMid
                                )
                            )

                            FilledTonalIconButton(
                                onClick  = {
                                    if (!hayStock || cantidad < stockMax) {
                                        cantidad++
                                        cantidadText = cantidad.toString()
                                    }
                                },
                                enabled  = !hayStock || !enLimiteStock,
                                modifier = Modifier.size(34.dp),
                                colors   = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor         = BlueLight,
                                    disabledContainerColor = Color(0xFFEEEEEE)
                                )
                            ) {
                                Text(
                                    "+",
                                    fontSize   = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color      = if (enLimiteStock) GrayText else BlueDeep
                                )
                            }
                        }
                    }

                    // ── Total ─────────────────────────────────────────────────
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Text("Total:", fontSize = 13.sp, color = TextDark)
                        Text(
                            "Bs ${producto.precio * cantidad}",
                            fontSize   = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color      = BlueDeep
                        )
                    }

                    if (!hayStock) {
                        Button(
                            onClick  = { onSolicitar() },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape    = RoundedCornerShape(14.dp),
                            colors   = ButtonDefaults.buttonColors(containerColor = OrangeWarn)
                        ) {
                            Text("📋  Solicitar este producto", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        }
                    } else {
                        Button(
                            onClick = {
                                aplicarCantidadTexto()
                                repeat(cantidad) { cartViewModel.agregarProducto(producto) }
                                agregado = true
                            },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape    = RoundedCornerShape(14.dp),
                            colors   = ButtonDefaults.buttonColors(
                                containerColor = if (agregado) GreenOk else BlueDeep
                            )
                        ) {
                            Text(
                                if (agregado) "✓ Agregado al carrito" else "Agregar al carrito",
                                fontSize   = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(GrayBg)
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // ── Imagen ────────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp),
                contentAlignment = Alignment.Center
            ) {
                if (producto.imageUrl.isNotBlank()) {
                    AsyncImage(
                        model              = producto.imageUrl,
                        contentDescription = producto.nombre,
                        contentScale       = ContentScale.Crop,
                        modifier           = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, GrayBg),
                                    startY = 160f,
                                    endY   = 350f
                                )
                            )
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.verticalGradient(listOf(BlueLight, White))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🦷", fontSize = 96.sp)
                    }
                }

                // Badge descuento
                if (tieneDescuento) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                            .background(RedOff, RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("-${producto.descuento}%", color = White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Badge sin stock
                if (!hayStock) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(16.dp)
                            .background(OrangeWarn, RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("Sin stock", color = White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // ── Contenido ─────────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(White)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Categoría
                Box(
                    modifier = Modifier
                        .background(BlueLight, RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(producto.categoria, fontSize = 11.sp, color = BlueMid, fontWeight = FontWeight.SemiBold)
                }

                // Nombre
                Text(producto.nombre, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextDark, lineHeight = 28.sp)

                // Precios
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Bs ${producto.precio}", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = BlueDeep)
                    if (tieneDescuento && producto.precioOriginal > 0) {
                        Text(
                            "Bs ${producto.precioOriginal}",
                            fontSize       = 16.sp,
                            color          = GrayText,
                            textDecoration = TextDecoration.LineThrough
                        )
                    }
                }

                // Stock
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(if (hayStock) GreenOk else OrangeWarn, CircleShape)
                    )
                    Text(
                        if (hayStock) "${producto.stock} unidades disponibles"
                        else "Sin stock — puedes solicitarlo",
                        fontSize   = 12.sp,
                        color      = if (hayStock) GreenOk else OrangeWarn,
                        fontWeight = FontWeight.Medium
                    )
                }

                Divider(color = Color(0xFFF0F0F0))

                // Descripción
                if (producto.descripcion.isNotBlank()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Descripción", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextDark)
                        Text(producto.descripcion, fontSize = 14.sp, color = GrayText, lineHeight = 22.sp)
                    }
                }

                Spacer(Modifier.height(8.dp))
            }
        }
    }
}