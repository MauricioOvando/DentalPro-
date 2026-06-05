package com.example.dentalpro.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

private val GreenDark    = Color(0xFF1B5E20)
private val GreenMid     = Color(0xFF2E7D32)
private val GreenLight   = Color(0xFF4CAF50)
private val GreenPastel  = Color(0xFFE8F5E9)
private val White        = Color(0xFFFFFFFF)
private val BgColor      = Color(0xFFF0F4F0)
private val GrayText     = Color(0xFF546E7A)
private val OrangeOff    = Color(0xFFEF6C00)
private val CardBg       = Color(0xFFFFFFFF)
private val DividerColor = Color(0xFFCFD8DC)

// ── Helper: muestra imagen de Cloudinary o emoji de fallback ──────────────────
@Composable
fun ProductImage(
    imageUrl: String,
    modifier: Modifier = Modifier,
    fallbackEmoji: String = "🦷",
    fallbackFontSize: androidx.compose.ui.unit.TextUnit = 40.sp,
    fallbackBg: Color = GreenPastel
) {
    if (imageUrl.isNotBlank()) {
        AsyncImage(
            model              = imageUrl,
            contentDescription = "Imagen del producto",
            contentScale       = ContentScale.Crop,
            modifier           = modifier
        )
    } else {
        Box(
            modifier         = modifier.background(fallbackBg),
            contentAlignment = Alignment.Center
        ) {
            Text(fallbackEmoji, fontSize = fallbackFontSize)
        }
    }
}

// ── HomeScreen ────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    cartViewModel: CartViewModel,
    onProductClick: (ProductoFirestore) -> Unit,
    onGoToCart: () -> Unit,
    onGoToProfile: () -> Unit,
    onGoToAdmin: () -> Unit = {},
    onGoToNotificaciones: () -> Unit = {},
    onGoToSolicitudes: () -> Unit = {}
) {
    var searchQuery      by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Todos") }
    var productos        by remember { mutableStateOf<List<ProductoFirestore>>(emptyList()) }
    var categorias       by remember { mutableStateOf<List<CategoriaFirestore>>(emptyList()) }
    var isLoading        by remember { mutableStateOf(true) }
    var isAdmin          by remember { mutableStateOf(false) }
    var badgeCount       by remember { mutableIntStateOf(0) }
    val scope            = rememberCoroutineScope()
    val cartCount        = cartViewModel.totalItems

    LaunchedEffect(Unit) {
        scope.launch {
            productos  = FirestoreRepository.getProductos()
            categorias = FirestoreRepository.getCategorias()
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (uid != null) {
                val usuario = FirestoreRepository.getUsuario(uid)
                isAdmin    = usuario?.rol == "admin"
                badgeCount = SolicitudRepository.contarNoLeidas(uid)
            }
            isLoading = false
        }
    }

    val filteredProducts = productos.filter { p ->
        val matchCategory = selectedCategory == "Todos" || p.categoria == selectedCategory
        val matchSearch   = searchQuery.isBlank() || p.nombre.contains(searchQuery, ignoreCase = true)
        val tieneStock    = p.stock > 0
        matchCategory && matchSearch && tieneStock
    }

    Scaffold(
        containerColor = BgColor,
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value         = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder   = { Text("Buscar productos...", fontSize = 13.sp, color = GrayText) },
                        leadingIcon   = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = GreenMid)
                        },
                        modifier   = Modifier.fillMaxWidth().height(52.dp),
                        shape      = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor      = GreenMid,
                            unfocusedBorderColor    = DividerColor,
                            unfocusedContainerColor = Color(0xFFF5F5F5),
                            focusedContainerColor   = White
                        )
                    )
                },
                actions = {
                    // Ícono de notificaciones con badge
                    BadgedBox(
                        badge = {
                            if (badgeCount > 0) Badge { Text("$badgeCount") }
                        },
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        IconButton(onClick = onGoToNotificaciones) {
                            Icon(
                                Icons.Default.Notifications,
                                contentDescription = "Notificaciones",
                                tint = GreenDark
                            )
                        }
                    }

                    // Carrito
                    Box(modifier = Modifier.padding(end = 8.dp)) {
                        IconButton(onClick = onGoToCart) {
                            Icon(Icons.Default.ShoppingCart, contentDescription = "Carrito", tint = GreenDark)
                        }
                        if (cartCount > 0) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .background(OrangeOff, CircleShape)
                                    .align(Alignment.TopEnd),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(cartCount.toString(), fontSize = 9.sp, color = White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = White)
            )
        },
        bottomBar = {
            NavigationBar(containerColor = White, tonalElevation = 4.dp) {
                NavigationBarItem(
                    selected = true,  onClick = {},
                    icon     = { Text("🏠", fontSize = 18.sp) },
                    label    = { Text("Inicio", fontSize = 11.sp, color = Color.Black) }
                )
                NavigationBarItem(
                    selected = false, onClick = onGoToSolicitudes,
                    icon     = { Text("📋", fontSize = 18.sp) },
                    label    = { Text("Pedido esp.", fontSize = 11.sp, color = Color.Black) }
                )
                if (isAdmin) {
                    NavigationBarItem(
                        selected = false, onClick = onGoToAdmin,
                        icon     = { Text("⚙️", fontSize = 18.sp) },
                        label    = { Text("Admin", fontSize = 11.sp, color = Color.Black) }
                    )
                }
                NavigationBarItem(
                    selected = false, onClick = onGoToProfile,
                    icon     = { Text("👤", fontSize = 18.sp) },
                    label    = { Text("Cuenta", fontSize = 11.sp, color = Color.Black) }
                )
            }
        }
    ) { innerPadding ->
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = GreenMid)
                    Spacer(Modifier.height(12.dp))
                    Text("Cargando productos...", color = GrayText, fontSize = 14.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                // Categorías
                item {
                    Card(
                        modifier  = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                        shape     = RoundedCornerShape(16.dp),
                        colors    = CardDefaults.cardColors(containerColor = CardBg),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Categorias", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = GreenDark)
                                TextButton(onClick = { selectedCategory = "Todos" }) {
                                    Text("Ver todas", fontSize = 12.sp, color = GreenMid)
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                items(categorias) { cat ->
                                    CategoryChipHome(
                                        nombre   = cat.nombre,
                                        emoji    = cat.emoji,
                                        selected = selectedCategory == cat.nombre,
                                        onClick  = {
                                            selectedCategory =
                                                if (selectedCategory == cat.nombre) "Todos" else cat.nombre
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Banner oferta
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Brush.linearGradient(listOf(GreenDark, GreenLight)))
                            .padding(20.dp)
                    ) {
                        Column {
                            Text("Ofertas Especiales",  fontSize = 14.sp, fontWeight = FontWeight.Bold, color = White)
                            Text("Hasta 20% OFF",       fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = White)
                            Text("en resinas dentales", fontSize = 12.sp, color = White.copy(alpha = 0.85f))
                        }
                        Box(modifier = Modifier.align(Alignment.CenterEnd)) {
                            Text("🦷", fontSize = 48.sp)
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }

                // Título productos
                item {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Productos", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = GreenDark)
                        Text("${filteredProducts.size} productos", fontSize = 12.sp, color = GrayText)
                    }
                }

                // Grid de productos
                items(filteredProducts.chunked(2)) { rowProducts ->
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowProducts.forEach { producto ->
                            ProductCardHome(
                                producto    = producto,
                                modifier    = Modifier.weight(1f),
                                onCardClick = { onProductClick(producto) },
                                onAddToCart = { cartViewModel.agregarProducto(producto) }
                            )
                        }
                        if (rowProducts.size == 1) Spacer(Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(10.dp))
                }

                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

// ── CategoryChip ──────────────────────────────────────────────────────────────
@Composable
fun CategoryChipHome(
    nombre: String,
    emoji: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(if (selected) GreenMid else Color(0xFFE8F5E9)),
            contentAlignment = Alignment.Center
        ) {
            Text(emoji, fontSize = 22.sp)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            nombre,
            fontSize   = 10.sp,
            color      = if (selected) GreenMid else GrayText,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

// ── ProductCard ───────────────────────────────────────────────────────────────
@Composable
fun ProductCardHome(
    producto: ProductoFirestore,
    modifier: Modifier = Modifier,
    onCardClick: () -> Unit,
    onAddToCart: () -> Unit
) {
    Card(
        modifier  = modifier.clickable { onCardClick() },
        shape     = RoundedCornerShape(14.dp),
        colors    = CardDefaults.cardColors(containerColor = White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            if (producto.descuento > 0) {
                Box(
                    modifier = Modifier
                        .background(OrangeOff, RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("${producto.descuento}%", fontSize = 10.sp, color = White, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(4.dp))
            }

            ProductImage(
                imageUrl         = producto.imageUrl,
                fallbackFontSize = 40.sp,
                fallbackBg       = GreenPastel,
                modifier         = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .clip(RoundedCornerShape(10.dp))
            )

            Spacer(Modifier.height(8.dp))
            Text(
                producto.nombre,
                fontSize   = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines   = 2,
                overflow   = TextOverflow.Ellipsis,
                color      = Color(0xFF263238)
            )
            Spacer(Modifier.height(2.dp))
            Text(producto.categoria, fontSize = 10.sp, color = GrayText)
            Spacer(Modifier.height(4.dp))

            if (producto.precioOriginal > 0 && producto.precioOriginal != producto.precio) {
                Text(
                    "Bs. ${producto.precioOriginal}",
                    fontSize = 10.sp,
                    color    = GrayText,
                    textDecoration = TextDecoration.LineThrough
                )
            }
            Text("Bs. ${producto.precio}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = GreenDark)

            if (producto.stock in 1..5) {
                Text("Solo ${producto.stock} en stock", fontSize = 9.sp, color = OrangeOff, fontWeight = FontWeight.Medium)
            }

            Spacer(Modifier.height(8.dp))
            Button(
                onClick        = onAddToCart,
                modifier       = Modifier.fillMaxWidth().height(34.dp),
                shape          = RoundedCornerShape(10.dp),
                colors         = ButtonDefaults.buttonColors(containerColor = GreenMid),
                contentPadding = PaddingValues(0.dp),
                enabled        = producto.stock > 0
            ) {
                Text(
                    if (producto.stock > 0) "Añadir" else "Sin stock",
                    fontSize   = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}