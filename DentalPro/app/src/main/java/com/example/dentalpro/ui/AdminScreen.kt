package com.example.dentalpro.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

private val NavyDark    = Color(0xFF0D1B2A)
private val NavyMid     = Color(0xFF1B2B3D)
private val AccentBlue  = Color(0xFF2563EB)
private val GreenOk     = Color(0xFF16A34A)
private val GreenPastel = Color(0xFFDCFCE7)
private val RedDark     = Color(0xFFDC2626)
private val RedPastel   = Color(0xFFFFEBEE)
private val OrangeWarn  = Color(0xFFEA580C)
private val OrangePast  = Color(0xFFFFF3E0)
private val White       = Color(0xFFFFFFFF)
private val BgGray      = Color(0xFFF1F5F9)
private val TextGray    = Color(0xFF101011)
private val BorderColor = Color(0xFFE2E8F0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    onBack: () -> Unit = {},
    onNewProduct: () -> Unit,
    onEditProduct: (ProductoFirestore) -> Unit,
    onManageUsers: () -> Unit,
    onManageOrders: () -> Unit,
    onManageSolicitudes: () -> Unit,        // ← nuevo parámetro
    onManageQr: () -> Unit,                 // ← QR de pago
    onManageDashboard: () -> Unit,          // ← Dashboard
    onManageReports: () -> Unit,            // ← Reportes
    onLogout: () -> Unit
) {
    val scope        = rememberCoroutineScope()
    var productos    by remember { mutableStateOf<List<ProductoFirestore>>(emptyList()) }
    var filtered     by remember { mutableStateOf<List<ProductoFirestore>>(emptyList()) }
    var searchQuery  by remember { mutableStateOf("") }
    var isLoading    by remember { mutableStateOf(true) }
    var deleteTarget by remember { mutableStateOf<ProductoFirestore?>(null) }
    var showLogout   by remember { mutableStateOf(false) }
    var snackbarMsg  by remember { mutableStateOf<String?>(null) }
    val snackbarHost = remember { SnackbarHostState() }

    val adminName = FirebaseAuth.getInstance().currentUser?.displayName
        ?: FirebaseAuth.getInstance().currentUser?.email?.substringBefore("@") ?: "Admin"

    fun reload() {
        scope.launch {
            isLoading = true
            productos = FirestoreRepository.getProductos()
            filtered  = productos
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { reload() }

    LaunchedEffect(searchQuery) {
        filtered = if (searchQuery.isBlank()) productos
        else productos.filter { it.nombre.contains(searchQuery, ignoreCase = true) }
    }

    LaunchedEffect(snackbarMsg) {
        snackbarMsg?.let { snackbarHost.showSnackbar(it); snackbarMsg = null }
    }

    // Dialogo cerrar sesion
    if (showLogout) {
        AlertDialog(
            onDismissRequest = { showLogout = false },
            containerColor   = White,
            shape            = RoundedCornerShape(20.dp),
            title = { Text("Cerrar sesion",color = Color.Black, fontWeight = FontWeight.Bold) },
            text  = { Text("Estas seguro que deseas salir del panel admin?", color = TextGray) },
            confirmButton = {
                Button(
                    onClick = { FirebaseAuth.getInstance().signOut(); showLogout = false; onLogout() },
                    colors   = ButtonDefaults.buttonColors(containerColor = RedDark),
                    shape    = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Salir", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                OutlinedButton(
                    onClick  = { showLogout = false },
                    shape    = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Cancelar", color = TextGray)}
            }
        )
    }

    // Dialogo eliminar producto
    deleteTarget?.let { producto ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            containerColor   = White,
            shape            = RoundedCornerShape(20.dp),
            icon = {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(RedPastel, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = RedDark, modifier = Modifier.size(28.dp))
                }
            },
            title = { Text("Eliminar producto", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            text  = {
                Text(
                    "Esta accion eliminara \"${producto.nombre}\" permanentemente y no se puede deshacer.",
                    fontSize = 14.sp, color = TextGray, lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            val ok = FirestoreRepository.eliminarProducto(producto.id)
                            deleteTarget = null
                            snackbarMsg  = if (ok) "Producto eliminado" else "Error al eliminar"
                            if (ok) reload()
                        }
                    },
                    colors   = ButtonDefaults.buttonColors(containerColor = RedDark),
                    shape    = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Eliminar", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                OutlinedButton(
                    onClick  = { deleteTarget = null },
                    shape    = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Cancelar") }
            }
        )
    }

    Scaffold(
        containerColor = BgGray,
        snackbarHost   = { SnackbarHost(snackbarHost) },
        floatingActionButton = {
            FloatingActionButton(
                onClick        = onNewProduct,
                containerColor = AccentBlue,
                contentColor   = White,
                shape          = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nuevo producto", modifier = Modifier.size(26.dp))
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {

            // Header oscuro con saludo y acciones
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.linearGradient(listOf(NavyDark, NavyMid)))
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("DentalPro", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = White)
                            Text("Hola, $adminName", fontSize = 13.sp, color = White.copy(alpha = 0.7f))
                        }
                        IconButton(onClick = { showLogout = true }) {
                            Icon(Icons.Default.ExitToApp, contentDescription = "Salir", tint = Color.Red)
                        }
                    }

                    // Fila 1: Pedidos + Usuarios
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick  = onManageOrders,
                            modifier = Modifier.weight(1f),
                            shape    = RoundedCornerShape(10.dp),
                            colors   = ButtonDefaults.outlinedButtonColors(contentColor = White),
                            border   = BorderStroke(1.dp, White.copy(alpha = 0.4f))
                        ) {
                            Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Pedidos", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                        OutlinedButton(
                            onClick  = onManageUsers,
                            modifier = Modifier.weight(1f),
                            shape    = RoundedCornerShape(10.dp),
                            colors   = ButtonDefaults.outlinedButtonColors(contentColor = White),
                            border   = BorderStroke(1.dp, White.copy(alpha = 0.4f))
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Usuarios", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    // Fila 2: Solicitudes (ancho completo)
                    OutlinedButton(
                        onClick  = onManageSolicitudes,
                        modifier = Modifier.fillMaxWidth(),
                        shape    = RoundedCornerShape(10.dp),
                        colors   = ButtonDefaults.outlinedButtonColors(contentColor = White),
                        border   = BorderStroke(1.dp, White.copy(alpha = 0.4f))
                    ) {
                        Icon(Icons.Default.Inbox, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Solicitudes especiales", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }

                    // Fila 3: QR de pago
                    OutlinedButton(
                        onClick  = onManageQr,
                        modifier = Modifier.fillMaxWidth(),
                        shape    = RoundedCornerShape(10.dp),
                        colors   = ButtonDefaults.outlinedButtonColors(contentColor = White),
                        border   = BorderStroke(1.dp, White.copy(alpha = 0.4f))
                    ) {
                        Text("📱", fontSize = 15.sp)
                        Spacer(Modifier.width(6.dp))
                        Text("Gestionar QR de pago", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }

                    // Fila 4: Dashboard + Reportes
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick  = onManageDashboard,
                            modifier = Modifier.weight(1f),
                            shape    = RoundedCornerShape(10.dp),
                            colors   = ButtonDefaults.outlinedButtonColors(contentColor = White),
                            border   = BorderStroke(1.dp, White.copy(alpha = 0.4f))
                        ) {
                            Text("📊", fontSize = 15.sp)
                            Spacer(Modifier.width(6.dp))
                            Text("Dashboard", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                        OutlinedButton(
                            onClick  = onManageReports,
                            modifier = Modifier.weight(1f),
                            shape    = RoundedCornerShape(10.dp),
                            colors   = ButtonDefaults.outlinedButtonColors(contentColor = White),
                            border   = BorderStroke(1.dp, White.copy(alpha = 0.4f))
                        ) {
                            Text("📋", fontSize = 15.sp)
                            Spacer(Modifier.width(6.dp))
                            Text("Reportes", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // Barra busqueda + boton nuevo
            Column(
                modifier = Modifier.fillMaxWidth().background(White).padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Productos", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value         = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder   = { Text("Buscar productos...", fontSize = 13.sp, color = TextGray) },
                        leadingIcon   = { Icon(Icons.Default.Search, contentDescription = null, tint = TextGray, modifier = Modifier.size(18.dp)) },
                        modifier      = Modifier.weight(1f).height(48.dp),
                        shape         = RoundedCornerShape(10.dp),
                        singleLine    = true,
                        colors        = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor      = AccentBlue,
                            unfocusedBorderColor    = BorderColor,
                            unfocusedContainerColor = BgGray,
                            focusedContainerColor   = White
                        )
                    )
                    Button(
                        onClick        = onNewProduct,
                        colors         = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                        shape          = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        modifier       = Modifier.height(48.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Nuevo", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            HorizontalDivider(color = BorderColor)

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AccentBlue)
                }
            } else {
                LazyColumn(
                    modifier       = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    // Stats
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            StatChip(modifier = Modifier.weight(1f), label = "Total",     value = productos.size.toString(),                        color = AccentBlue)
                            StatChip(modifier = Modifier.weight(1f), label = "Sin stock", value = productos.count { it.stock == 0L }.toString(),    color = RedDark)
                            StatChip(modifier = Modifier.weight(1f), label = "Stock bajo",value = productos.count { it.stock in 1..5 }.toString(),  color = OrangeWarn)
                        }
                    }

                    if (filtered.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("📦", fontSize = 48.sp)
                                    Text("No se encontraron productos", fontSize = 14.sp, color = TextGray)
                                }
                            }
                        }
                    } else {
                        items(filtered, key = { it.id }) { producto ->
                            AdminProductRow(
                                producto = producto,
                                onEdit   = { onEditProduct(producto) },
                                onDelete = { deleteTarget = producto }
                            )
                            HorizontalDivider(color = BorderColor)
                        }
                    }

                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}

@Composable
fun StatChip(modifier: Modifier = Modifier, label: String, value: String, color: Color) {
    Card(
        modifier  = modifier,
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = color)
            Text(label, fontSize = 11.sp, color = TextGray)
        }
    }
}

@Composable
fun AdminProductRow(
    producto: ProductoFirestore,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val sinStock  = producto.stock == 0L
    val stockBajo = producto.stock in 1..5

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(White)
            .padding(horizontal = 4.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier.size(52.dp).clip(RoundedCornerShape(12.dp)).background(AccentBlue),
            contentAlignment = Alignment.Center
        ) { Text("🦷", fontSize = 26.sp) }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(producto.nombre, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(producto.categoria, fontSize = 11.sp, color = TextGray)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Bs. ${producto.precio}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AccentBlue)
                Box(
                    modifier = Modifier
                        .background(
                            when { sinStock -> RedPastel; stockBajo -> OrangePast; else -> GreenPastel },
                            RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(
                        "Stock: ${producto.stock}",
                        fontSize = 10.sp, fontWeight = FontWeight.Medium,
                        color = when { sinStock -> RedDark; stockBajo -> OrangeWarn; else -> GreenOk }
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            IconButton(onClick = onEdit, modifier = Modifier.size(36.dp).background(BgGray, RoundedCornerShape(10.dp))) {
                Icon(Icons.Default.Edit, contentDescription = "Editar", tint = AccentBlue, modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp).background(RedPastel, RoundedCornerShape(10.dp))) {
                Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = RedDark, modifier = Modifier.size(18.dp))
            }
        }
    }
}