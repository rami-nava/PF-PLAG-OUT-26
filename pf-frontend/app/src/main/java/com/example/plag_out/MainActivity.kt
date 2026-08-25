package com.example.plag_out

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
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
import androidx.compose.runtime.DisposableEffect
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
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
import com.example.plag_out.AlmacenamientoLocal.UsuarioRepository
import com.example.plag_out.Service.RetrofitClient
import com.example.plag_out.ui.theme.PlagOutColors
import com.example.plag_out.ui.theme.PlagasGDDTheme
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import kotlin.collections.contains

import android.preference.PreferenceManager
import org.osmdroid.config.Configuration

class MainActivity : ComponentActivity() {

    // monitoreo_id pendiente de un tap en una notificación; lo observa AppNavigation
    // para hacer el deep-link una vez que hay sesión y NavController.
    private val deepLinkMonitoreoId = mutableStateOf<String?>(null)
    private val deepLinkReporteId = mutableStateOf<String?>(null)
    private val deepLinkPlantacionId = mutableStateOf<String?>(null)

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val osmConfig = Configuration.getInstance()
        osmConfig.userAgentValue = "PlagOutMobileApp/1.0.1 (contacto@mi-app.com)"
        osmConfig.osmdroidBasePath = java.io.File(cacheDir, "osmdroid")
        osmConfig.osmdroidTileCache = java.io.File(cacheDir, "osmdroid/tiles")
        osmConfig.load(applicationContext, applicationContext.getSharedPreferences("plag_out_prefs", android.content.Context.MODE_PRIVATE))
        android.util.Log.d("OSM_DEBUG", "User-Agent en MainActivity: ${osmConfig.userAgentValue}")
        
        leerDeepLink(intent)
        setContent {
            PlagasGDDTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(
                        deepLinkMonitoreoId = deepLinkMonitoreoId.value,
                        onDeepLinkMonitoreoConsumido = { deepLinkMonitoreoId.value = null },
                        deepLinkReporteId = deepLinkReporteId.value,
                        onDeepLinkReporteConsumido = { deepLinkReporteId.value = null },
                        deepLinkPlantacionId = deepLinkPlantacionId.value,
                        onDeepLinkPlantacionConsumido = { deepLinkPlantacionId.value = null }
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
        intent?.getStringExtra(PlagOutMessagingService.EXTRA_MONITOREO_ID)?.let {
            deepLinkMonitoreoId.value = it
        }
        intent?.getStringExtra(PlagOutMessagingService.EXTRA_REPORTE_ID)?.let {
            deepLinkReporteId.value = it
        }
        intent?.getStringExtra(PlagOutMessagingService.EXTRA_PLANTACION_ID)?.let {
            deepLinkPlantacionId.value = it
        }
        // Push genérico (solo entidad_id): el tipo dice a qué entidad apunta ese id.
        intent?.getStringExtra(PlagOutMessagingService.EXTRA_ENTIDAD_ID)?.let { id ->
            when {
                intent.getStringExtra(PlagOutMessagingService.EXTRA_TIPO)
                    ?.uppercase()?.contains("BIOFIX") == true -> deepLinkPlantacionId.value = id
                deepLinkMonitoreoId.value == null && deepLinkReporteId.value == null &&
                    deepLinkPlantacionId.value == null -> deepLinkReporteId.value = id
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AppNavigation(
    deepLinkMonitoreoId: String? = null,
    onDeepLinkMonitoreoConsumido: () -> Unit = {},
    deepLinkReporteId: String? = null,
    onDeepLinkReporteConsumido: () -> Unit = {},
    deepLinkPlantacionId: String? = null,
    onDeepLinkPlantacionConsumido: () -> Unit = {}
) {
    val context = LocalContext.current.applicationContext
    val db = remember(context) { AppDatabase.getDatabase(context) }
    val monitoreoRepository = remember(db) { MonitoreoRepository(db.monitoreoDao()) }
    val terrenoRepository = remember(db) { TerrenoRepository(db.terrenoDao()) }
    val plantacionRepository = remember(db) { PlantacionRepository(db.plantacionDao()) }
    val usuarioRepository = remember(db) { UsuarioRepository(db.usuarioDao()) }

    val monitoreosViewModel: MonitoreosViewModel = viewModel(
        factory = remember(context, monitoreoRepository) { MonitoreosViewModelFactory(context, monitoreoRepository) }
    )
    val terrenosViewModel: TerrenosViewModel = viewModel(
        factory = remember(context, terrenoRepository) { TerrenosViewModelFactory(context, terrenoRepository) }
    )
    val plantacionesViewModel: PlantacionesViewModel = viewModel(
        factory = remember(context, plantacionRepository) { PlantacionesViewModelFactory(context, plantacionRepository) }
    )
    val nuevoTerrenoViewModel: NuevoTerrenoViewModel = viewModel(
        factory = remember(context, terrenoRepository) { NuevoTerrenoViewModelFactory(context, terrenoRepository) }
    )
    val misReportesViewModel: MisReportesViewModel = viewModel(
        factory = MisReportesViewModelFactory()
    )

    val authViewModel: AuthViewModel = viewModel(
        factory = remember(context, monitoreoRepository, terrenoRepository, plantacionRepository, usuarioRepository) {
            AuthViewModel.AuthViewModelFactory(
                SupabaseProvider.client, context,
                monitoreoRepository, terrenoRepository, plantacionRepository, usuarioRepository
            )
        }
    )
    val userViewModel: UserViewModel = viewModel(
        factory = remember(usuarioRepository) { UserViewModelFactory(usuarioRepository) }
    )
    val notificacionesViewModel: NotificacionesViewModel = viewModel(
        factory = remember { NotificacionesViewModelFactory() }
    )

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
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                FcmTokenRegistrar.registrar()
            }
        }
    }

    // startDestination se fija una sola vez (arriba) leyendo sessionStatus en ese momento. Si
    // sessionStatus pasó primero por NotAuthenticated y recién después (al terminar de cargar la
    // sesión guardada) llegó a Authenticated, el NavHost ya arrancó en "logIn" y se queda ahí sin
    // este empujón: si nos autenticamos estando parados en el login, saltamos a monitoreos.
    LaunchedEffect(sessionStatus) {
        val currentRoute = navBackStackEntry?.destination?.route
        if (sessionStatus is SessionStatus.Authenticated && currentRoute == "logIn") {
            navController.navigate("monitoreos") {
                popUpTo("logIn") { inclusive = true }
            }
        }
    }

    // Deep-link desde una notificación: cada aviso lleva a la entidad que lo originó
    // (monitoreo para las alertas de GDD, reporte para los reportes cercanos y plantación
    // para el biofix).
    LaunchedEffect(deepLinkMonitoreoId, sessionStatus) {
        if (deepLinkMonitoreoId != null && sessionStatus is SessionStatus.Authenticated) {
            navController.navigate("monitoreo/$deepLinkMonitoreoId")
            onDeepLinkMonitoreoConsumido()
        }
    }

    LaunchedEffect(deepLinkReporteId, sessionStatus) {
        if (deepLinkReporteId != null && sessionStatus is SessionStatus.Authenticated) {
            navController.navigate("ver_reporte/$deepLinkReporteId")
            onDeepLinkReporteConsumido()
        }
    }

    // Aviso de biofix: la entidad es la plantación que arrancó a acumular GDD.
    LaunchedEffect(deepLinkPlantacionId, sessionStatus) {
        if (deepLinkPlantacionId != null && sessionStatus is SessionStatus.Authenticated) {
            navController.navigate("plantacion/$deepLinkPlantacionId")
            onDeepLinkPlantacionConsumido()
        }
    }

    val notificacionesState by notificacionesViewModel.state.collectAsState()
    var mostrarNotificaciones by remember { mutableStateOf(false) }

    // Recién autenticado (login o sesión restaurada): el ON_RESUME de abajo ya pasó con la app
    // en primer plano, así que sin esto el badge se quedaría en cero hasta minimizar y volver.
    LaunchedEffect(sessionStatus) {
        if (sessionStatus is SessionStatus.Authenticated) notificacionesViewModel.cargar()
    }

    // Y cada vez que se vuelve del fondo: si llegó un push mientras la app estaba minimizada,
    // el contador lo refleja al volver.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, sessionStatus) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && sessionStatus is SessionStatus.Authenticated) {
                notificacionesViewModel.cargar()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Push recibido con la app ya abierta: ON_RESUME no dispara en ese caso porque nunca se fue
    // a segundo plano, así que sin esto la campana se queda desactualizada hasta el próximo clic.
    LaunchedEffect(Unit) {
        NotificacionesEventBus.eventos.collect {
            if (sessionStatus is SessionStatus.Authenticated) notificacionesViewModel.cargar()
        }
    }

    val fullBleedScreens = listOf("logIn", "crearCuenta", "editarPerfil", "monitoreo/{monitoreo_id}", "ver_reporte/{reporte_id}", "ver_reporte/{reporte_id}/{reporte_json}")
    val rutaActual = navBackStackEntry?.destination?.route

    // Cerrar sesión: AuthViewModel borra el almacenamiento local (token, Room,
    // marcas de caché); acá se descarta además el estado en memoria de los
    // ViewModels y se vuelve al login vaciando el back stack
    val limpiarSesion: (Boolean) -> Unit = { desregistrarDispositivo ->
        authViewModel.cerrarSesion(desregistrarDispositivo) {
            userViewModel.limpiar()
            monitoreosViewModel.limpiar()
            terrenosViewModel.limpiar()
            plantacionesViewModel.limpiar()
            notificacionesViewModel.limpiar()
            misReportesViewModel.limpiar()
            navController.navigate("logIn") {
                popUpTo(0) { inclusive = true }
            }
        }
    }
    val cerrarSesion: () -> Unit = { limpiarSesion(true) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            if (shouldShowTopBar(navController)) {
                TopBar(
                    noLeidas = notificacionesState.noLeidas,
                    onNotificacionesClick = {
                        notificacionesViewModel.cargar()
                        mostrarNotificaciones = true
                    }
                )
            }
        },
        bottomBar = {
            if (shouldShowBottomBar(navController)) {
                BottomNavigationBar(navController)
            }
        }
    ) { paddingValues ->
        //Hay pantallas que al no tener topbar y bottom bar tienen padding 0
        //para ocupar toda la pantalla y ocupar el espacio del scaffold
        val paddingContenido = when {
            fullBleedScreens.contains(rutaActual) -> PaddingValues(0.dp)
            // El perfil es pestaña (necesita dejar libre la barra inferior) pero dibuja su propio
            // header detrás de la status bar: sin el top del Scaffold no se duplica ese inset.
            rutaActual == "perfil" -> PaddingValues(bottom = paddingValues.calculateBottomPadding())
            else -> paddingValues
        }

        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(paddingContenido)
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
                MonitoreosScreen(monitoreosViewModel, plantacionesViewModel, terrenosViewModel,navController)
            }
            composable("perfil") {
                PerfilScreen(
                    userViewModel = userViewModel,
                    terrenosViewModel = terrenosViewModel,
                    plantacionesViewModel = plantacionesViewModel,
                    monitoreosViewModel = monitoreosViewModel,
                    authViewModel = authViewModel,
                    onEditarPerfil = {
                        navController.navigate("editarPerfil") { launchSingleTop = true }
                    },
                    onCerrarSesion = cerrarSesion,
                    // La cuenta ya se borró en el backend: queda limpiar el dispositivo y volver
                    // al login, igual que el cierre de sesión pero sin desregistrar el token FCM
                    // (el DELETE /usuarios/me ya se llevó los dispositivos junto con el usuario).
                    onCuentaEliminada = { limpiarSesion(false) }
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
                            // El aviso va acá y no en la pantalla de edición porque esta se
                            // desmonta al volver al perfil.
                            Toast.makeText(
                                context,
                                "Perfil actualizado.",
                                Toast.LENGTH_SHORT
                            ).show()
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
                MonitoreosPorPlantacion(
                    plantacionId = plantacionId,
                    viewModel = monitoreosViewModel,
                    plantacionesViewModel = plantacionesViewModel,
                    onBack = { navController.popBackStack() },
                    onMonitoreoClick = { monitoreoId -> navController.navigate("monitoreo/$monitoreoId") },
                    onAgregarMonitoreo = { navController.navigate("agregar_monitoreo/$plantacionId") }
                )
            }
            composable("monitoreo/{monitoreo_id}") { backStackEntry ->
                val monitoreoId =
                    backStackEntry.arguments?.getString("monitoreo_id")?.toInt() ?: 0
                val detalleViewModel: MonitoreoDetalleViewModel =
                    viewModel(factory = MonitoreoDetalleViewModelFactory(context, monitoreoRepository))
                MonitoreoDetalleScreen(
                    monitoreoId = monitoreoId,
                    viewModel = detalleViewModel,
                    onBack = { navController.popBackStack() },
                    onFinalizado = {
                        monitoreosViewModel.refrescar()
                        navController.popBackStack()
                    },
                    onVerPlantacion = { plantacionId -> navController.navigate("plantacion/$plantacionId") },
                    onVerTerreno = { terrenoId -> navController.navigate("terreno/$terrenoId") }
                )
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
                    onBack = { creados ->
                        monitoreosViewModel.agregarEnMemoria(creados)
                        navController.popBackStack()
                    },
                    onSuccess = { creados ->
                        monitoreosViewModel.agregarEnMemoria(creados)
                        navController.popBackStack()
                    }
                )
            }
            // Alta desde la pantalla de una plantación: terreno y plantación ya vienen decididos.
            composable("agregar_monitoreo/{plantacion_id}") { backStackEntry ->
                val agregarMonitoreoViewModel: AgregarMonitoreoViewModel = viewModel(factory = AgregarMonitoreoViewModelFactory(context, monitoreoRepository, plantacionRepository, terrenoRepository))
                val plantacionId = backStackEntry.arguments?.getString("plantacion_id")?.toIntOrNull()

                AgregarMonitoreoScreen(
                    viewModel = agregarMonitoreoViewModel,
                    plantacionId = plantacionId,
                    onBack = { creados ->
                        monitoreosViewModel.agregarEnMemoria(creados)
                        navController.popBackStack()
                    },
                    onSuccess = { creados ->
                        monitoreosViewModel.agregarEnMemoria(creados)
                        navController.popBackStack()
                    }
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
            composable("crear_reporte") {
                val crearReporteViewModel: CrearReporteViewModel = viewModel(
                    factory = CrearReporteViewModelFactory(context)
                )
                CrearReporteScreen(
                    viewModel = crearReporteViewModel,
                    onBack = { navController.popBackStack() },
                    onSuccess = { reporteId, reporteJson ->
                        navController.navigate("ver_reporte/$reporteId/$reporteJson") {
                            popUpTo("crear_reporte") { inclusive = true }
                        }
                    }
                )
            }
            composable("ver_reporte/{reporte_id}") { backStackEntry ->
                val reporteId = backStackEntry.arguments?.getString("reporte_id")?.toIntOrNull() ?: 0
                val verReporteViewModel: VerReporteViewModel = viewModel(
                    factory = VerReporteViewModelFactory()
                )
                VerReporteScreen(
                    reporteId = reporteId,
                    reporteJsonFallback = null,
                    viewModel = verReporteViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("ver_reporte/{reporte_id}/{reporte_json}") { backStackEntry ->
                val reporteId = backStackEntry.arguments?.getString("reporte_id")?.toIntOrNull() ?: 0
                val reporteJson = backStackEntry.arguments?.getString("reporte_json")
                val verReporteViewModel: VerReporteViewModel = viewModel(
                    factory = VerReporteViewModelFactory()
                )
                VerReporteScreen(
                    reporteId = reporteId,
                    reporteJsonFallback = reporteJson,
                    viewModel = verReporteViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("reportes") {
                MisReportesScreen(misReportesViewModel, navController)
            }
        }
    }

    // Fuera del Scaffold: la hoja se dibuja en su propia ventana y tiene que poder taparlo
    // entero, incluidas las barras.
    if (mostrarNotificaciones) {
        NotificacionesSheet(
            state = notificacionesState,
            onMarcarLeida = { notificacionesViewModel.marcarLeida(it) },
            onNavegar = { destino -> navController.navigate(destino) },
            onDismiss = {
                mostrarNotificaciones = false
                notificacionesViewModel.descartarError()
            }
        )
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
    val screensWithoutNavBar = listOf("datos_terreno", "seleccionar_ubicacion", "seleccionar_cultivo", "agregar_plantacion/{terreno_id}", "agregar_monitoreo", "agregar_monitoreo/{plantacion_id}", "logIn", "crearCuenta", "editarPerfil", "monitoreo/{monitoreo_id}", "crear_reporte", "ver_reporte/{reporte_id}", "ver_reporte/{reporte_id}/{reporte_json}")
    return !screensWithoutNavBar.contains(currentScreen)
}

@Composable
fun shouldShowTopBar(navController: NavController): Boolean {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentScreen = navBackStackEntry?.destination?.route
    val screensWithoutNavBar = listOf("logIn","crearCuenta","perfil","editarPerfil","monitoreo/{monitoreo_id}","ver_reporte/{reporte_id}","ver_reporte/{reporte_id}/{reporte_json}")
    return !screensWithoutNavBar.contains(currentScreen)
}