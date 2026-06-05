package com.example.dentalpro.ui

import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import java.util.Calendar

// ── Modelos de estadísticas ───────────────────────────────────────────────────

data class ResumenGeneral(
    val totalProductos: Int = 0,
    val productosSinStock: Int = 0,
    val productosStockBajo: Int = 0,   // stock entre 1 y 5
    val totalPedidos: Int = 0,
    val pedidosPendientes: Int = 0,
    val pedidosCompletados: Int = 0,
    val totalClientes: Int = 0,
    val ingresosTotales: Long = 0,
    val ingresosEsteMes: Long = 0,
    val solicitudesPendientes: Int = 0
)

data class VentaMensual(
    val mes: String,           // "Ene", "Feb", …
    val mesNumero: Int,        // 1..12
    val anio: Int,
    val totalVentas: Long,
    val cantidadPedidos: Int
)

data class ProductoVendido(
    val productoId: String,
    val nombre: String,
    val categoria: String,
    val cantidadVendida: Int,
    val ingresos: Long
)

data class StockAlerta(
    val productoId: String,
    val nombre: String,
    val categoria: String,
    val stock: Long,
    val precio: Long
)

data class EstadisticaCategoria(
    val categoria: String,
    val cantidadProductos: Int,
    val totalVentas: Long,
    val unidadesVendidas: Int
)

// ── Repository ────────────────────────────────────────────────────────────────

object StatsRepository {

    private val db = Firebase.firestore

    private val MESES = listOf("Ene","Feb","Mar","Abr","May","Jun","Jul","Ago","Sep","Oct","Nov","Dic")

    // ── Resumen general ───────────────────────────────────────────────────────
    suspend fun getResumenGeneral(): ResumenGeneral {
        return try {
            val productos   = db.collection("productos").get().await().documents
            val pedidos     = db.collection("pedidos").get().await().documents
            val usuarios    = db.collection("usuarios")
                .whereEqualTo("rol", "cliente").get().await()
            val solicitudes = db.collection("solicitudes")
                .whereEqualTo("estado", "pendiente").get().await()

            val ahora      = Calendar.getInstance()
            val mesActual  = ahora.get(Calendar.MONTH)
            val anioActual = ahora.get(Calendar.YEAR)

            var ingresosTotal    = 0L
            var ingresosMes      = 0L
            var pedidosPend      = 0
            var pedidosComp      = 0

            pedidos.forEach { doc ->
                val estado = doc.getString("estado") ?: ""
                val total  = doc.getLong("total") ?: 0L
                val fecha  = doc.getTimestamp("fecha")

                if (estado == "completado" || estado == "entregado") {
                    ingresosTotal += total
                    pedidosComp++
                    if (fecha != null) {
                        val cal = Calendar.getInstance()
                        cal.timeInMillis = fecha.seconds * 1000
                        if (cal.get(Calendar.MONTH) == mesActual &&
                            cal.get(Calendar.YEAR)  == anioActual) {
                            ingresosMes += total
                        }
                    }
                }
                if (estado == "pendiente") pedidosPend++
            }

            ResumenGeneral(
                totalProductos        = productos.size,
                productosSinStock     = productos.count { (it.getLong("stock") ?: 0L) == 0L },
                productosStockBajo    = productos.count { (it.getLong("stock") ?: 0L) in 1..5 },
                totalPedidos          = pedidos.size,
                pedidosPendientes     = pedidosPend,
                pedidosCompletados    = pedidosComp,
                totalClientes         = usuarios.size(),
                ingresosTotales       = ingresosTotal,
                ingresosEsteMes       = ingresosMes,
                solicitudesPendientes = solicitudes.size()
            )
        } catch (e: Exception) {
            android.util.Log.e("STATS", "Error resumen: ${e.message}")
            ResumenGeneral()
        }
    }

    // ── Ventas por mes (últimos N meses) ──────────────────────────────────────
    suspend fun getVentasMensuales(meses: Int = 6): List<VentaMensual> {
        return try {
            val pedidos = db.collection("pedidos")
                .whereIn("estado", listOf("completado", "entregado"))
                .get().await().documents

            // Construir mapa mes→datos para los últimos N meses
            val cal    = Calendar.getInstance()
            val resultado = mutableMapOf<String, VentaMensual>()

            repeat(meses) { i ->
                val c = Calendar.getInstance()
                c.add(Calendar.MONTH, -i)
                val key = "${c.get(Calendar.YEAR)}-${c.get(Calendar.MONTH)}"
                resultado[key] = VentaMensual(
                    mes             = MESES[c.get(Calendar.MONTH)],
                    mesNumero       = c.get(Calendar.MONTH) + 1,
                    anio            = c.get(Calendar.YEAR),
                    totalVentas     = 0L,
                    cantidadPedidos = 0
                )
            }

            pedidos.forEach { doc ->
                val fecha = doc.getTimestamp("fecha") ?: return@forEach
                val total = doc.getLong("total") ?: 0L
                cal.timeInMillis = fecha.seconds * 1000
                val key = "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.MONTH)}"
                resultado[key]?.let { vm ->
                    resultado[key] = vm.copy(
                        totalVentas     = vm.totalVentas + total,
                        cantidadPedidos = vm.cantidadPedidos + 1
                    )
                }
            }

            // Orden cronológico (más antiguo primero para el gráfico)
            resultado.values.sortedWith(compareBy({ it.anio }, { it.mesNumero }))

        } catch (e: Exception) {
            android.util.Log.e("STATS", "Error ventas mensuales: ${e.message}")
            emptyList()
        }
    }

    // ── Productos más vendidos ────────────────────────────────────────────────
    suspend fun getProductosMasVendidos(limite: Int = 5): List<ProductoVendido> {
        return try {
            val pedidos = db.collection("pedidos")
                .whereIn("estado", listOf("completado", "entregado", "pendiente"))
                .get().await().documents

            val mapaProductos = mutableMapOf<String, ProductoVendido>()

            pedidos.forEach { doc ->
                @Suppress("UNCHECKED_CAST")
                val items = doc.get("items") as? List<Map<String, Any>> ?: return@forEach
                items.forEach { item ->
                    val id       = item["productoId"] as? String ?: return@forEach
                    val nombre   = item["nombre"] as? String ?: ""
                    val cantidad = when (val c = item["cantidad"]) {
                        is Long   -> c.toInt()
                        is Int    -> c
                        is Double -> c.toInt()
                        else      -> 1
                    }
                    val precio = when (val p = item["precio"]) {
                        is Long   -> p
                        is Double -> p.toLong()
                        else      -> 0L
                    }
                    val existing = mapaProductos[id]
                    if (existing != null) {
                        mapaProductos[id] = existing.copy(
                            cantidadVendida = existing.cantidadVendida + cantidad,
                            ingresos        = existing.ingresos + (precio * cantidad)
                        )
                    } else {
                        mapaProductos[id] = ProductoVendido(
                            productoId      = id,
                            nombre          = nombre,
                            categoria       = "",
                            cantidadVendida = cantidad,
                            ingresos        = precio * cantidad
                        )
                    }
                }
            }

            mapaProductos.values
                .sortedByDescending { it.cantidadVendida }
                .take(limite)

        } catch (e: Exception) {
            android.util.Log.e("STATS", "Error productos vendidos: ${e.message}")
            emptyList()
        }
    }

    // ── Productos con stock bajo o sin stock ──────────────────────────────────
    suspend fun getAlertasStock(limiteStock: Int = 5): List<StockAlerta> {
        return try {
            val docs = db.collection("productos").get().await().documents
            docs.mapNotNull { doc ->
                val stock = doc.getLong("stock") ?: 0L
                if (stock <= limiteStock) {
                    StockAlerta(
                        productoId = doc.id,
                        nombre     = doc.getString("nombre") ?: "",
                        categoria  = doc.getString("categoria") ?: "",
                        stock      = stock,
                        precio     = doc.getLong("precio") ?: 0L
                    )
                } else null
            }.sortedBy { it.stock }
        } catch (e: Exception) {
            android.util.Log.e("STATS", "Error alertas stock: ${e.message}")
            emptyList()
        }
    }

    // ── Ventas por categoría ──────────────────────────────────────────────────
    suspend fun getEstadisticasPorCategoria(): List<EstadisticaCategoria> {
        return try {
            val productos = db.collection("productos").get().await().documents
            val pedidos   = db.collection("pedidos")
                .whereIn("estado", listOf("completado", "entregado", "pendiente"))
                .get().await().documents

            // Mapa productoId → categoria
            val catPorProducto = productos.associate { doc ->
                doc.id to (doc.getString("categoria") ?: "Sin categoría")
            }

            val mapa = mutableMapOf<String, EstadisticaCategoria>()

            // Contar productos por categoría
            productos.forEach { doc ->
                val cat = doc.getString("categoria") ?: "Sin categoría"
                val existing = mapa[cat]
                mapa[cat] = (existing ?: EstadisticaCategoria(cat, 0, 0L, 0))
                    .copy(cantidadProductos = (existing?.cantidadProductos ?: 0) + 1)
            }

            // Acumular ventas por categoría
            pedidos.forEach { doc ->
                @Suppress("UNCHECKED_CAST")
                val items = doc.get("items") as? List<Map<String, Any>> ?: return@forEach
                items.forEach { item ->
                    val pid      = item["productoId"] as? String ?: return@forEach
                    val cat      = catPorProducto[pid] ?: "Sin categoría"
                    val cantidad = when (val c = item["cantidad"]) {
                        is Long -> c.toInt(); is Int -> c; is Double -> c.toInt(); else -> 1
                    }
                    val precio = when (val p = item["precio"]) {
                        is Long -> p; is Double -> p.toLong(); else -> 0L
                    }
                    val existing = mapa[cat] ?: EstadisticaCategoria(cat, 0, 0L, 0)
                    mapa[cat] = existing.copy(
                        totalVentas      = existing.totalVentas + (precio * cantidad),
                        unidadesVendidas = existing.unidadesVendidas + cantidad
                    )
                }
            }

            mapa.values.sortedByDescending { it.totalVentas }

        } catch (e: Exception) {
            android.util.Log.e("STATS", "Error categorías: ${e.message}")
            emptyList()
        }
    }

    // ── Ingresos del día actual ───────────────────────────────────────────────
    suspend fun getIngresosHoy(): Long {
        return try {
            val cal = Calendar.getInstance()
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            val inicioDia = com.google.firebase.Timestamp(cal.timeInMillis / 1000, 0)

            val pedidos = db.collection("pedidos")
                .whereGreaterThanOrEqualTo("fecha", inicioDia)
                .whereIn("estado", listOf("completado", "entregado"))
                .get().await().documents

            pedidos.sumOf { it.getLong("total") ?: 0L }
        } catch (e: Exception) { 0L }
    }
}