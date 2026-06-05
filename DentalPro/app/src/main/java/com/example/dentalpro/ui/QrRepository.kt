package com.example.dentalpro.ui

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Base64

// ─────────────────────────────────────────────────────────────────────────────
// CLOUDINARY
// ─────────────────────────────────────────────────────────────────────────────
private const val CLOUDINARY_CLOUD_NAME = "do34x6bsw"
private const val CLOUDINARY_UPLOAD_PRESET = "dentalpro_upload"

// ─────────────────────────────────────────────────────────────────────────────
// FIRESTORE MODEL
// ─────────────────────────────────────────────────────────────────────────────
data class QrConfig(
    val imageUrl: String = "",
    val updatedAt: com.google.firebase.Timestamp =
        com.google.firebase.Timestamp.now()
)

// ─────────────────────────────────────────────────────────────────────────────
// REPOSITORY
// ─────────────────────────────────────────────────────────────────────────────
object QrRepository {

    private val db = Firebase.firestore

    private val qrDoc =
        db.collection("config")
            .document("qr_pago")

    // ─────────────────────────────────────────────────────────────────────────
    // OBTENER QR
    // ─────────────────────────────────────────────────────────────────────────
    suspend fun getQrConfig(): QrConfig? {

        return try {

            val doc = qrDoc.get().await()

            if (doc.exists()) {

                QrConfig(
                    imageUrl =
                        doc.getString("imageUrl") ?: "",

                    updatedAt =
                        doc.getTimestamp("updatedAt")
                            ?: com.google.firebase.Timestamp.now()
                )

            } else {
                null
            }

        } catch (e: Exception) {

            Log.e(
                "QR",
                "Error leyendo QR: ${e.message}"
            )

            null
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SUBIR NUEVO QR
    // ─────────────────────────────────────────────────────────────────────────
    suspend fun subirNuevoQr(
        context: Context,
        uri: Uri
    ): Result<String> {

        return withContext(Dispatchers.IO) {

            try {

                val base64 =
                    uriToBase64(context, uri)
                        ?: return@withContext Result.failure(
                            Exception("No se pudo leer la imagen")
                        )

                // ─────────────────────────────────────
                // SUBIR A CLOUDINARY
                // ─────────────────────────────────────
                val uploadedUrl = uploadToCloudinary(base64)

                // ─────────────────────────────────────
                // GUARDAR EN FIRESTORE
                // ─────────────────────────────────────
                qrDoc.set(
                    mapOf(
                        "imageUrl" to uploadedUrl,

                        "updatedAt" to
                                com.google.firebase.Timestamp.now()
                    )
                ).await()

                Result.success(uploadedUrl)

            } catch (e: Exception) {

                Log.e(
                    "QR",
                    "Error subiendo QR: ${e.message}"
                )

                Result.failure(e)
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SUBIR COMPROBANTE
    // ─────────────────────────────────────────────────────────────────────────
    suspend fun subirComprobanteYGuardar(
        context: Context,
        uri: Uri,
        tipo: ComprobanteDestino,
        documentId: String
    ): Result<String> {

        return withContext(Dispatchers.IO) {

            try {

                val base64 =
                    uriToBase64(context, uri)
                        ?: return@withContext Result.failure(
                            Exception("No se pudo leer comprobante")
                        )

                val uploadedUrl =
                    uploadToCloudinary(base64)

                db.collection(tipo.coleccion)
                    .document(documentId)
                    .update(
                        mapOf(
                            "comprobanteUrl" to uploadedUrl,

                            "fechaComprobante" to
                                    com.google.firebase.Timestamp.now()
                        )
                    )
                    .await()

                Result.success(uploadedUrl)

            } catch (e: Exception) {

                Log.e(
                    "QR",
                    "Error subiendo comprobante: ${e.message}"
                )

                Result.failure(e)
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GUARDAR EN GALERÍA
    // ─────────────────────────────────────────────────────────────────────────
    suspend fun guardarQrEnGaleria(
        context: Context,
        imageUrl: String
    ): Result<Unit> {

        return withContext(Dispatchers.IO) {

            try {

                val bitmap =
                    descargarBitmap(imageUrl)
                        ?: return@withContext Result.failure(
                            Exception("No se pudo descargar imagen")
                        )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

                    val values = ContentValues().apply {

                        put(
                            MediaStore.Images.Media.DISPLAY_NAME,
                            "QR_DentalPro_${System.currentTimeMillis()}.jpg"
                        )

                        put(
                            MediaStore.Images.Media.MIME_TYPE,
                            "image/jpeg"
                        )

                        put(
                            MediaStore.Images.Media.RELATIVE_PATH,
                            Environment.DIRECTORY_PICTURES +
                                    "/DentalPro"
                        )
                    }

                    val resolver = context.contentResolver

                    val uri = resolver.insert(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        values
                    )
                        ?: return@withContext Result.failure(
                            Exception("Error creando archivo")
                        )

                    resolver.openOutputStream(uri)?.use { out ->

                        bitmap.compress(
                            Bitmap.CompressFormat.JPEG,
                            95,
                            out
                        )
                    }

                } else {

                    @Suppress("DEPRECATION")
                    val dir =
                        Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_PICTURES
                        )

                    val file = java.io.File(
                        dir,
                        "QR_DentalPro_${System.currentTimeMillis()}.jpg"
                    )

                    java.io.FileOutputStream(file).use { out ->

                        bitmap.compress(
                            Bitmap.CompressFormat.JPEG,
                            95,
                            out
                        )
                    }
                }

                Result.success(Unit)

            } catch (e: Exception) {

                Log.e(
                    "QR",
                    "Error guardando QR: ${e.message}"
                )

                Result.failure(e)
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // URI → BASE64
    // ─────────────────────────────────────────────────────────────────────────
    private fun uriToBase64(
        context: Context,
        uri: Uri
    ): String? {

        return try {

            val inputStream =
                context.contentResolver.openInputStream(uri)
                    ?: return null

            val bytes = inputStream.readBytes()

            inputStream.close()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                Base64.getEncoder()
                    .encodeToString(bytes)

            } else {

                android.util.Base64.encodeToString(
                    bytes,
                    android.util.Base64.NO_WRAP
                )
            }

        } catch (e: Exception) {

            Log.e(
                "QR",
                "Error convirtiendo Base64: ${e.message}"
            )

            null
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CLOUDINARY UNSIGNED UPLOAD
    // ─────────────────────────────────────────────────────────────────────────
    private fun uploadToCloudinary(
        base64: String
    ): String {

        val url = URL(
            "https://api.cloudinary.com/v1_1/" +
                    "$CLOUDINARY_CLOUD_NAME/image/upload"
        )

        val conn =
            url.openConnection() as HttpURLConnection

        conn.requestMethod = "POST"

        conn.setRequestProperty(
            "Content-Type",
            "application/json"
        )

        conn.doOutput = true

        conn.connectTimeout = 30000
        conn.readTimeout = 30000

        // ─────────────────────────────────────
        // BODY JSON
        // SOLO PARÁMETROS PERMITIDOS
        // ─────────────────────────────────────
        val body = JSONObject().apply {

            put(
                "file",
                "data:image/jpeg;base64,$base64"
            )

            put(
                "upload_preset",
                CLOUDINARY_UPLOAD_PRESET
            )

            // IMPORTANTE:
            // evita error "Display name can't contain slashes"
            put(
                "filename_override",
                "qr_pago.jpg"
            )
        }

        conn.outputStream.use {

            it.write(
                body.toString().toByteArray()
            )
        }

        val responseCode = conn.responseCode

        if (responseCode != 200) {

            val errorBody =
                conn.errorStream
                    ?.bufferedReader()
                    ?.readText()
                    ?: "sin detalle"

            throw Exception(
                "Cloudinary error $responseCode: $errorBody"
            )
        }

        val response =
            conn.inputStream
                .bufferedReader()
                .readText()

        val json = JSONObject(response)

        return json.getString("secure_url")
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DESCARGAR BITMAP
    // ─────────────────────────────────────────────────────────────────────────
    private fun descargarBitmap(
        url: String
    ): Bitmap? {

        return try {

            val conn =
                URL(url).openConnection() as HttpURLConnection

            conn.connectTimeout = 15000
            conn.readTimeout = 15000

            conn.connect()

            val bitmap =
                BitmapFactory.decodeStream(conn.inputStream)

            conn.disconnect()

            bitmap

        } catch (e: Exception) {

            Log.e(
                "QR",
                "Error descargando bitmap: ${e.message}"
            )

            null
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// DESTINO COMPROBANTE
// ─────────────────────────────────────────────────────────────────────────────
enum class ComprobanteDestino(
    val coleccion: String
) {

    PEDIDO("pedidos"),

    SOLICITUD("solicitudes")
}