package com.example.dentalpro.ui

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private val BlueDeep   = Color(0xFF1565C0)
private val BlueMid    = Color(0xFF1E88E5)
private val BlueLight  = Color(0xFF42A5F5)
private val White      = Color(0xFFFCF9F9)
private val GrayBorder = Color(0xFF0E0D0D)
private val GrayText   = Color(0xFF0B0C0C)
private val TextDark   = Color(0xFF0C0D0E)

@Composable
fun LoginScreen(
    onLoginSuccess: (idToken: String?) -> Unit,
    onNavigateToRegister: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val auth: FirebaseAuth = FirebaseAuth.getInstance()

    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken("823504267325-d00f945o7p4stluuidv0a7al626amtmb.apps.googleusercontent.com")
        .requestEmail()
        .build()

    val googleSignInClient = GoogleSignIn.getClient(context, gso)

    val googleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)

                scope.launch {
                    try {
                        val authResult = auth.signInWithCredential(credential).await()
                        val user = authResult.user

                        if (user != null) {
                            // Guardar usuario en Firestore
                            FirestoreRepository.guardarUsuarioGoogle(
                                uid = user.uid,
                                nombre = user.displayName ?: "",
                                email = user.email ?: ""
                            )
                            isLoading = false
                            onLoginSuccess(account.idToken)
                        }
                    } catch (e: Exception) {
                        isLoading = false
                        errorMessage = "Error: ${e.message}"
                    }
                }
            } catch (e: ApiException) {
                isLoading = false
                errorMessage = "Error Google: ${e.statusCode}"
            }
        } else {
            isLoading = false
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(White)) {
        Box(
            modifier = Modifier.fillMaxWidth()
                .background(Brush.linearGradient(listOf(BlueDeep, BlueLight)))
                .padding(vertical = 48.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier.size(64.dp).background(White, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) { Text("🦷", fontSize = 30.sp) }
                Text("DentalPro", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = White)
                Text("PRODUCTOS DENTALES", fontSize = 11.sp, color = White.copy(alpha = 0.75f), letterSpacing = 2.sp)
            }
        }

        Surface(
            modifier = Modifier.fillMaxSize().offset(y = (-16).dp),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            color = White
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 28.dp)) {
                errorMessage?.let { msg ->
                    Text(msg, color = Color(0xFFE53935), fontSize = 12.sp,
                        modifier = Modifier.fillMaxWidth()
                            .background(Color(0xFFFFEBEE), RoundedCornerShape(8.dp))
                            .padding(10.dp))
                    Spacer(Modifier.height(12.dp))
                }

                Text("INICIAR SESIÓN CON", fontSize = 11.sp, color = GrayText, letterSpacing = 1.5.sp,
                    modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                Spacer(Modifier.height(12.dp))

                OutlinedButton(
                    onClick = {
                        isLoading = true
                        errorMessage = null
                        googleSignInClient.signOut().addOnCompleteListener {
                            googleLauncher.launch(googleSignInClient.signInIntent)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextDark)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = BlueMid)
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("G", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4285F4))
                            Text("Continuar con Google", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = GrayBorder)
                    Text("  o ingresa tu correo  ", fontSize = 11.sp, color = GrayText)
                    HorizontalDivider(modifier = Modifier.weight(1f), color = GrayBorder)
                }

                Spacer(Modifier.height(16.dp))

                Text("Correo electrónico", fontSize = 11.sp, color = GrayText, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(4.dp))
                OutlinedTextField(
                    value = email, onValueChange = { email = it },
                    placeholder = { Text("Email ID", fontSize = 13.sp, color = GrayText) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp), singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = BlueMid,
                        unfocusedBorderColor = GrayBorder,
                        focusedTextColor     = Color(0xFF0F172A),
                        unfocusedTextColor   = Color(0xFF0F172A)
                    )
                )

                Spacer(Modifier.height(10.dp))

                Text("Contraseña", fontSize = 11.sp, color = GrayText, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(4.dp))
                OutlinedTextField(
                    value = password, onValueChange = { password = it },
                    placeholder = { Text("••••••••", fontSize = 13.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp), singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        TextButton(onClick = { passwordVisible = !passwordVisible }) {
                            Text(if (passwordVisible) "Ocultar" else "Ver", fontSize = 11.sp, color = BlueMid)
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = BlueMid,
                        unfocusedBorderColor = GrayBorder,
                        focusedTextColor     = Color(0xFF0F172A),
                        unfocusedTextColor   = Color(0xFF0F172A)
                    )
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = {
                        if (email.isNotBlank()) {
                            auth.sendPasswordResetEmail(email)
                                .addOnSuccessListener { errorMessage = "Correo enviado" }
                                .addOnFailureListener { e -> errorMessage = "Error: ${e.message}" }
                        } else {
                            errorMessage = "Ingresa tu correo primero"
                        }
                    }) {
                        Text("¿Olvidaste tu contraseña?", fontSize = 11.sp, color = BlueMid)
                    }
                }

                Button(
                    onClick = {
                        if (email.isBlank() || password.isBlank()) {
                            errorMessage = "Completa todos los campos"
                        } else {
                            isLoading = true
                            auth.signInWithEmailAndPassword(email, password)
                                .addOnSuccessListener {
                                    isLoading = false
                                    onLoginSuccess(null)
                                }
                                .addOnFailureListener { e ->
                                    isLoading = false
                                    errorMessage = when {
                                        e.message?.contains("no user") == true -> "Correo no registrado"
                                        e.message?.contains("password") == true -> "Contraseña incorrecta"
                                        else -> "Error: ${e.message}"
                                    }
                                }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BlueDeep)
                ) {
                    Text("Ingresar", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("¿No tienes cuenta? ", fontSize = 12.sp, color = GrayText)
                    Text("Regístrate", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = BlueMid,
                        modifier = Modifier.clickable { onNavigateToRegister() })
                }
            }
        }
    }
}