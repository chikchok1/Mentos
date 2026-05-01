package com.example.personalfinance

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.personalfinance.navigation.AppNavigation
import com.example.personalfinance.ui.theme.PersonalFinanceTheme

class MainActivity : ComponentActivity() {
    private lateinit var tokenManager: com.example.personalfinance.data.TokenManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tokenManager = com.example.personalfinance.data.TokenManager(this)
        
        enableEdgeToEdge()
        setContent {
            PersonalFinanceTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color    = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(tokenManager = tokenManager)
                }
            }
        }
    }
}
