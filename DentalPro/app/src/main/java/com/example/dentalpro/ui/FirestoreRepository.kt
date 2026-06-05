package com.example.dentalpro.ui

import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import java.io.Serializable

data class ProductoFirestore(
    val id: String = "",
    val nombre: String = "",
    val categoria: String = "",
    val descripcion: String = "",
    val precio: Long = 0,
    val precioOriginal: Long = 0,
    val descuento: Long = 0,
    val stock: Long = 0,
    val destacado: Boolean = false,
    val imageUrl: String = ""
) : Serializable

data class CategoriaFirestore(
    val id: String = "",
    val nombre: String = "",
    val emoji: String = "",
    val orden: Long = 0
)

data class UsuarioFirestore(
    val id: String = "",
    val nombre: String = "",
    val email: String = "",
    val telefono: String = "",
    val rol: String = "cliente",
    val fecha: com.google.firebase.Timestamp = com.google.firebase.Timestamp.now()
)

data class PedidoFirestore(
    val id: String = "",
    val usuarioId: String = "",
    val fecha: com.google.firebase.Timestamp = com.google.firebase.Timestamp.now(),
    val metodoPago: String = "",
    val total: Long = 0,
    val estado: String = "pendiente",
    val items: List<Map<String, Any>> = emptyList(),
    val comprobanteUrl: String = "",                          // ← URL del comprobante subido por el cliente
    val fechaComprobante: com.google.firebase.Timestamp? = null
)

object FirestoreRepository {

    private val db = Firebase.firestore

    // ── Productos ─────────────────────────────────────────────────────────────
    private fun mapProducto(doc: com.google.firebase.firestore.DocumentSnapshot): ProductoFirestore {
        return ProductoFirestore(
            id             = doc.id,
            nombre         = doc.getString("nombre") ?: "",
            categoria      = doc.getString("categoria") ?: "",
            descripcion    = doc.getString("descripcion") ?: "",
            precio         = doc.getLong("precio") ?: 0,
            precioOriginal = when (val v = doc.get("precioOriginal")) {
                is Long   -> v
                is Double -> v.toLong()
                is String -> v.toLongOrNull() ?: 0L
                else      -> 0L
            },
            descuento  = doc.getLong("descuento") ?: 0,
            stock      = doc.getLong("stock") ?: 0,
            destacado  = doc.getBoolean("destacado") ?: false,
            imageUrl   = doc.getString("imageUrl") ?: ""
        )
    }

    suspend fun getProductos(): List<ProductoFirestore> {
        return try {
            val snapshot = db.collection("productos").get().await()
            snapshot.documents.map { mapProducto(it) }
        } catch (e: Exception) {
            android.util.Log.e("FIRESTORE", "Error cargando productos: ${e.message}")
            emptyList()
        }
    }

    suspend fun getProductosPorCategoria(categoria: String): List<ProductoFirestore> {
        return try {
            val snapshot = db.collection("productos")
                .whereEqualTo("categoria", categoria)
                .get().await()
            snapshot.documents.map { mapProducto(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun agregarProducto(producto: ProductoFirestore): Boolean {
        return try {
            val data = hashMapOf(
                "nombre"         to producto.nombre,
                "categoria"      to producto.categoria,
                "descripcion"    to producto.descripcion,
                "precio"         to producto.precio,
                "precioOriginal" to producto.precioOriginal,
                "descuento"      to producto.descuento,
                "stock"          to producto.stock,
                "destacado"      to producto.destacado,
                "imageUrl"       to producto.imageUrl
            )
            db.collection("productos").add(data).await()
            true
        } catch (e: Exception) {
            android.util.Log.e("FIRESTORE", "Error agregando producto: ${e.message}")
            false
        }
    }

    suspend fun editarProducto(producto: ProductoFirestore): Boolean {
        return try {
            val data = mapOf(
                "nombre"         to producto.nombre,
                "categoria"      to producto.categoria,
                "descripcion"    to producto.descripcion,
                "precio"         to producto.precio,
                "precioOriginal" to producto.precioOriginal,
                "descuento"      to producto.descuento,
                "stock"          to producto.stock,
                "destacado"      to producto.destacado,
                "imageUrl"       to producto.imageUrl
            )
            db.collection("productos").document(producto.id).update(data).await()
            true
        } catch (e: Exception) {
            android.util.Log.e("FIRESTORE", "Error editando producto: ${e.message}")
            false
        }
    }

    suspend fun eliminarProducto(productoId: String): Boolean {
        return try {
            db.collection("productos").document(productoId).delete().await()
            true
        } catch (e: Exception) {
            android.util.Log.e("FIRESTORE", "Error eliminando producto: ${e.message}")
            false
        }
    }

    // ── Categorias ────────────────────────────────────────────────────────────
    suspend fun getCategorias(): List<CategoriaFirestore> {
        return try {
            val snapshot = db.collection("categoria").orderBy("orden").get().await()
            snapshot.documents.map { doc ->
                CategoriaFirestore(
                    id     = doc.id,
                    nombre = doc.getString("nombre") ?: "",
                    emoji  = doc.getString("emoji") ?: "",
                    orden  = doc.getLong("orden") ?: 0
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun agregarCategoria(nombre: String, emoji: String): Boolean {
        return try {
            val orden = getCategorias().size.toLong() + 1
            val data = hashMapOf(
                "nombre" to nombre,
                "emoji"  to emoji,
                "orden"  to orden
            )
            db.collection("categoria").add(data).await()
            true
        } catch (e: Exception) {
            android.util.Log.e("FIRESTORE", "Error agregando categoria: ${e.message}")
            false
        }
    }

    // ── Usuarios ──────────────────────────────────────────────────────────────
    suspend fun guardarUsuarioGoogle(uid: String, nombre: String, email: String): Boolean {
        return try {
            val existingUser = db.collection("usuarios").document(uid).get().await()
            if (!existingUser.exists()) {
                val usuario = hashMapOf(
                    "nombre"   to nombre,
                    "email"    to email,
                    "telefono" to "",
                    "rol"      to "cliente",
                    "fecha"    to com.google.firebase.Timestamp.now()
                )
                db.collection("usuarios").document(uid).set(usuario).await()
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getUsuario(uid: String): UsuarioFirestore? {
        return try {
            val doc = db.collection("usuarios").document(uid).get().await()
            if (doc.exists()) {
                UsuarioFirestore(
                    id       = doc.id,
                    nombre   = doc.getString("nombre") ?: "",
                    email    = doc.getString("email") ?: "",
                    telefono = doc.getString("telefono") ?: "",
                    rol      = doc.getString("rol") ?: "cliente",
                    fecha    = doc.getTimestamp("fecha") ?: com.google.firebase.Timestamp.now()
                )
            } else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getTodosLosUsuarios(): List<UsuarioFirestore> {
        return try {
            val snapshot = db.collection("usuarios").get().await()
            snapshot.documents.map { doc ->
                UsuarioFirestore(
                    id       = doc.id,
                    nombre   = doc.getString("nombre") ?: "",
                    email    = doc.getString("email") ?: "",
                    telefono = doc.getString("telefono") ?: "",
                    rol      = doc.getString("rol") ?: "cliente",
                    fecha    = doc.getTimestamp("fecha") ?: com.google.firebase.Timestamp.now()
                )
            }.sortedWith(compareBy({ it.rol != "admin" }, { it.nombre }))
        } catch (e: Exception) {
            android.util.Log.e("FIRESTORE", "Error cargando usuarios: ${e.message}")
            emptyList()
        }
    }

    suspend fun cambiarRolUsuario(uid: String, nuevoRol: String): Boolean {
        return try {
            db.collection("usuarios").document(uid).update("rol", nuevoRol).await()
            true
        } catch (e: Exception) {
            android.util.Log.e("FIRESTORE", "Error cambiando rol: ${e.message}")
            false
        }
    }

    // ── Pedidos ───────────────────────────────────────────────────────────────

    // Helper para mapear un documento de pedido
    private fun mapPedido(doc: com.google.firebase.firestore.DocumentSnapshot): PedidoFirestore {
        return PedidoFirestore(
            id               = doc.id,
            usuarioId        = doc.getString("usuarioId") ?: "",
            fecha            = doc.getTimestamp("fecha") ?: com.google.firebase.Timestamp.now(),
            metodoPago       = doc.getString("metodoPago") ?: "",
            total            = doc.getLong("total") ?: 0,
            estado           = doc.getString("estado") ?: "pendiente",
            items            = doc.get("items") as? List<Map<String, Any>> ?: emptyList(),
            comprobanteUrl   = doc.getString("comprobanteUrl") ?: "",
            fechaComprobante = doc.getTimestamp("fechaComprobante")
        )
    }

    // Guarda el pedido y retorna el ID del documento creado (null si falla)
    suspend fun guardarPedidoYDescontarStockConId(
        usuarioId: String,
        items: List<CartItem>,
        total: Double
    ): String? {
        return try {
            val batch = db.batch()

            val itemsData = items.map { item ->
                mapOf(
                    "productoId" to item.producto.id,
                    "nombre"     to item.producto.nombre,
                    "cantidad"   to item.cantidad,
                    "precio"     to item.producto.precio
                )
            }

            val pedido = hashMapOf(
                "usuarioId"        to usuarioId,
                "items"            to itemsData,
                "total"            to total.toLong(),
                "estado"           to "pendiente",
                "metodoPago"       to "QR",
                "fecha"            to com.google.firebase.Timestamp.now(),
                "comprobanteUrl"   to "",     // se rellena al subir el comprobante
                "fechaComprobante" to null
            )

            val pedidoRef = db.collection("pedidos").document()  // genera ID automático
            batch.set(pedidoRef, pedido)

            items.forEach { item ->
                val productoRef = db.collection("productos").document(item.producto.id)
                val nuevoStock  = item.producto.stock - item.cantidad
                batch.update(productoRef, "stock", nuevoStock)
            }

            batch.commit().await()

            pedidoRef.id   // ← retorna el ID real del documento

        } catch (e: Exception) {
            android.util.Log.e("FIRESTORE", "Error guardando pedido con ID: ${e.message}")
            null
        }
    }

    // Versión legacy que retorna Boolean (mantener por compatibilidad)
    suspend fun guardarPedidoYDescontarStock(
        usuarioId: String,
        items: List<CartItem>,
        total: Double
    ): Boolean {
        return guardarPedidoYDescontarStockConId(usuarioId, items, total) != null
    }

    suspend fun getPedidosUsuario(uid: String): List<PedidoFirestore> {
        return try {
            val snapshot = db.collection("pedidos")
                .whereEqualTo("usuarioId", uid)
                .get().await()
            snapshot.documents
                .map { mapPedido(it) }
                .sortedByDescending { it.fecha.seconds }
        } catch (e: Exception) {
            android.util.Log.e("FIRESTORE", "Error cargando pedidos: ${e.message}")
            emptyList()
        }
    }

    suspend fun getTodosPedidos(): List<PedidoFirestore> {
        return try {
            val snapshot = db.collection("pedidos").get().await()
            snapshot.documents
                .map { mapPedido(it) }
                .sortedByDescending { it.fecha.seconds }
        } catch (e: Exception) {
            android.util.Log.e("FIRESTORE", "Error cargando todos los pedidos: ${e.message}")
            emptyList()
        }
    }

    suspend fun actualizarEstadoPedido(pedidoId: String, nuevoEstado: String): Boolean {
        return try {
            db.collection("pedidos").document(pedidoId)
                .update("estado", nuevoEstado).await()
            true
        } catch (e: Exception) {
            android.util.Log.e("FIRESTORE", "Error actualizando estado: ${e.message}")
            false
        }
    }
}