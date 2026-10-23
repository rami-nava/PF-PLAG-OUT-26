import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
}

// Valores públicos del proyecto Supabase vigente: la clave publicable está pensada para viajar
// en la app. local.properties puede pisarlos (supabase.url / supabase.publishable.key), y en
// la migración de proyecto se cambian acá.
val supabaseUrlPorDefecto = "https://djhrmrvcffolxzvcqlux.supabase.co"
val supabaseClavePorDefecto = "sb_publishable_tB5w0haD6JUc2O4ORfWlqA_UWdh5yU9"

android {
    val localProperties = Properties().apply {
        val file = rootProject.file("local.properties")
        if (file.exists()) {
            file.inputStream().use { load(it) }
        }
    }
    fun local(clave: String) = (localProperties.getProperty(clave) ?: "").trim().trim('"')

    var backendUrl = local("backend.url").ifEmpty { "http://10.0.2.2:8000/" }
    if (!backendUrl.endsWith("/")) {
        backendUrl = "$backendUrl/"
    }
    val supabaseUrl = local("supabase.url").ifEmpty { supabaseUrlPorDefecto }
    val supabaseClave = local("supabase.publishable.key").ifEmpty { supabaseClavePorDefecto }

    // Firma del release: la clave del proyecto (CN=Plag Out Android Release). La ruta y las
    // contraseñas viven solo en local.properties, que no se versiona.
    val keystore = local("release.storeFile")
    val firmaConfigurada = listOf(keystore, local("release.storePassword"), local("release.keyAlias"), local("release.keyPassword"))
        .all { it.isNotEmpty() }

    namespace = "com.example.plag_out"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.plag_out"
        minSdk = 24
        targetSdk = 36
        versionCode = 2
        versionName = "2.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "BACKEND_URL", "\"$backendUrl\"")
        buildConfigField("String", "SUPABASE_URL", "\"$supabaseUrl\"")
        buildConfigField("String", "SUPABASE_PUBLISHABLE_KEY", "\"$supabaseClave\"")
    }

    signingConfigs {
        if (firmaConfigurada) {
            create("release") {
                storeFile = file(keystore)
                storePassword = local("release.storePassword")
                keyAlias = local("release.keyAlias")
                keyPassword = local("release.keyPassword")
                if (keystore.endsWith(".p12") || keystore.endsWith(".pfx")) storeType = "pkcs12"
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (firmaConfigurada) signingConfig = signingConfigs.getByName("release")
        }
    }

    // Un release que se reparte no puede salir apuntando al emulador, sin HTTPS o sin firmar:
    // la app quedaría muerta sin avisar (el tráfico en claro está bloqueado en release).
    val problemasRelease = buildList {
        if (!backendUrl.startsWith("https://")) add("backend.url tiene que ser https:// (hoy: $backendUrl)")
        if (!supabaseUrl.startsWith("https://")) add("supabase.url tiene que ser https://")
        if (!firmaConfigurada) add("faltan release.storeFile, release.storePassword, release.keyAlias o release.keyPassword")
    }
    val validarRelease = tasks.register("validarConfiguracionRelease") {
        doLast {
            if (problemasRelease.isNotEmpty()) {
                throw GradleException(
                    "El release no se puede compilar. Revisá pf-frontend/local.properties:\n- " +
                        problemasRelease.joinToString("\n- ")
                )
            }
        }
    }
    tasks.matching { it.name == "preReleaseBuild" }.configureEach { dependsOn(validarRelease) }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }

    // Fakes compartidos entre los tests JVM (test) y los instrumentados (androidTest)
    sourceSets {
        getByName("test") { java.srcDir("src/testShared/java") }
        getByName("androidTest") { java.srcDir("src/testShared/java") }
    }
}


kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:5.0.0-alpha.14")
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.3")
    implementation("androidx.work:work-runtime-ktx:2.10.1")
    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    implementation(libs.play.services.location)
    implementation("org.osmdroid:osmdroid-android:6.1.20")
    implementation(libs.ui)


    implementation("androidx.compose.material:material-icons-extended:1.7.5")
    ksp(libs.room.compiler)
    implementation(libs.androidx.navigation.compose)
    implementation("com.google.maps.android:maps-compose:4.3.3")
    implementation("com.google.android.gms:play-services-maps:18.2.0")

    // Supabase
    implementation(platform(libs.supabase.bom))
    implementation(libs.supabase.auth)
    implementation(libs.supabase.postgrest)
    implementation(libs.ktor.client.android)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)

    // Firebase Cloud Messaging (push notifications)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
    // El FID con el que se registra el dispositivo (firebase-messaging 25.1.0+).
    implementation(libs.firebase.installations)

    testImplementation(libs.androidx.ui.test.junit4)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}