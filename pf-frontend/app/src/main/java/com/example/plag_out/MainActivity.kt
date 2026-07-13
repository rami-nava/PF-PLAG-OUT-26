package com.example.plag_out

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
import com.example.plag_out.Service.RetrofitClient
import com.example.plag_out.ui.theme.PlagOutColors
import com.example.plag_out.ui.theme.PlagasGDDTheme
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
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
    // applicationContext: los ViewModels sobreviven a la Activity (rotación) y
    // guardarles el context de la Activity la dejaría retenida en memoria
    val context = LocalContext.current.applicationContext
    val monitoreoRepository = MonitoreoRepository(db.monitoreoDao())
    val terrenoRepository = TerrenoRepository(db.terrenoDao())
    val plantacionRepository = PlantacionRepository(db.plantacionDao())

    val monitoreosViewModel: MonitoreosViewModel = viewModel(factory = MonitoreosViewModelFactory(context, monitoreoRepository))
    val terrenosViewModel: TerrenosViewModel = viewModel(factory = TerrenosViewModelFactory(context, terrenoRepository))
    val plantacionesViewModel: PlantacionesViewModel = viewModel(factory = PlantacionesViewModelFactory(context, plantacionRepository))
    val nuevoTerrenoViewModel: NuevoTerrenoViewModel = viewModel(factory = NuevoTerrenoViewModelFactory(context, terrenoRepository))
    
    val sessionManager = SessionManager(context)
    RetrofitClient.setSessionManager(sessionManager)

    val authViewModel: AuthViewModel = viewModel(
        factory = AuthViewModel.AuthViewModelFactory(SupabaseProvider.client, sessionManager)
    )

    // Supabase restaura la sesión guardada al iniciar (y renueva el token si hace
    // falta). Mientras tanto el estado es Initializing: mostramos un splash y recién
    // después decidimos si arrancar en el home o en el login.
    val sessionStatus by SupabaseProvider.client.auth.sessionStatus.collectAsState()
    if (sessionStatus is SessionStatus.Initializing) {
        PantallaCargandoSesion()
        return
    }
    val startDestination = remember {
        if (sessionStatus is SessionStatus.Authenticated) "monitoreos" else "logIn"
    }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val fullBleedScreens = listOf("logIn", "crearCuenta")
    val isFullBleedRoute = fullBleedScreens.contains(navBackStackEntry?.destination?.route)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            if (shouldShowTopBar(navController)) {
                TopBar()
            }
        },
        bottomBar = {
            if (shouldShowBottomBar(navController)) {
                BottomNavigationBar(navController)
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(if (isFullBleedRoute) PaddingValues(0.dp) else paddingValues)
        ) {
            composable("logIn") {
                LoginScreen(
                    authViewModel,
                    // Sacar el login del back stack: "atrás" desde home no debe volver al login
                    {
                        navController.navigate("monitoreos") {
                            popUpTo("logIn") { inclusive = true }
                        }
                    },
                    { navController.navigate("crearCuenta") }
                )
            }
            composable("crearCuenta") {
                CrearCuentaScreen(authViewModel,{navController.popBackStack()},{navController.popBackStack()})
            }
            composable("monitoreos") {
                MonitoreosScreen(monitoreosViewModel, plantacionesViewModel, navController)
            }
            composable("terrenos") {
                TerrenoScreen(terrenosViewModel, monitoreosViewModel, plantacionesViewModel,nuevoTerrenoViewModel, navController)
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
                    monitoreosViewModel, terrenosViewModel,
                    navController
                ) {
                    navController.popBackStack()
                }
            }
            composable("agregar_plantacion/{terreno_id}") { backStackEntry ->
                val agregarPlantacionViewModel: AgregarPlantacionViewModel = viewModel(factory = AgregarPlantacionViewModelFactory(context, plantacionRepository, terrenoRepository))
                val terrenoId =
                    backStackEntry.arguments?.getString("terreno_id")?.toInt() ?: 0
                AgregarPlantacionScreen(
                    terrenoId = terrenoId,
                    viewModel = agregarPlantacionViewModel,
                    onBack = { navController.popBackStack() },
                    onSuccess = {
                        plantacionesViewModel.getPlantaciones()
                        navController.popBackStack()
                    }
                )
            }
            composable("datos_terreno") {
                DatosDelTerrenoScreen(
                    nuevoTerrenoViewModel = nuevoTerrenoViewModel,
                    onBack = { navController.popBackStack() },
                    onContinue = { navController.navigate("seleccionar_ubicacion") }
                )
            }
            composable("agregar_monitoreo") {
                val agregarMonitoreoViewModel: AgregarMonitoreoViewModel = viewModel(factory = AgregarMonitoreoViewModelFactory(context, monitoreoRepository, plantacionRepository, terrenoRepository))

                AgregarMonitoreoScreen(
                    viewModel = agregarMonitoreoViewModel,
                    onBack = { navController.popBackStack() },
                    onSuccess = { navController.popBackStack() }
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
                        nuevoTerrenoViewModel.resetState()
                        navController.navigate("terrenos") {
                            popUpTo("terrenos") { inclusive = false }
                        }
                    }
                )
            }
        }
    }
}

/** Splash mostrado mientras Supabase restaura la sesión guardada. */
@Composable
private fun PantallaCargandoSesion() {
    Box(modifier = Modifier.fillMaxSize()) {
        FondoVerdeAuth()
        CircularProgressIndicator(
            color = PlagOutColors.TextOnDark,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
fun shouldShowBottomBar(navController: NavController): Boolean {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentScreen = navBackStackEntry?.destination?.route
    val screensWithoutNavBar = listOf("datos_terreno", "seleccionar_ubicacion", "seleccionar_cultivo", "agregar_plantacion/{terreno_id}", "agregar_monitoreo","logIn","crearCuenta")
    return !screensWithoutNavBar.contains(currentScreen)
}

@Composable
fun shouldShowTopBar(navController: NavController): Boolean {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentScreen = navBackStackEntry?.destination?.route
    val screensWithoutNavBar = listOf("logIn","crearCuenta")
    return !screensWithoutNavBar.contains(currentScreen)
}