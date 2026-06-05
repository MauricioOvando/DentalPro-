package com.example.dentalpro.ui

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

// ── Estados del flujo ─────────────────────────────────────────────────────────
//
//  pendiente   → el cliente acaba de enviar la solicitud, admin aún no la revisa
//  aceptado    → admin confirmó que puede conseguir el producto
//  rechazado   → admin no puede conseguirlo (o venció el plazo de 7 días)
//  pago_pendiente → admin marcó aceptado Y está esperando que el cliente pague
//  pagado      → cliente notificó que pagó (admin debe confirmar)
//  completado  → admin confirmó el pago; pedido finalizado
//  vencido     → pasaron 7 días sin respuesta del admin

object EstadoSolicitud {
    const val PENDIENTE       = "pendiente"
    const val ACEPTADO        = "aceptado"
    const val RECHAZADO       = "rechazado"
    const val PAGO_PENDIENTE  = "pago_pendiente"
    const val PAGADO          = "pagado"          // cliente dice que pagó
    const val COMPLETADO      = "completado"      // admin confirma pago
    const val VENCIDO         = "vencido"
}

data class SolicitudFirestore(
    val id: String = "",
    val usuarioId: String = "",
    val nombreUsuario: String = "",
    val emailUsuario: String = "",
    val productoId: String = "",
    val nombreProducto: String = "",
    val categoria: String = "",
    val precioReferencia: Long = 0,
    val cantidad: Int = 1,
    val nota: String = "",                    // nota opcional del cliente
    val estado: String = EstadoSolicitud.PENDIENTE,
    val fechaSolicitud: Timestamp = Timestamp.now(),
    val fechaLimite: Timestamp = Timestamp(          // 7 días después
        Timestamp.now().seconds + TimeUnit.DAYS.toSeconds(7), 0
    ),
    val fechaRespuesta: Timestamp? = null,    // cuando admin acepta/rechaza
    val fechaPago: Timestamp? = null,         // cuando cliente notifica pago
    val fechaCompletado: Timestamp? = null,   // cuando admin confirma pago
    val motivoRechazo: String = "",           // razón del rechazo (opcional)
    val notaAdmin: String = "",               // instrucciones de pago del admin
    val precioAdmin: Long = 0L,               // precio final definido por el admin al aceptar
    val leidoPorCliente: Boolean = false,     // para el badge de notificaciones
    val comprobanteUrl: String = "",          // URL del comprobante subido por el cliente
    val fechaComprobante: Timestamp? = null   // cuándo se subió el comprobante
)

object SolicitudRepository {

    private val db = Firebase.firestore
    private val col = db.collection("solicitudes")

    // ── Helpers de mapeo ──────────────────────────────────────────────────────
    private fun mapSolicitud(
        doc: com.google.firebase.firestore.DocumentSnapshot
    ): SolicitudFirestore {
        val ahora = Timestamp.now()
        val fechaSol = doc.getTimestamp("fechaSolicitud") ?: ahora
        // límite = 7 días desde la fecha de solicitud (o el campo guardado)
        val fechaLimDefault = Timestamp(
            fechaSol.seconds + TimeUnit.DAYS.toSeconds(7), 0
        )
        return SolicitudFirestore(
            id               = doc.id,
            usuarioId        = doc.getString("usuarioId") ?: "",
            nombreUsuario    = doc.getString("nombreUsuario") ?: "",
            emailUsuario     = doc.getString("emailUsuario") ?: "",
            productoId       = doc.getString("productoId") ?: "",
            nombreProducto   = doc.getString("nombreProducto") ?: "",
            categoria        = doc.getString("categoria") ?: "",
            precioReferencia = doc.getLong("precioReferencia") ?: 0,
            cantidad         = (doc.getLong("cantidad") ?: 1).toInt(),
            nota             = doc.getString("nota") ?: "",
            estado           = doc.getString("estado") ?: EstadoSolicitud.PENDIENTE,
            fechaSolicitud   = fechaSol,
            fechaLimite      = doc.getTimestamp("fechaLimite") ?: fechaLimDefault,
            fechaRespuesta   = doc.getTimestamp("fechaRespuesta"),
            fechaPago        = doc.getTimestamp("fechaPago"),
            fechaCompletado  = doc.getTimestamp("fechaCompletado"),
            motivoRechazo    = doc.getString("motivoRechazo") ?: "",
            notaAdmin        = doc.getString("notaAdmin") ?: "",
            precioAdmin      = doc.getLong("precioAdmin") ?: 0L,
            leidoPorCliente  = doc.getBoolean("leidoPorCliente") ?: false,
            comprobanteUrl   = doc.getString("comprobanteUrl") ?: "",
            fechaComprobante = doc.getTimestamp("fechaComprobante")
        )
    }

    // ── Cliente: crear solicitud ───────────────────────────────────────────────
    suspend fun crearSolicitud(
        usuarioId: String,
        nombreUsuario: String,
        emailUsuario: String,
        producto: ProductoFirestore,
        cantidad: Int,
        nota: String
    ): Boolean {
        return try {
            val ahora = Timestamp.now()
            val limite = Timestamp(ahora.seconds + TimeUnit.DAYS.toSeconds(7), 0)
            val data = hashMapOf(
                "usuarioId"        to usuarioId,
                "nombreUsuario"    to nombreUsuario,
                "emailUsuario"     to emailUsuario,
                "productoId"       to producto.id,
                "nombreProducto"   to producto.nombre,
                "categoria"        to producto.categoria,
                "precioReferencia" to producto.precio,
                "cantidad"         to cantidad,
                "nota"             to nota,
                "estado"           to EstadoSolicitud.PENDIENTE,
                "fechaSolicitud"   to ahora,
                "fechaLimite"      to limite,
                "fechaRespuesta"   to null,
                "fechaPago"        to null,
                "fechaCompletado"  to null,
                "motivoRechazo"    to "",
                "notaAdmin"        to "",
                "precioAdmin"      to 0L,
                "leidoPorCliente"  to true   // el propio cliente la crea, ya la "leyó"
            )
            col.add(data).await()
            true
        } catch (e: Exception) {
            android.util.Log.e("SOLICITUD", "Error creando solicitud: ${e.message}")
            false
        }
    }

    // ── Cliente: obtener sus solicitudes ──────────────────────────────────────
    suspend fun getSolicitudesUsuario(usuarioId: String): List<SolicitudFirestore> {
        return try {
            val snap = col.whereEqualTo("usuarioId", usuarioId).get().await()
            snap.documents
                .map { mapSolicitud(it) }
                .sortedByDescending { it.fechaSolicitud.seconds }
        } catch (e: Exception) {
            android.util.Log.e("SOLICITUD", "Error cargando solicitudes: ${e.message}")
            emptyList()
        }
    }

    // ── Cliente: contar solicitudes NO leídas (para badge) ────────────────────
    suspend fun contarNoLeidas(usuarioId: String): Int {
        return try {
            val snap = col
                .whereEqualTo("usuarioId", usuarioId)
                .whereEqualTo("leidoPorCliente", false)
                .get().await()
            snap.size()
        } catch (e: Exception) { 0 }
    }

    // ── Cliente: marcar solicitud como leída ──────────────────────────────────
    suspend fun marcarLeida(solicitudId: String) {
        try {
            col.document(solicitudId).update("leidoPorCliente", true).await()
        } catch (e: Exception) {
            android.util.Log.e("SOLICITUD", "Error marcando leída: ${e.message}")
        }
    }

    // ── Cliente: notificar que ya pagó ────────────────────────────────────────
    suspend fun notificarPago(solicitudId: String): Boolean {
        return try {
            col.document(solicitudId).update(
                mapOf(
                    "estado"    to EstadoSolicitud.PAGADO,
                    "fechaPago" to Timestamp.now()
                )
            ).await()
            true
        } catch (e: Exception) {
            android.util.Log.e("SOLICITUD", "Error notificando pago: ${e.message}")
            false
        }
    }

    // ── Admin: obtener TODAS las solicitudes ──────────────────────────────────
    suspend fun getAllSolicitudes(): List<SolicitudFirestore> {
        return try {
            val snap = col.get().await()
            snap.documents
                .map { mapSolicitud(it) }
                .sortedWith(
                    compareBy(
                        // primero las que necesitan acción inmediata
                        { estadoPrioridad(it.estado) },
                        { it.fechaSolicitud.seconds }
                    )
                )
        } catch (e: Exception) {
            android.util.Log.e("SOLICITUD", "Error cargando todas: ${e.message}")
            emptyList()
        }
    }

    private fun estadoPrioridad(estado: String): Int = when (estado) {
        EstadoSolicitud.PAGADO         -> 0   // pago esperando confirmación
        EstadoSolicitud.PENDIENTE      -> 1   // esperando respuesta del admin
        EstadoSolicitud.ACEPTADO       -> 2   // cliente aún no pagó
        EstadoSolicitud.PAGO_PENDIENTE -> 3
        EstadoSolicitud.COMPLETADO     -> 4
        EstadoSolicitud.RECHAZADO      -> 5
        EstadoSolicitud.VENCIDO        -> 6
        else                           -> 7
    }

    // ── Admin: aceptar solicitud (puede conseguir el producto) ───────────────
    suspend fun aceptarSolicitud(
        solicitudId: String,
        notaAdmin: String,          // instrucciones de pago, monto, etc.
        precioAdmin: Long           // precio final que el admin define
    ): Boolean {
        return try {
            col.document(solicitudId).update(
                mapOf(
                    "estado"          to EstadoSolicitud.PAGO_PENDIENTE,
                    "notaAdmin"       to notaAdmin,
                    "precioAdmin"     to precioAdmin,
                    "fechaRespuesta"  to Timestamp.now(),
                    "leidoPorCliente" to false    // cliente debe ver la notificación
                )
            ).await()
            true
        } catch (e: Exception) {
            android.util.Log.e("SOLICITUD", "Error aceptando: ${e.message}")
            false
        }
    }

    // ── Admin: rechazar solicitud ────────────────────────────────────────────
    suspend fun rechazarSolicitud(
        solicitudId: String,
        motivoRechazo: String
    ): Boolean {
        return try {
            col.document(solicitudId).update(
                mapOf(
                    "estado"          to EstadoSolicitud.RECHAZADO,
                    "motivoRechazo"   to motivoRechazo,
                    "fechaRespuesta"  to Timestamp.now(),
                    "leidoPorCliente" to false
                )
            ).await()
            true
        } catch (e: Exception) {
            android.util.Log.e("SOLICITUD", "Error rechazando: ${e.message}")
            false
        }
    }

    // ── Admin: confirmar pago del cliente ─────────────────────────────────────
    suspend fun confirmarPago(solicitudId: String): Boolean {
        return try {
            col.document(solicitudId).update(
                mapOf(
                    "estado"           to EstadoSolicitud.COMPLETADO,
                    "fechaCompletado"  to Timestamp.now(),
                    "leidoPorCliente"  to false    // cliente recibe notif de completado
                )
            ).await()
            true
        } catch (e: Exception) {
            android.util.Log.e("SOLICITUD", "Error confirmando pago: ${e.message}")
            false
        }
    }

    // ── Cliente: cancelar solicitud ───────────────────────────────────────────
    // Solo se permite cancelar si está en estado pendiente o pago_pendiente
    suspend fun cancelarSolicitud(solicitudId: String): Boolean {
        return try {
            col.document(solicitudId).update(
                mapOf(
                    "estado"          to "cancelado",
                    "fechaRespuesta"  to Timestamp.now(),
                    "leidoPorCliente" to true
                )
            ).await()
            true
        } catch (e: Exception) {
            android.util.Log.e("SOLICITUD", "Error cancelando solicitud: ${e.message}")
            false
        }
    }

    // ── Admin/Cron: marcar como vencidas las que pasaron 7 días ───────────────
    suspend fun vencerSolicitudesPasadas(): Int {
        return try {
            val ahora = Timestamp.now()
            val snap = col
                .whereEqualTo("estado", EstadoSolicitud.PENDIENTE)
                .get().await()
            var count = 0
            snap.documents.forEach { doc ->
                val limite = doc.getTimestamp("fechaLimite")
                if (limite != null && ahora.seconds > limite.seconds) {
                    col.document(doc.id).update(
                        mapOf(
                            "estado"          to EstadoSolicitud.VENCIDO,
                            "leidoPorCliente" to false
                        )
                    ).await()
                    count++
                }
            }
            count
        } catch (e: Exception) { 0 }
    }
}