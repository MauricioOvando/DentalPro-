package com.example.dentalpro.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

// ── Cloudinary config ─────────────────────────────────────────────────────────
private const val CLOUDINARY_CLOUD_NAME  = "do34x6bsw"
private const val CLOUDINARY_UPLOAD_PRESET = "dentalpro_upload"
private const val CLOUDINARY_UPLOAD_URL  =
    "https://api.cloudinary.com/v1_1/$CLOUDINARY_CLOUD_NAME/image/upload"

// ── Colores ───────────────────────────────────────────────────────────────────
private val NavyDark    = Color(0xFF238486)
private val NavyMid     = Color(0xFF040F5E)
private val AccentBlue  = Color(0xFF2563EB)
private val BluePastel  = Color(0xFFEFF6FF)
private val White       = Color(0xFFFFFFFF)
private val BgGray      = Color(0xFFF1F5F9)
private val TextGray    = Color(0xFF64748B)
private val BorderColor = Color(0xFFE2E8F0)
private val TextDark    = Color(0xFF838A8C)
private val RedDark     = Color(0xFFDC2626)
private val RedPastel   = Color(0xFFFFEBEE)

// ── Subida a Cloudinary (suspend, hilo IO) ────────────────────────────────────
suspend fun subirImagenACloudinary(
    context: android.content.Context,
    uri: Uri
): String? = withContext(Dispatchers.IO) {
    return@withContext try {
        val bytes = context.contentResolver.openInputStream(uri)?.readBytes()
            ?: return@withContext null

        val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
        val extension = when (mimeType) {
            "image/png"  -> "png"
            "image/webp" -> "webp"
            else         -> "jpg"
        }

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "file",
                "producto.$extension",
                bytes.toRequestBody(mimeType.toMediaTypeOrNull())
            )
            .addFormDataPart("upload_preset", CLOUDINARY_UPLOAD_PRESET)
            .build()

        val request = Request.Builder()
            .url(CLOUDINARY_UPLOAD_URL)
            .post(requestBody)
            .build()

        val client   = OkHttpClient()
        val response = client.newCall(request).execute()
        val body     = response.body?.string() ?: return@withContext null

        if (response.isSuccessful) {
            JSONObject(body).getString("secure_url")
        } else {
            android.util.Log.e("CLOUDINARY", "Error: $body")
            null
        }
    } catch (e: Exception) {
        android.util.Log.e("CLOUDINARY", "Excepción: ${e.message}")
        null
    }
}

// ── Pantalla ──────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminProductFormScreen(
    productoExistente: ProductoFirestore? = null,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val isEditing = productoExistente != null
    val scope     = rememberCoroutineScope()
    val context   = LocalContext.current

    var nombre         by remember { mutableStateOf(productoExistente?.nombre ?: "") }
    var categoria      by remember { mutableStateOf(productoExistente?.categoria ?: "") }
    var descripcion    by remember { mutableStateOf(productoExistente?.descripcion ?: "") }
    var precio         by remember { mutableStateOf(productoExistente?.precio?.toString() ?: "") }
    var precioOriginal by remember { mutableStateOf(productoExistente?.precioOriginal?.toString() ?: "") }
    var descuento      by remember { mutableStateOf(productoExistente?.descuento?.toString() ?: "0") }
    var stock          by remember { mutableStateOf(productoExistente?.stock?.toString() ?: "") }
    var destacado      by remember { mutableStateOf(productoExistente?.destacado ?: false) }

    // ── Estado imagen ─────────────────────────────────────────────────────────
    // imageUrl contiene la URL actual (existente o recién subida)
    var imageUrl       by remember { mutableStateOf(productoExistente?.imageUrl ?: "") }
    // localUri es la URI local seleccionada antes de subir
    var localUri       by remember { mutableStateOf<Uri?>(null) }
    var isUploadingImg by remember { mutableStateOf(false) }
    var uploadError    by remember { mutableStateOf<String?>(null) }

    // Lanzador del selector de imágenes
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            localUri = uri
            uploadError = null
            isUploadingImg = true
            scope.launch {
                val url = subirImagenACloudinary(context, uri)
                isUploadingImg = false
                if (url != null) {
                    imageUrl = url
                } else {
                    uploadError = "No se pudo subir la imagen. Intenta de nuevo."
                    localUri = null
                }
            }
        }
    }

    // Estado categorías
    var categorias         by remember { mutableStateOf<List<CategoriaFirestore>>(emptyList()) }
    var loadingCategorias  by remember { mutableStateOf(true) }
    var showNuevaCatDialog by remember { mutableStateOf(false) }

    var isLoading    by remember { mutableStateOf(false) }
    var errorMsg     by remember { mutableStateOf<String?>(null) }
    val snackbarHost = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        categorias = FirestoreRepository.getCategorias()
        loadingCategorias = false
    }

    LaunchedEffect(errorMsg) {
        errorMsg?.let { snackbarHost.showSnackbar(it); errorMsg = null }
    }

    // ── Diálogo nueva categoría ───────────────────────────────────────────────
    if (showNuevaCatDialog) {
        NuevaCategoriaDialog(
            onDismiss = { showNuevaCatDialog = false },
            onSave    = { nombreCat, emoji ->
                scope.launch {
                    val ok = FirestoreRepository.agregarCategoria(nombreCat, emoji)
                    if (ok) {
                        categorias = FirestoreRepository.getCategorias()
                        categoria  = nombreCat
                    } else {
                        errorMsg = "Error al guardar la categoría"
                    }
                    showNuevaCatDialog = false
                }
            }
        )
    }

    Scaffold(
        containerColor = BgGray,
        snackbarHost   = { SnackbarHost(snackbarHost) }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {

            // Header oscuro
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
                        if (isEditing) "Editar Producto" else "Nuevo Producto",
                        fontSize = 18.sp, fontWeight = FontWeight.Bold, color = White
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {

                // ── Selector de imagen ────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(White)
                        .border(
                            width = 2.dp,
                            color = if (uploadError != null) RedDark else BorderColor,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable(enabled = !isUploadingImg) {
                            imagePickerLauncher.launch("image/*")
                        },
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        isUploadingImg -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(color = AccentBlue, modifier = Modifier.size(32.dp))
                                Text("Subiendo imagen...", fontSize = 12.sp, color = TextGray)
                            }
                        }
                        imageUrl.isNotBlank() -> {
                            // Imagen subida con éxito → mostrar preview
                            AsyncImage(
                                model             = imageUrl,
                                contentDescription = "Imagen del producto",
                                contentScale      = ContentScale.Crop,
                                modifier          = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp))
                            )
                            // Overlay para cambiar imagen
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(16.dp)),
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                Text(
                                    "Toca para cambiar",
                                    fontSize = 11.sp,
                                    color = White,
                                    modifier = Modifier.padding(bottom = 10.dp)
                                )
                            }
                        }
                        else -> {
                            // Estado vacío
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("📷", fontSize = 36.sp)
                                Text(
                                    "Toca para seleccionar imagen",
                                    fontSize = 13.sp,
                                    color = TextGray,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    "JPG, PNG — se sube a Cloudinary",
                                    fontSize = 10.sp,
                                    color = TextGray.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }

                // Error de subida de imagen
                uploadError?.let { err ->
                    Text(
                        err,
                        color = RedDark,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(RedPastel, RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    )
                }

                // Nombre
                FormFieldNew(
                    label = "Nombre del producto", value = nombre,
                    onValueChange = { nombre = it }, placeholder = "Ej: Cepillo Dental Suave"
                )

                // ── Selector de Categoría ─────────────────────────────────────
                var dropdownExpanded by remember { mutableStateOf(false) }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Categoría", fontSize = 13.sp, color = Color(0xFF374151), fontWeight = FontWeight.Medium)
                        OutlinedButton(
                            onClick = { showNuevaCatDialog = true },
                            modifier = Modifier.height(32.dp),
                            shape    = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                            colors   = ButtonDefaults.outlinedButtonColors(contentColor = AccentBlue)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Nueva categoría", modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Nueva", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }

                    if (loadingCategorias) {
                        Box(modifier = Modifier.fillMaxWidth().height(52.dp), contentAlignment = Alignment.CenterStart) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = AccentBlue)
                        }
                    } else {
                        ExposedDropdownMenuBox(
                            expanded        = dropdownExpanded,
                            onExpandedChange = { dropdownExpanded = !dropdownExpanded }
                        ) {
                            OutlinedTextField(
                                value         = if (categoria.isBlank()) "" else categoria,
                                onValueChange = {},
                                readOnly      = true,
                                placeholder   = { Text("Selecciona una categoría", fontSize = 13.sp, color = Color(0xFF9CA3AF)) },
                                trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                                modifier      = Modifier.fillMaxWidth().menuAnchor(),
                                shape         = RoundedCornerShape(10.dp),
                                colors        = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor      = AccentBlue,
                                    unfocusedBorderColor    = BorderColor,
                                    unfocusedContainerColor = White,
                                    focusedContainerColor   = White,
                                    focusedTextColor        = Color(0xFF0F172A),
                                    unfocusedTextColor      = Color(0xFF0F172A)
                                )
                            )

                            ExposedDropdownMenu(
                                expanded        = dropdownExpanded,
                                onDismissRequest = { dropdownExpanded = false }
                            ) {
                                if (categorias.isEmpty()) {
                                    DropdownMenuItem(
                                        text    = { Text("Sin categorías — toca + Nueva", fontSize = 13.sp, color = TextGray) },
                                        onClick = { dropdownExpanded = false }
                                    )
                                } else {
                                    categorias.forEach { cat ->
                                        DropdownMenuItem(
                                            text = {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                ) {
                                                    if (cat.emoji.isNotBlank()) Text(cat.emoji, fontSize = 18.sp)
                                                    Text(
                                                        cat.nombre,
                                                        fontSize   = 14.sp,
                                                        fontWeight = if (categoria == cat.nombre) FontWeight.Bold else FontWeight.Normal,
                                                        color      = if (categoria == cat.nombre) AccentBlue else TextDark
                                                    )
                                                }
                                            },
                                            onClick = {
                                                categoria        = cat.nombre
                                                dropdownExpanded = false
                                            },
                                            trailingIcon = {
                                                if (categoria == cat.nombre) {
                                                    Text("✓", fontSize = 14.sp, color = AccentBlue, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Precio y Stock en fila
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        FormFieldNew(label = "Precio (Bs.)", value = precio, onValueChange = { precio = it },
                            placeholder = "45.00", isNumber = true, prefix = "Bs. ")
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        FormFieldNew(label = "Stock", value = stock, onValueChange = { stock = it },
                            placeholder = "150", isNumber = true)
                    }
                }

                // Precio original y descuento en fila
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        FormFieldNew(label = "Precio original", value = precioOriginal,
                            onValueChange = { precioOriginal = it }, placeholder = "55.00 (opcional)", isNumber = true)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        FormFieldNew(label = "Descuento (%)", value = descuento,
                            onValueChange = { descuento = it }, placeholder = "0", isNumber = true)
                    }
                }

                // Descripcion
                FormFieldNew(label = "Descripcion", value = descripcion, onValueChange = { descripcion = it },
                    placeholder = "Descripcion del producto...", maxLines = 3)

                // Switch destacado
                Card(
                    shape     = RoundedCornerShape(12.dp),
                    colors    = CardDefaults.cardColors(containerColor = White),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Producto destacado", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextDark)
                            Text("Aparece en la seccion principal", fontSize = 11.sp, color = TextGray)
                        }
                        Switch(
                            checked = destacado, onCheckedChange = { destacado = it },
                            colors  = SwitchDefaults.colors(checkedThumbColor = White, checkedTrackColor = AccentBlue)
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))

                // Botón guardar
                Button(
                    onClick = {
                        when {
                            nombre.isBlank()    -> errorMsg = "El nombre es obligatorio"
                            categoria.isBlank() -> errorMsg = "Selecciona o crea una categoría"
                            precio.isBlank()    -> errorMsg = "El precio es obligatorio"
                            stock.isBlank()     -> errorMsg = "El stock es obligatorio"
                            isUploadingImg      -> errorMsg = "Espera a que termine la subida de imagen"
                            else -> {
                                isLoading = true
                                scope.launch {
                                    val producto = ProductoFirestore(
                                        id             = productoExistente?.id ?: "",
                                        nombre         = nombre.trim(),
                                        categoria      = categoria.trim(),
                                        descripcion    = descripcion.trim(),
                                        precio         = precio.toLongOrNull() ?: 0,
                                        precioOriginal = precioOriginal.toLongOrNull() ?: 0,
                                        descuento      = descuento.toLongOrNull() ?: 0,
                                        stock          = stock.toLongOrNull() ?: 0,
                                        destacado      = destacado,
                                        imageUrl       = imageUrl          // ← guarda la URL de Cloudinary
                                    )
                                    val ok = if (isEditing)
                                        FirestoreRepository.editarProducto(producto)
                                    else
                                        FirestoreRepository.agregarProducto(producto)

                                    isLoading = false
                                    if (ok) onSaved()
                                    else errorMsg = "Error al guardar. Intenta de nuevo."
                                }
                            }
                        }
                    },
                    modifier  = Modifier.fillMaxWidth().height(52.dp),
                    shape     = RoundedCornerShape(14.dp),
                    colors    = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                    enabled   = !isLoading && !isUploadingImg
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = White)
                    } else {
                        Text(
                            if (isEditing) "Actualizar Producto" else "Guardar Producto",
                            fontSize = 15.sp, fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

// ── Diálogo nueva categoría ───────────────────────────────────────────────────
@Composable
fun NuevaCategoriaDialog(
    onDismiss: () -> Unit,
    onSave: (nombre: String, emoji: String) -> Unit
) {
    var nombre   by remember { mutableStateOf("") }
    var emoji    by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val emojisSugeridos = listOf("🦷","🪥","💊","🧴","🩺","🔬","💉","🧪","🩹","✨")

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = White)) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier.size(40.dp).background(BluePastel, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) { Text("🏷️", fontSize = 20.sp) }
                    Column {
                        Text("Nueva Categoría", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextDark)
                        Text("Se guardará en Firestore", fontSize = 11.sp, color = TextGray)
                    }
                }

                HorizontalDivider(color = BorderColor)

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Ícono (toca para seleccionar)", fontSize = 12.sp, color = TextGray, fontWeight = FontWeight.Medium)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        emojisSugeridos.take(5).forEach { e ->
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .border(
                                        width = if (emoji == e) 2.dp else 1.dp,
                                        color = if (emoji == e) AccentBlue else BorderColor,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .background(if (emoji == e) BluePastel else White, RoundedCornerShape(8.dp))
                                    .clickable { emoji = e },
                                contentAlignment = Alignment.Center
                            ) { Text(e, fontSize = 18.sp) }
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        emojisSugeridos.drop(5).forEach { e ->
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .border(
                                        width = if (emoji == e) 2.dp else 1.dp,
                                        color = if (emoji == e) AccentBlue else BorderColor,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .background(if (emoji == e) BluePastel else White, RoundedCornerShape(8.dp))
                                    .clickable { emoji = e },
                                contentAlignment = Alignment.Center
                            ) { Text(e, fontSize = 18.sp) }
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Nombre de la categoría *", fontSize = 12.sp, color = TextGray, fontWeight = FontWeight.Medium)
                    OutlinedTextField(
                        value         = nombre,
                        onValueChange = { nombre = it; errorMsg = null },
                        placeholder   = { Text("Ej: Higiene Bucal", fontSize = 13.sp, color = Color(0xFF9CA3AF)) },
                        modifier      = Modifier.fillMaxWidth(),
                        shape         = RoundedCornerShape(10.dp),
                        singleLine    = true,
                        colors        = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor      = AccentBlue,
                            unfocusedBorderColor    = BorderColor,
                            unfocusedContainerColor = White,
                            focusedContainerColor   = White,
                            focusedTextColor        = Color(0xFF0F172A),
                            unfocusedTextColor      = Color(0xFF0F172A)
                        )
                    )
                }

                errorMsg?.let { msg ->
                    Text(
                        msg, color = RedDark, fontSize = 12.sp,
                        modifier = Modifier.fillMaxWidth()
                            .background(RedPastel, RoundedCornerShape(8.dp)).padding(10.dp)
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = onDismiss, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp)
                    ) { Text("Cancelar") }
                    Button(
                        onClick = {
                            if (nombre.isBlank()) errorMsg = "El nombre es obligatorio"
                            else onSave(nombre.trim(), emoji)
                        },
                        modifier = Modifier.weight(1f),
                        shape    = RoundedCornerShape(10.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                    ) { Text("Guardar", fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

// ── Campo de texto reutilizable ───────────────────────────────────────────────
@Composable
fun FormFieldNew(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    isNumber: Boolean = false,
    maxLines: Int = 1,
    prefix: String = ""
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, fontSize = 13.sp, color = Color(0xFF374151), fontWeight = FontWeight.Medium)
        OutlinedTextField(
            value         = value,
            onValueChange = onValueChange,
            placeholder   = { Text(placeholder, fontSize = 13.sp, color = Color(0xFF9CA3AF)) },
            prefix        = if (prefix.isNotEmpty()) { { Text(prefix, fontSize = 13.sp, color = Color(0xFF6B7280)) } } else null,
            modifier      = Modifier.fillMaxWidth(),
            shape         = RoundedCornerShape(10.dp),
            singleLine    = maxLines == 1,
            maxLines      = maxLines,
            keyboardOptions = if (isNumber) KeyboardOptions(keyboardType = KeyboardType.Number) else KeyboardOptions.Default,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor      = Color(0xFF2563EB),
                unfocusedBorderColor    = Color(0xFFE2E8F0),
                unfocusedContainerColor = Color(0xFFFFFFFF),
                focusedContainerColor   = Color(0xFFFFFFFF),
                focusedTextColor        = Color(0xFF0F172A),
                unfocusedTextColor      = Color(0xFF0F172A)
            )
        )
    }
}