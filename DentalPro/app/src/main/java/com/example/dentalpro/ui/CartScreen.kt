package com.example.dentalpro.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val BlueDeep  = Color(0xFF1565C0)
private val BlueLight = Color(0xFFE3F2FD)
private val White     = Color(0xFFFFFFFF)
private val GrayBg    = Color(0xFFF5F7FA)
private val GrayText  = Color(0xFF78909C)
private val TextDark  = Color(0xFF1A2332)
private val RedOff    = Color(0xFFE53935)
private val GreenOk   = Color(0xFF2E7D32)
private val OrangeWarn= Color(0xFFEF6C00)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    cartViewModel: CartViewModel,
    onBack: () -> Unit,
    onCheckout: () -> Unit
) {
    val items = cartViewModel.items
    val total = cartViewModel.total

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Mi Carrito", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver",tint = Color.Black)
                    }
                },
                actions = {
                    if (items.isNotEmpty()) {
                        TextButton(onClick = { cartViewModel.vaciarCarrito() }) {
                            Text("Vaciar", color = RedOff, fontSize = 13.sp)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = White)
            )
        },
        // El bottomBar usa WindowInsets para respetar la barra de navegación del celular
        bottomBar = {
            if (items.isNotEmpty()) {
                Surface(
                    shadowElevation = 8.dp,
                    color = White
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()  // ← respeta botones de navegación
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Subtotal (${cartViewModel.totalItems} items)",
                                fontSize = 13.sp,
                                color = GrayText
                            )
                            Text(
                                "Bs. ${"%.0f".format(total)}",
                                fontSize = 13.sp,
                                color = GrayText
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Total",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDark
                            )
                            Text(
                                "Bs. ${"%.0f".format(total)}",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = BlueDeep
                            )
                        }
                        Button(
                            onClick = onCheckout,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape  = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BlueDeep)
                        ) {
                            Text(
                                "Proceder al pago",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        if (items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(GrayBg)
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("🛒", fontSize = 64.sp)
                    Text(
                        "Tu carrito esta vacio",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextDark
                    )
                    Text(
                        "Agrega productos para continuar",
                        fontSize = 14.sp,
                        color = GrayText
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = onBack,
                        shape  = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BlueDeep)
                    ) {
                        Text("Ver productos", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(GrayBg)
                    .padding(padding),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                items(items, key = { it.producto.id }) { item ->
                    CartItemCard(
                        item          = item,
                        onIncrease    = { cartViewModel.agregarProducto(item.producto) },
                        onDecrease    = { cartViewModel.quitarProducto(item.producto) },
                        onDelete      = { cartViewModel.eliminarProducto(item.producto) },
                        onSetCantidad = { nueva -> cartViewModel.setCantidad(item.producto, nueva) }
                    )
                }
            }
        }
    }
}

@Composable
fun CartItemCard(
    item: CartItem,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onDelete: () -> Unit,
    onSetCantidad: (Int) -> Unit = {}
) {
    val stockMax      = item.producto.stock.toInt().coerceAtLeast(1)
    val enLimite      = item.cantidad >= stockMax
    val stockRestante = (stockMax - item.cantidad).coerceAtLeast(0)

    // Estado local de texto para edición por teclado
    var cantidadText by remember(item.cantidad) { mutableStateOf(item.cantidad.toString()) }

    Card(
        shape     = RoundedCornerShape(14.dp),
        colors    = CardDefaults.cardColors(containerColor = White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Imagen placeholder
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(BlueLight, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("🦷", fontSize = 32.sp)
            }

            // Info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    item.producto.nombre,
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = TextDark,
                    maxLines   = 2,
                    overflow   = TextOverflow.Ellipsis
                )
                Text(item.producto.categoria, fontSize = 11.sp, color = GrayText)
                Text(
                    "Bs. ${item.producto.precio * item.cantidad}",
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color      = BlueDeep
                )

                // Stock disponible
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(
                                when {
                                    item.producto.stock == 0L -> RedOff
                                    stockRestante <= 2        -> OrangeWarn
                                    else                      -> GreenOk
                                },
                                androidx.compose.foundation.shape.CircleShape
                            )
                    )
                    Text(
                        when {
                            item.producto.stock == 0L -> "Sin stock"
                            enLimite                  -> "Límite alcanzado (stock: $stockMax)"
                            stockRestante <= 2        -> "Solo $stockRestante disponibles"
                            else                      -> "Stock disponible: $stockRestante"
                        },
                        fontSize   = 10.sp,
                        color      = when {
                            item.producto.stock == 0L -> RedOff
                            stockRestante <= 2        -> OrangeWarn
                            else                      -> GreenOk
                        },
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Controles cantidad + eliminar
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                IconButton(
                    onClick  = onDelete,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Eliminar",
                        tint     = RedOff,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Row(
                    verticalAlignment      = Alignment.CenterVertically,
                    horizontalArrangement  = Arrangement.spacedBy(6.dp)
                ) {
                    FilledTonalIconButton(
                        onClick  = onDecrease,
                        modifier = Modifier.size(28.dp),
                        colors   = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = BlueLight
                        )
                    ) {
                        Text("−", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = BlueDeep)
                    }

                    // Campo editable por teclado
                    OutlinedTextField(
                        value         = cantidadText,
                        onValueChange = { v: String ->
                            if (v.length <= 3 && v.all { ch -> ch.isDigit() }) {
                                cantidadText = v
                                val parsed  = v.toIntOrNull() ?: return@OutlinedTextField
                                val clamped = parsed.coerceIn(1, stockMax)
                                if (clamped != item.cantidad) onSetCantidad(clamped)
                            }
                        },
                        modifier        = Modifier
                            .width(52.dp)
                            .padding(0.dp),
                        singleLine      = true,
                        textStyle       = LocalTextStyle.current.copy(
                            fontSize   = 13.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign  = TextAlign.Center,
                            color      = TextDark
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape           = RoundedCornerShape(8.dp),
                        colors          = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = BlueDeep,
                            unfocusedBorderColor = BlueLight
                        )
                    )

                    FilledTonalIconButton(
                        onClick  = onIncrease,
                        modifier = Modifier.size(28.dp),
                        enabled  = !enLimite,
                        colors   = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor         = BlueLight,
                            disabledContainerColor = Color(0xFFEEEEEE)
                        )
                    ) {
                        Text(
                            "+",
                            fontSize   = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color      = if (enLimite) GrayText else BlueDeep
                        )
                    }
                }
            }
        }
    }
}