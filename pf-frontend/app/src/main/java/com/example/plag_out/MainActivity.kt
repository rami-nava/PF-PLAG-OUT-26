package com.example.plag_out

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.plag_out.ui.theme.PlagasGDDTheme

class MainActivity : ComponentActivity() {

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PlagasGDDTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AppNavigation() {
    val monitoreosViewModel: MonitoreosViewModel = viewModel()
    val terrenosViewModel: TerrenosViewModel = viewModel()
    val plantacionesViewModel: PlantacionesViewModel = viewModel()
    val nuevoTerrenoViewModel: NuevoTerrenoViewModel = viewModel()
    val navController = rememberNavController()

    Scaffold(containerColor = MaterialTheme.colorScheme.background,
        topBar = {TopBar()},//color del background
        bottomBar = { if (shouldShowBottomBar(navController)){ BottomNavigationBar(navController)}})
    { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = "monitoreos",
            modifier = Modifier.padding(paddingValues)
        ) {
            composable("monitoreos") {
                MonitoreosScreen(monitoreosViewModel, plantacionesViewModel,navController)
            }
            composable("terrenos") {
                TerrenosScreen(terrenosViewModel, monitoreosViewModel,navController)
            }
            composable("plantacion/{plantacion_id}") { backStackEntry ->
                val plantacionId =
                    backStackEntry.arguments?.getString("plantacion_id")?.toInt() ?: 0
                MonitoreosPorPlantacion(plantacionId, monitoreosViewModel) {
                    navController.popBackStack()
                }
            }
            composable("seleccionar_cultivo") {
                SelectCultivoScreen(
                    nuevoTerrenoViewModel = nuevoTerrenoViewModel,
                    onBack = { navController.popBackStack() },
                    onContinue = { navController.navigate("seleccionar_ubicacion") }
                )
            }
            composable("seleccionar_ubicacion") {
                SelectLocationScreen(
                    nuevoTerrenoViewModel = nuevoTerrenoViewModel,
                    onBack = {
                        navController.popBackStack()
                    },
                    onConfirm = {
                        nuevoTerrenoViewModel.registrarTerreno {
                            navController.navigate("terrenos") {
                                popUpTo("terrenos") { inclusive = false }
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun shouldShowBottomBar(navController: NavController): Boolean {
//Para que se actualize cada vez que se cambia de pantalla
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentScreen = navBackStackEntry?.destination?.route

    val screensWithOutNavBar = listOf("seleccionar_cultivo", "seleccionar_ubicacion")

    return !screensWithOutNavBar.contains(currentScreen);
}
