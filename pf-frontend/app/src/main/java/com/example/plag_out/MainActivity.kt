package com.example.plag_out

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.plag_out.Service.FcmTokenRegistrar
import com.example.plag_out.Service.PlagOutMessagingService
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.plag_out.AlmacenamientoLocal.AppDatabase
import com.example.plag_out.AlmacenamientoLocal.PreferenciasUsuario
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

    // plantacion_id pendiente de un tap en una notificación; lo observa AppNavigation
    // para hacer el deep-link una vez que hay sesión y NavController.
    private val deepLinkPlantacionId = mutableStateOf<String?>(null)

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        leerDeepLink(intent)
        setContent {
            PlagasGDDTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(
                        deepLinkPlantacionId = deepLinkPlantacionId.value,
                        onDeepLinkConsumido = { deepLinkPlantacionId.value = null }
                    )
                }
            }
        }
    }

    // launchMode="singleTop": si la app ya está abierta, el tap llega por acá
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        leerDeepLink(intent)
    }

    private fun leerDeepLink(intent: Intent?) {
        intent?.getStringExtra(PlagOutMessagingService.EXTRA_PLANTACION_ID)?.let {
            deepLinkPlantacionId.value = it
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AppNavigation(
    deepLinkPlantacionId: String? = null,
    onDeepLinkConsumido: () -> Unit = {}
) {
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

    val authViewModel: AuthViewModel = viewModel(
        factory = AuthViewModel.AuthViewModelFactory(
            SupabaseProvider.client, context,
            monitoreoRepository, terrenoRepository, plantacionRepository
        )
    )
    val userViewModel: UserViewModel = viewModel()

    // Supabase restaura la sesión guardada al iniciar (y renueva el token si hace
    // falta). Mientras tanto el estado es Initializing: mostramos un splash y recién
    // después decidimos si arrancar en el home o en el login.
    val sessionStatus by SupabaseProvider.client.auth.sessionStatus.collectAsState()

    // Si el token guardado está vencido, Supabase necesita red para renovarlo. Sin red (o con
    // el DNS caído) esa renovación no termina nunca y el estado se queda en Initializing: sin
    // este corte el usuario se queda mirando el spinner sin forma de salir. Pasado el límite le
    // ofrecemos ir al login.
    var esperaAgotada by remember { mutableStateOf(false) }
    var forzarLogin by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(ESPERA_MAXIMA_SESION_MS)
        esperaAgotada = true
    }

    if (sessionStatus is SessionStatus.Initializing && !forzarLogin) {
        PantallaCargandoSesion(
            esperaAgotada = esperaAgotada,
            onIrAlLogin = { forzarLogin = true }
        )
        return
    }
    val startDestination = remember {
        if (sessionStatus is SessionStatus.Authenticated) "monitoreos" else "logIn"
    }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()

    // Pedir permiso de notificaciones (Android 13+). Debajo de API 33 es implícito.
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* si lo deniega, simplemente no verá las notificaciones */ }
    LaunchedEffect(Unit) {
        // Si el usuario apagó las notificaciones desde su perfil, no tiene sentido insistir.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            PreferenciasUsuario.notificacionesActivadas(context)
        ) {
            val concedido = ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!concedido) permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Registrar el token FCM también al restaurar una sesión guardada (usuario que
    // no pasó por el login en esta ejecución). Es idempotente con el registro del login.
    // Se respeta la preferencia del perfil: si no, cada arranque volvería a activar las
    // notificaciones de quien las había apagado.
    LaunchedEffect(sessionStatus) {
        if (sessionStatus is SessionStatus.Authenticated &&
            PreferenciasUsuario.notificacionesActivadas(context)
        ) {
            FcmTokenRegistrar.registrar()
        }
    }

    // Deep-link desde una notificación: navegar a la plantación una vez autenticado.
    LaunchedEffect(deepLinkPlantacionId, sessionStatus) {
        if (deepLinkPlantacionId != null && sessionStatus is SessionStatus.Authenticated) {
            navController.navigate("plantacion/$deepLinkPlantacionId")
            onDeepLinkConsumido()
        }
    }

    val fullBleedScreens = listOf("logIn", "crearCuenta", "perfil", "editarPerfil")
    val isFullBleedRoute = fullBleedScreens.contains(navBackStackEntry?.destination?.route)

    // Cerrar sesión: AuthViewModel borra el almacenamiento local (token, Room,
    // marcas de caché); acá se descarta además el estado en memoria de los
    // ViewModels y se vuelve al login vaciando el back stack
    val cerrarSesion: () -> Unit = {
        authViewModel.cerrarSesion {
            userViewModel.limpiar()
            monitoreosViewModel.limpiar()
            terrenosViewModel.limpiar()
            plantacionesViewModel.limpiar()
            navController.navigate("logIn") {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            if (shouldShowTopBar(navController)) {
                TopBar(
                    onPerfilClick = {
                        navController.navigate("perfil") { launchSingleTop = true }
                    },
                    onCerrarSesion = cerrarSesion
                )
            }
        },
        bottomBar = {
            if (shouldShowBottomBar(navController)) {
                BottomNavigationBar(navController)
            }
        }
    ) { paddingValues ->
        NavHost(
            //Hay pantallas que al no tener topbar y bottom bar tienen padding 0
            //para ocupar toda la pantalla y ocupar el espacio del scaffold
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
            composable("perfil") {
                PerfilScreen(
                    userViewModel = userViewModel,
                    terrenosViewModel = terrenosViewModel,
                    plantacionesViewModel = plantacionesViewModel,
                    monitoreosViewModel = monitoreosViewModel,
                    authViewModel = authViewModel,
                    onBack = { navController.popBackStack() },
                    onEditarPerfil = {
                        navController.navigate("editarPerfil") { launchSingleTop = true }
                    },
                    onCerrarSesion = cerrarSesion
                )
            }
            composable("editarPerfil") {
                // Solo se llega desde el perfil, que ya cargó el usuario; si por algún motivo no
                // está, se vuelve atrás en lugar de mostrar un formulario vacío.
                val usuario = userViewModel.state.collectAsState().value.usuario
                if (usuario == null) {
                    LaunchedEffect(Unit) { navController.popBackStack() }
                } else {
                    val editarPerfilViewModel: EditarPerfilViewModel = viewModel()
                    EditarPerfilScreen(
                        usuario = usuario,
                        viewModel = editarPerfilViewModel,
                        onBack = { navController.popBackStack() },
                        onGuardado = { actualizado ->
                            userViewModel.aplicarUsuario(actualizado)
                            navController.popBackStack()
                        }
                    )
                }
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

/** Cuánto esperamos a que Supabase resuelva la sesión antes de ofrecer ir al login. */
private const val ESPERA_MAXIMA_SESION_MS = 8_000L

@Composable
private fun PantallaCargandoSesion(esperaAgotada: Boolean, onIrAlLogin: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        FondoVerdeAuth()
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(color = PlagOutColors.TextOnDark)

            if (esperaAgotada) {
                Spacer(Modifier.height(24.dp))
                Text(
                    "No pudimos verificar tu sesión",
                    color = PlagOutColors.TextOnDark,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Revisá tu conexión a internet. Podés iniciar sesión de nuevo para continuar.",
                    color = PlagOutColors.TextOnDark.copy(alpha = 0.75f),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = onIrAlLogin,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PlagOutColors.TextOnDark,
                        contentColor = PlagOutColors.Forest
                    ),
                    modifier = Modifier
                        .height(50.dp)
                        .testTag("btnIrAlLogin")
                ) {
                    Text("Ir al inicio de sesión", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun shouldShowBottomBar(navController: NavController): Boolean {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentScreen = navBackStackEntry?.destination?.route
    val screensWithoutNavBar = listOf("datos_terreno", "seleccionar_ubicacion", "seleccionar_cultivo", "agregar_plantacion/{terreno_id}", "agregar_monitoreo","logIn","crearCuenta","perfil","editarPerfil")
    return !screensWithoutNavBar.contains(currentScreen)
}

@Composable
fun shouldShowTopBar(navController: NavController): Boolean {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentScreen = navBackStackEntry?.destination?.route
    val screensWithoutNavBar = listOf("logIn","crearCuenta","perfil","editarPerfil")
    return !screensWithoutNavBar.contains(currentScreen)
}