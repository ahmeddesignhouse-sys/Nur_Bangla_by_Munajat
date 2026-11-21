package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.ui.NoorApp
import com.example.util.DailyInspirationCatalog

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DailyInspirationCatalog.initialize(applicationContext)
        enableEdgeToEdge()
        setContent {
            NoorApp()
        }
    }
}
