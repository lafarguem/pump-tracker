package com.lafarguem.pumptracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.lafarguem.pumptracker.ui.PumpApp
import com.lafarguem.pumptracker.ui.PumpTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PumpTheme {
                PumpApp()
            }
        }
    }
}
