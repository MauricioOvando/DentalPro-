package com.example.dentalpro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.dentalpro.ui.DentalProNavGraph
import com.example.dentalpro.ui.theme.DentalPROTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DentalPROTheme {
                DentalProNavGraph()
            }
        }
    }
}