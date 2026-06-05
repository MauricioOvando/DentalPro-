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
import androidx.compose.material.icons.filled.Edit
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
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val NavyDark    = Color(0xFF0D1B2A)
private val AccentBlue  = Color(0xFF2563EB)
private val GreenOk     = Color(0xFF16A34A)
private val GreenPastel = Color(0xFFDCFCE7)
private val RedDark     = Color(0xFFDC2626)
private val White       = Color(0xFFFFFFFF)
private val BgGray      = Color(0xFFF1F5F9)
private val TextGray    = Color(0xFF64748B)
private val BorderColor = Color(0xFFE2E8F0)
private val TextDark    = Color(0xFF0F172A)

// ─────────────────────────────────────────────────────────────────────────────
// AdminQrScreen
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminQrScreen(onBack: () -> Unit) {

    val context      = LocalContext.current
    val scope        = rememberCoroutineScope()
    val snackbarHost = remember { SnackbarHostState() }

    var qrConfig by remember { mutableStateOf<QrConfig?>(null) }

    var isLoading by remember {
        mutableStateOf(true)
    }

    var isUploading by remember {
        mutableStateOf(false)
    }

    // ─────────────────────────────────────────────────────────────
    // PERMISOS
    // ─────────────────────────────────────────────────────────────
    var permisoConcedido by remember {
        mutableStateOf(false)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        permisoConcedido = granted
    }

    // ─────────────────────────────────────────────────────────────
    // SELECTOR DE IMAGEN
    // ─────────────────────────────────────────────────────────────
    val imagePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->

        if (uri != null) {

            scope.launch {

                isUploading = true

                val result = QrRepository.subirNuevoQr(
                    context,
                    uri
                )

                isUploading = false

                if (result.isSuccess) {

                    snackbarHost.showSnackbar(
                        "✅ QR actualizado correctamente"
                    )

                    // RECARGAR QR NUEVO
                    qrConfig = QrRepository.getQrConfig()

                } else {

                    snackbarHost.showSnackbar(
                        "❌ Error: ${result.exceptionOrNull()?.message}"
                    )
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // CARGAR QR ACTUAL
    // ─────────────────────────────────────────────────────────────
    LaunchedEffect(Unit) {

        isLoading = true

        qrConfig = QrRepository.getQrConfig()

        isLoading = false

        val permission =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_IMAGES
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }

        permissionLauncher.launch(permission)
    }

    val fmtDate = SimpleDateFormat(
        "dd MMM yyyy HH:mm",
        Locale("es")
    )

    Scaffold(
        containerColor = BgGray,

        snackbarHost = {
            SnackbarHost(snackbarHost)
        },

        topBar = {

            TopAppBar(

                title = {
                    Text(
                        text = "QR de Pago",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.Black
                    )
                },

                navigationIcon = {

                    IconButton(
                        onClick = onBack
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color.Black
                        )
                    }
                },

                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = White
                )
            )
        }

    ) { padding ->

        Column(

            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),

            verticalArrangement = Arrangement.spacedBy(20.dp),

            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // ─────────────────────────────────────────────────────
            // TARJETA INFO
            // ─────────────────────────────────────────────────────
            Card(
                shape = RoundedCornerShape(16.dp),

                colors = CardDefaults.cardColors(
                    containerColor = White
                ),

                elevation = CardDefaults.cardElevation(
                    defaultElevation = 2.dp
                )
            ) {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),

                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {

                    Text(
                        text = "📱 Imagen QR activa",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )

                    Text(
                        text =
                            "Esta imagen se muestra a los clientes cuando realizan un pago. " +
                                    "Puedes reemplazarla en cualquier momento.",
                        fontSize = 13.sp,
                        color = TextGray,
                        lineHeight = 20.sp
                    )
                }
            }

            // ─────────────────────────────────────────────────────
            // QR ACTUAL
            // ─────────────────────────────────────────────────────
            Card(
                shape = RoundedCornerShape(16.dp),

                colors = CardDefaults.cardColors(
                    containerColor = White
                ),

                elevation = CardDefaults.cardElevation(
                    defaultElevation = 2.dp
                )
            ) {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),

                    horizontalAlignment = Alignment.CenterHorizontally,

                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {

                    when {

                        isLoading -> {

                            Box(
                                modifier = Modifier
                                    .size(220.dp)
                                    .background(
                                        BgGray,
                                        RoundedCornerShape(12.dp)
                                    ),

                                contentAlignment = Alignment.Center
                            ) {

                                CircularProgressIndicator(
                                    color = AccentBlue
                                )
                            }
                        }

                        qrConfig?.imageUrl.isNullOrBlank() -> {

                            Box(
                                modifier = Modifier
                                    .size(220.dp)
                                    .background(
                                        BgGray,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .border(
                                        2.dp,
                                        BorderColor,
                                        RoundedCornerShape(12.dp)
                                    ),

                                contentAlignment = Alignment.Center
                            ) {

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,

                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {

                                    Text(
                                        "📷",
                                        fontSize = 40.sp
                                    )

                                    Text(
                                        text = "Sin QR configurado",
                                        fontSize = 13.sp,
                                        color = TextGray,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }

                        else -> {

                            AsyncImage(

                                model = ImageRequest.Builder(context)

                                    .data(qrConfig!!.imageUrl)

                                    .crossfade(true)

                                    // DESACTIVAR CACHE
                                    .memoryCachePolicy(
                                        CachePolicy.DISABLED
                                    )

                                    .diskCachePolicy(
                                        CachePolicy.DISABLED
                                    )

                                    .build(),

                                contentDescription = "QR de pago",

                                contentScale = ContentScale.Fit,

                                modifier = Modifier
                                    .size(220.dp)
                                    .clip(
                                        RoundedCornerShape(12.dp)
                                    )
                                    .border(
                                        2.dp,
                                        AccentBlue,
                                        RoundedCornerShape(12.dp)
                                    )
                            )

                            qrConfig?.updatedAt?.let { ts ->

                                Text(
                                    text =
                                        "Última actualización: ${
                                            fmtDate.format(
                                                Date(ts.seconds * 1000)
                                            )
                                        }",

                                    fontSize = 11.sp,

                                    color = TextGray
                                )
                            }
                        }
                    }

                    // ─────────────────────────────────────────────
                    // BOTÓN SUBIR
                    // ─────────────────────────────────────────────
                    Button(

                        onClick = {
                            imagePickerLauncher.launch("image/*")
                        },

                        enabled = !isUploading && !isLoading,

                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),

                        shape = RoundedCornerShape(12.dp),

                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentBlue
                        )
                    ) {

                        if (isUploading) {

                            Row(
                                verticalAlignment = Alignment.CenterVertically,

                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {

                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = White
                                )

                                Text(
                                    text = "Subiendo imagen...",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                        } else {

                            Row(
                                verticalAlignment = Alignment.CenterVertically,

                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {

                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )

                                Text(
                                    text =
                                        if (qrConfig?.imageUrl.isNullOrBlank()) {
                                            "Subir imagen QR"
                                        } else {
                                            "Reemplazar QR"
                                        },

                                    fontSize = 15.sp,

                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            // ─────────────────────────────────────────────────────
            // AVISO
            // ─────────────────────────────────────────────────────
            Card(
                shape = RoundedCornerShape(12.dp),

                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFF8E1)
                )
            ) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),

                    horizontalArrangement = Arrangement.spacedBy(10.dp),

                    verticalAlignment = Alignment.Top
                ) {

                    Text(
                        "⚠️",
                        fontSize = 16.sp
                    )

                    Text(
                        text =
                            "Al subir una nueva imagen, el QR anterior será reemplazado " +
                                    "visualmente para todos los clientes.",

                        fontSize = 12.sp,

                        color = Color(0xFF6D4C00),

                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}