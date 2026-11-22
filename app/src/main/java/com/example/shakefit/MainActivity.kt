package com.example.shakefit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.shakefit.navigation.ShakeFitNavHost
import com.example.shakefit.ui.theme.ShakeFitTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ShakeFitTheme {
                ShakeFitNavHost()
            }
        }
    }
}
