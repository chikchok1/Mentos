package com.example.personalfinance.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.personalfinance.ui.screens.HomeScreen
import com.example.personalfinance.ui.screens.LedgerScreen
import com.example.personalfinance.ui.screens.MenuScreen
import com.example.personalfinance.ui.screens.NewRecordScreen

// ── Routes ────────────────────────────────────────────────────────────────────
// Sealed class mirrors the React Router paths ("/", "/ledger", "/new-record", "/menu")

sealed class Screen(val route: String) {
    object Home      : Screen("home")
    object Ledger    : Screen("ledger")
    object NewRecord : Screen("new_record")
    object Menu      : Screen("menu")
}

// ── Navigation Host ───────────────────────────────────────────────────────────

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController    = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route)      { HomeScreen(navController)      }
        composable(Screen.Ledger.route)    { LedgerScreen(navController)    }
        composable(Screen.NewRecord.route) { NewRecordScreen(navController) }
        composable(Screen.Menu.route)      { MenuScreen(navController)      }
    }
}
