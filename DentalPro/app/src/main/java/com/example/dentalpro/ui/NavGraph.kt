package com.example.dentalpro.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object Routes {
    const val LOGIN              = "login"
    const val REGISTER           = "register"
    const val HOME               = "home"
    const val CART               = "cart"
    const val CHECKOUT           = "checkout"
    const val PRODUCT            = "product/{productoId}"
    const val PROFILE            = "profile"
    const val ADMIN              = "admin"
    const val ADMIN_FORM         = "admin_form?productoId={productoId}"
    const val ADMIN_USERS        = "admin_users"
    const val ADMIN_ORDERS       = "admin_orders"
    const val SOLICITUD          = "solicitud/{productoId}"
    const val NOTIFICACIONES     = "notificaciones"
    const val ADMIN_SOLICITUDES  = "admin_solicitudes"
    const val ADMIN_QR           = "admin_qr"
    const val PEDIDO_ESPECIAL    = "pedido_especial"
    const val DASHBOARD          = "dashboard"
    const val REPORTS            = "reports"

    fun productRoute(id: String)    = "product/$id"
    fun adminFormEdit(id: String)   = "admin_form?productoId=$id"
    fun adminFormNew()              = "admin_form?productoId="
    fun solicitudRoute(id: String)  = "solicitud/$id"
}

@Composable
fun DentalProNavGraph(
    navController: NavHostController = rememberNavController()
) {
    val cartViewModel: CartViewModel = viewModel()

    NavHost(
        navController    = navController,
        startDestination = Routes.LOGIN
    ) {
        // ── Login ─────────────────────────────────────────────────────────────
        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    val uid = FirebaseAuth.getInstance().currentUser?.uid
                    if (uid != null) {
                        CoroutineScope(Dispatchers.Main).launch {
                            val usuario = FirestoreRepository.getUsuario(uid)
                            val destino = if (usuario?.rol == "admin") Routes.ADMIN else Routes.HOME
                            navController.navigate(destino) {
                                popUpTo(Routes.LOGIN) { inclusive = true }
                            }
                        }
                    } else {
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    }
                },
                onNavigateToRegister = { navController.navigate(Routes.REGISTER) }
            )
        }

        // ── Registro ──────────────────────────────────────────────────────────
        composable(Routes.REGISTER) {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onNavigateToLogin = { navController.popBackStack() }
            )
        }

        // ── Home (clientes) ───────────────────────────────────────────────────
        composable(Routes.HOME) {
            HomeScreen(
                cartViewModel  = cartViewModel,
                onProductClick = { producto ->
                    navController.currentBackStackEntry
                        ?.savedStateHandle
                        ?.set("producto_${producto.id}", producto)
                    navController.navigate(Routes.productRoute(producto.id))
                    navController.currentBackStackEntry
                        ?.savedStateHandle
                        ?.set("producto_${producto.id}", producto)
                },
                onGoToCart           = { navController.navigate(Routes.CART) },
                onGoToProfile        = { navController.navigate(Routes.PROFILE) },
                onGoToAdmin          = { navController.navigate(Routes.ADMIN) },
                onGoToNotificaciones = { navController.navigate(Routes.NOTIFICACIONES) },
                onGoToSolicitudes    = { navController.navigate(Routes.PEDIDO_ESPECIAL) }  // ← CORREGIDO
            )
        }

        // ── Detalle producto ──────────────────────────────────────────────────
        composable(
            route     = Routes.PRODUCT,
            arguments = listOf(navArgument("productoId") { type = NavType.StringType })
        ) { backStackEntry ->
            val productoId = backStackEntry.arguments?.getString("productoId") ?: ""
            val producto   = backStackEntry.savedStateHandle.get<ProductoFirestore>("producto_$productoId")
                ?: navController.previousBackStackEntry?.savedStateHandle?.get<ProductoFirestore>("producto_$productoId")

            if (producto != null) {
                ProductDetailScreen(
                    producto      = producto,
                    cartViewModel = cartViewModel,
                    onBack        = { navController.popBackStack() },
                    onGoToCart    = { navController.navigate(Routes.CART) },
                    onSolicitar   = {
                        navController.currentBackStackEntry
                            ?.savedStateHandle
                            ?.set("solicitud_${producto.id}", producto)
                        navController.navigate(Routes.solicitudRoute(producto.id))
                    }
                )
            } else {
                LaunchedEffect(Unit) { navController.popBackStack() }
            }
        }

        // ── Carrito ───────────────────────────────────────────────────────────
        composable(Routes.CART) {
            CartScreen(
                cartViewModel = cartViewModel,
                onBack        = { navController.popBackStack() },
                onCheckout    = { navController.navigate(Routes.CHECKOUT) }
            )
        }

        // ── Checkout ──────────────────────────────────────────────────────────
        composable(Routes.CHECKOUT) {
            CheckoutScreen(
                cartViewModel = cartViewModel,
                onBack        = { navController.popBackStack() },
                onOrderPlaced = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.HOME) { inclusive = true }
                    }
                }
            )
        }

        // ── Perfil ────────────────────────────────────────────────────────────
        composable(Routes.PROFILE) {
            ProfileScreen(
                onLogout = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // ── Panel Admin ───────────────────────────────────────────────────────
        composable(Routes.ADMIN) {
            AdminScreen(
                onBack               = { navController.popBackStack() },
                onNewProduct         = { navController.navigate(Routes.adminFormNew()) },
                onEditProduct        = { producto ->
                    navController.currentBackStackEntry
                        ?.savedStateHandle
                        ?.set("edit_producto", producto)
                    navController.navigate(Routes.adminFormEdit(producto.id))
                },
                onManageUsers        = { navController.navigate(Routes.ADMIN_USERS) },
                onManageOrders       = { navController.navigate(Routes.ADMIN_ORDERS) },
                onManageSolicitudes  = { navController.navigate(Routes.ADMIN_SOLICITUDES) },
                onManageQr           = { navController.navigate(Routes.ADMIN_QR) },
                onManageDashboard    = { navController.navigate(Routes.DASHBOARD) },
                onManageReports      = { navController.navigate(Routes.REPORTS) },
                onLogout             = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // ── Formulario producto ───────────────────────────────────────────────
        composable(
            route     = Routes.ADMIN_FORM,
            arguments = listOf(navArgument("productoId") {
                type         = NavType.StringType
                defaultValue = ""
            })
        ) { backStackEntry ->
            val productoId = backStackEntry.arguments?.getString("productoId") ?: ""
            val producto   = if (productoId.isNotBlank())
                navController.previousBackStackEntry?.savedStateHandle?.get<ProductoFirestore>("edit_producto")
            else null

            AdminProductFormScreen(
                productoExistente = producto,
                onBack            = { navController.popBackStack() },
                onSaved           = { navController.popBackStack() }
            )
        }

        // ── Usuarios admin ────────────────────────────────────────────────────
        composable(Routes.ADMIN_USERS) {
            AdminUsersScreen(onBack = { navController.popBackStack() })
        }

        // ── Pedidos admin ─────────────────────────────────────────────────────
        composable(Routes.ADMIN_ORDERS) {
            AdminOrdersScreen(onBack = { navController.popBackStack() })
        }

        // ── Solicitud producto sin stock ──────────────────────────────────────
        composable(
            route     = Routes.SOLICITUD,
            arguments = listOf(navArgument("productoId") { type = NavType.StringType })
        ) { backStackEntry ->
            val productoId = backStackEntry.arguments?.getString("productoId") ?: ""
            val producto   = navController.previousBackStackEntry
                ?.savedStateHandle?.get<ProductoFirestore>("solicitud_$productoId")

            if (producto != null) {
                SolicitudScreen(
                    producto  = producto,
                    onBack    = { navController.popBackStack() },
                    onSuccess = { navController.popBackStack() }
                )
            } else {
                LaunchedEffect(Unit) { navController.popBackStack() }
            }
        }

        // ── Notificaciones del cliente ────────────────────────────────────────
        composable(Routes.NOTIFICACIONES) {
            NotificacionesScreen(onBack = { navController.popBackStack() })
        }

        // ── Solicitudes panel admin ───────────────────────────────────────────
        composable(Routes.ADMIN_SOLICITUDES) {
            AdminSolicitudesScreen(onBack = { navController.popBackStack() })
        }

        // ── QR de pago admin ──────────────────────────────────────────────────
        composable(Routes.ADMIN_QR) {
            AdminQrScreen(onBack = { navController.popBackStack() })
        }

        // ── Pedido especial (libre, sin producto concreto) ────────────────────
        composable(Routes.PEDIDO_ESPECIAL) {
            PedidoEspecialScreen(onBack = { navController.popBackStack() })
        }

        // ── Dashboard ─────────────────────────────────────────────────────────
        composable(Routes.DASHBOARD) {
            DashboardScreen(onBack = { navController.popBackStack() })
        }

        // ── Reportes ──────────────────────────────────────────────────────────
        composable(Routes.REPORTS) {
            ReportsScreen(onBack = { navController.popBackStack() })
        }
    }
}