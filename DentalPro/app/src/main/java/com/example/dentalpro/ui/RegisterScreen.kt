package com.example.dentalpro.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

private val GreenDark  = Color(0xFF1B5E20)
private val GreenMid   = Color(0xFF2E7D32)
private val GreenLight = Color(0xFF4CAF50)
private val White      = Color(0xFFFFFFFF)
private val GrayBorder = Color(0xFFE0E0E0)
private val GrayText   = Color(0xFF374151)

@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val auth = Firebase.auth
    val db   = Firebase.firestore

    var name            by remember { mutableStateOf("") }
    var email           by remember { mutableStateOf("") }
    var phone           by remember { mutableStateOf("") }
    var password        by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading       by remember { mutableStateOf(false) }
    var errorMessage    by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().background(White)
    ) {
        // Hero
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(GreenDark, GreenLight)))
                .padding(vertical = 36.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(White, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) { Text("🦷", fontSize = 28.sp) }
                Text("Crear Cuenta", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = White)
                Text("DentalPro", fontSize = 13.sp, color = White.copy(alpha = 0.8f))
            }
        }

        // Formulario
        Surface(
            modifier = Modifier.fillMaxSize().offset(y = (-16).dp),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            color = White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                errorMessage?.let { msg ->
                    Text(
                        msg,
                        color = if (msg.startsWith("✅")) Color(0xFF2E7D32) else Color(0xFFE53935),
                        fontSize = 12.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (msg.startsWith("✅")) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(10.dp)
                    )
                }

                InputFieldRegister("Nombre completo", name, { name = it }, "Tu nombre")
                InputFieldRegister("Correo electrónico", email, { email = it }, "tu@correo.com")
                InputFieldRegister("Teléfono (opcional)", phone, { phone = it }, "+591 70000000")

                // Contraseña
                Text("Contraseña", fontSize = 11.sp, color = GrayText, fontWeight = FontWeight.Medium)
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    placeholder = { Text("Mínimo 6 caracteres", fontSize = 13.sp, color = Color(0xFF374151)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        TextButton(onClick = { passwordVisible = !passwordVisible }) {
                            Text(if (passwordVisible) "Ocultar" else "Ver", fontSize = 11.sp, color = GreenMid)
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = GreenMid,
                        focusedTextColor     = Color(0xFF0F172A),
                        unfocusedTextColor   = Color(0xFF0F172A),
                        unfocusedBorderColor = GrayBorder
                    )
                )

                InputFieldRegister(
                    "Confirmar contraseña",
                    confirmPassword,
                    { confirmPassword = it },
                    "Repite tu contraseña",
                    isPassword = true
                )

                Spacer(Modifier.height(8.dp))

                // Botón Crear cuenta con Firebase Auth
                Button(
                    onClick = {
                        when {
                            name.isBlank() || email.isBlank() || password.isBlank() ->
                                errorMessage = "Completa todos los campos obligatorios."
                            password != confirmPassword ->
                                errorMessage = "Las contraseñas no coinciden."
                            password.length < 6 ->
                                errorMessage = "La contraseña debe tener al menos 6 caracteres."
                            else -> {
                                isLoading = true
                                errorMessage = null
                                auth.createUserWithEmailAndPassword(email.trim(), password)
                                    .addOnSuccessListener { authResult ->
                                        val uid = authResult.user?.uid ?: ""
                                        // Guardar datos adicionales en Firestore
                                        val userData = hashMapOf(
                                            "nombre"   to name,
                                            "email"    to email.trim(),
                                            "telefono" to phone,
                                            "rol"      to "cliente",
                                            "fecha"    to com.google.firebase.Timestamp.now()
                                        )
                                        db.collection("usuarios").document(uid)
                                            .set(userData)
                                            .addOnCompleteListener {
                                                isLoading = false
                                                onRegisterSuccess()
                                            }
                                    }
                                    .addOnFailureListener { exception ->
                                        isLoading = false
                                        errorMessage = when {
                                            exception.message?.contains("email address is already in use") == true ->
                                                "Ya existe una cuenta con ese correo."
                                            exception.message?.contains("badly formatted") == true ->
                                                "El formato del correo no es válido."
                                            else -> "Error al crear la cuenta. Intenta de nuevo."
                                        }
                                    }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GreenMid),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = White
                        )
                    } else {
                        Text("Crear cuenta", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("¿Ya tienes cuenta? ", fontSize = 12.sp, color = GrayText)
                    Text(
                        "Inicia sesión",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = GreenMid,
                        modifier = Modifier.clickable { onNavigateToLogin() }
                    )
                }
            }
        }
    }
}

@Composable
private fun InputFieldRegister(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    isPassword: Boolean = false
) {
    val GreenMid   = Color(0xFF2E7D32)
    val GrayBorder = Color(0xFFE0E0E0)
    val GrayText   = Color(0xFF374151)
    Text(label, fontSize = 11.sp, color = GrayText, fontWeight = FontWeight.Medium)
    Spacer(Modifier.height(4.dp))
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, fontSize = 13.sp, color = Color(0xFF374151)) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        singleLine = true,
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor   = GreenMid,
            unfocusedBorderColor = GrayBorder,
            focusedTextColor     = Color(0xFF0F172A),
            unfocusedTextColor   = Color(0xFF0F172A)
        )
    )
}