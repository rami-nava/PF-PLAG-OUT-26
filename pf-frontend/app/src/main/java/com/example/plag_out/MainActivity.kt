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
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.plag_out.AlmacenamientoLocal.AppDatabase
import com.example.plag_out.AlmacenamientoLocal.MonitoreoRepository
import com.example.plag_out.AlmacenamientoLocal.PlantacionRepository
import com.example.plag_out.AlmacenamientoLocal.TerrenoRepository
import com.example.plag_out.ui.theme.PlagasGDDTheme
import kotlin.collections.contains

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
    val db = AppDatabase.getDatabase(LocalContext.current)
    val context = LocalContext.current
    val monitoreoRepository = MonitoreoRepository(db.monitoreoDao())
    val terrenoRepository = TerrenoRepository(db.terrenoDao())
    val plantacionRepository = PlantacionRepository(db.plantacionDao())

    val monitoreosViewModel: MonitoreosViewModel = viewModel(factory = MonitoreosViewModelFactory(context, monitoreoRepository))
    val terrenosViewModel: TerrenosViewModel = viewModel(factory = TerrenosViewModelFactory(context, terrenoRepository))
    val plantacionesViewModel: PlantacionesViewModel = viewModel(factory = PlantacionesViewModelFactory(context, plantacionRepository))
    val nuevoTerrenoViewModel: NuevoTerrenoViewModel = viewModel(factory = NuevoTerrenoViewModelFactory(context, terrenoRepository))

    val navController = rememberNavController()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { TopBar() },
        bottomBar = {
            if (shouldShowBottomBar(navController)) {
                BottomNavigationBar(navController)
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = "monitoreos",
            modifier = Modifier.padding(paddingValues)
        ) {
            composable("monitoreos") {
                MonitoreosScreen(monitoreosViewModel, plantacionesViewModel, navController)
            }
            composable("terrenos") {
                TerrenosScreen(terrenosViewModel, monitoreosViewModel, nuevoTerrenoViewModel, navController)
            }
            composable("plantacion/{plantacion_id}") { backStackEntry ->
                val plantacionId =
                    backStackEntry.arguments?.getString("plantacion_id")?.toInt() ?: 0
                MonitoreosPorPlantacion(plantacionId, monitoreosViewModel) {
                    navController.popBackStack()
                }
            }
            composable("terreno/{terreno_id}") { backStackEntry ->
                val terrenoId =
                    backStackEntry.arguments?.getString("terreno_id")?.toInt() ?: 0
                PlantacionesPorTerreno(
                    terrenoId, plantacionesViewModel,
                    monitoreosViewModel, terrenosViewModel
                ) {
                    navController.popBackStack()
                }
            }
            composable("datos_terreno") {
                DatosDelTerrenoScreen(
                    nuevoTerrenoViewModel = nuevoTerrenoViewModel,
                    onBack = { navController.popBackStack() },
                    onContinue = { navController.navigate("seleccionar_ubicacion") }
                )
            }
            /*composable("seleccionar_cultivo") {
                SelectCultivoScreen(
                    nuevoTerrenoViewModel = nuevoTerrenoViewModel,
                    onBack = { navController.popBackStack() },
                    onContinue = { navController.navigate("") }
                )
            }*/
            composable("seleccionar_ubicacion") {
                SelectLocationScreen(
                    nuevoTerrenoViewModel = nuevoTerrenoViewModel,
                    onBack = { navController.popBackStack() },
                    onConfirm = {
                        navController.navigate("terrenos") {
                            popUpTo("terrenos") { inclusive = false }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun shouldShowBottomBar(navController: NavController): Boolean {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentScreen = navBackStackEntry?.destination?.route
    val screensWithoutNavBar = listOf("datos_terreno", "seleccionar_ubicacion", "seleccionar_cultivo")
    return !screensWithoutNavBar.contains(currentScreen)
}