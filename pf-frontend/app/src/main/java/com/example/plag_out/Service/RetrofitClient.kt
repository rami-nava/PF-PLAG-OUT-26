package com.example.plag_out.Service

import android.os.Build
import androidx.annotation.RequiresApi
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializer
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializer
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import com.example.plag_out.SupabaseProvider
import io.github.jan.supabase.auth.auth

object RetrofitClient {
    @RequiresApi(Build.VERSION_CODES.O)
    private val gson = GsonBuilder()
        .registerTypeAdapter(LocalDate::class.java, JsonDeserializer { json, _, _ ->
            LocalDate.parse(json.asString)
        })
        .registerTypeAdapter(LocalTime::class.java, JsonDeserializer { json, _, _ ->
            LocalTime.parse(json.asString)
        })
        // Para convertir LOCALDATETIME presente en notificaciones ("Hace 5 min")
        .registerTypeAdapter(LocalDateTime::class.java, JsonDeserializer { json, _, _ ->
            val texto = json.asString
            runCatching { OffsetDateTime.parse(texto).toLocalDateTime() }
                .getOrElse { LocalDateTime.parse(texto) }
        })
        .registerTypeAdapter(LocalDateTime::class.java, JsonSerializer<LocalDateTime> { fecha, _, _ ->
            JsonPrimitive(fecha.toString())
        })
        .registerTypeAdapter(LocalDate::class.java, JsonSerializer<LocalDate> { date, _, _ ->
            JsonPrimitive(date.toString())
        })
        .registerTypeAdapter(LocalTime::class.java, JsonSerializer<LocalTime> { time, _, _ ->
            JsonPrimitive(time.toString())
        })
        .create()

    private val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            // El token de Supabase expira (~1h) y el SDK lo renueva en memoria:
            // leerlo en cada request evita mandar un JWT vencido.
            val token = runCatching { SupabaseProvider.client.auth.currentAccessTokenOrNull() }
                .getOrNull()
            val builder = chain.request().newBuilder()
            if (token != null) {
                builder.addHeader("Authorization", "Bearer $token")
            }
            chain.proceed(builder.build())
        }
        .build()

    @RequiresApi(Build.VERSION_CODES.O)
    private val retrofit = Retrofit.Builder()
        .addConverterFactory(GsonConverterFactory.create(gson))
        .baseUrl("http://10.0.2.2:8000/")
        //.baseUrl("https://plag-out-backend.vercel.app")
        .client(client)
        .build()

    @RequiresApi(Build.VERSION_CODES.O)
    val gddService: GDDService = retrofit.create(GDDService::class.java)
}
