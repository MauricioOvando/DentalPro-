package com.example.dentalpro.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

data class CartItem(
    val producto: ProductoFirestore,
    var cantidad: Int
)

class CartViewModel : ViewModel() {

    var items by mutableStateOf<List<CartItem>>(emptyList())
        private set

    val totalItems: Int
        get() = items.sumOf { it.cantidad }

    val total: Double
        get() = items.sumOf { it.producto.precio.toDouble() * it.cantidad }

    fun agregarProducto(producto: ProductoFirestore) {
        val existingItem = items.find { it.producto.id == producto.id }

        if (existingItem != null) {
            // Verificar stock antes de aumentar cantidad
            if (existingItem.cantidad < producto.stock) {
                val updatedItems = items.map { item ->
                    if (item.producto.id == producto.id) {
                        item.copy(cantidad = item.cantidad + 1)
                    } else {
                        item
                    }
                }
                items = updatedItems
            }
            // Si ya llegó al stock, no hace nada (no aumenta)
        } else {
            // Nuevo producto, verificar que haya stock
            if (producto.stock > 0) {
                items = items + CartItem(producto, 1)
            }
        }
    }

    fun quitarProducto(producto: ProductoFirestore) {
        val existingItem = items.find { it.producto.id == producto.id }

        if (existingItem != null) {
            if (existingItem.cantidad > 1) {
                val updatedItems = items.map { item ->
                    if (item.producto.id == producto.id) {
                        item.copy(cantidad = item.cantidad - 1)
                    } else {
                        item
                    }
                }
                items = updatedItems
            } else {
                items = items.filter { it.producto.id != producto.id }
            }
        }
    }

    fun eliminarProducto(producto: ProductoFirestore) {
        items = items.filter { it.producto.id != producto.id }
    }

    fun setCantidad(producto: ProductoFirestore, nueva: Int) {
        val clamped = nueva.coerceIn(1, producto.stock.toInt().coerceAtLeast(1))
        items = items.map { item ->
            if (item.producto.id == producto.id) item.copy(cantidad = clamped) else item
        }
    }

    fun vaciarCarrito() {
        items = emptyList()
    }

    fun obtenerCantidad(productoId: String): Int {
        return items.find { it.producto.id == productoId }?.cantidad ?: 0
    }

    fun stockDisponible(producto: ProductoFirestore): Int {
        val cantidadEnCarrito = obtenerCantidad(producto.id)
        return (producto.stock - cantidadEnCarrito).toInt().coerceAtLeast(0)
    }
}