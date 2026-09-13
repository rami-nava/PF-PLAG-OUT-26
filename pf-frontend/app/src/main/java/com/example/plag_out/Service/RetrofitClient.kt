package com.example.plag_out.Service

import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializer
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializer
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Converter
import java.lang.reflect.Type
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import com.example.plag_out.SupabaseProvider
import io.github.jan.supabase.auth.auth

object RetrofitClient {

    /**
     * Gson descarta por defecto toda propiedad cuyo valor sea JSON null, así que el body
     * `{"umbral_alerta_ml": null}` que arma [com.example.plag_out.MonitoreoDetalleViewModel]
     * salía al cable como `{}`. El contrato de la Fase 4 distingue *omitir* el campo (preserva
     * el valor actual) de *mandar null* (limpia el override), con lo cual perder el null hacía
     * imposible volver al threshold recomendado desde la app.
     *
     * `serializeNulls()` en el [GsonBuilder] general arreglaría esto pero cambiaría el body de
     * TODOS los requests (los PATCH tipados dependen de que los campos null se omitan), así que
     * el arreglo se acota a los bodies declarados como [JsonObject] —hoy sólo el PATCH del
     * umbral ML— con un converter propio que corre antes del de Gson.
     */
    internal object JsonObjectConNullsConverterFactory : Converter.Factory() {

        private val JSON = "application/json; charset=UTF-8".toMediaType()
        private val gsonConNulls = GsonBuilder().serializeNulls().create()

        override fun requestBodyConverter(
            type: Type,
            parameterAnnotations: Array<out Annotation>,
            methodAnnotations: Array<out Annotation>,
            retrofit: Retrofit
        ): Converter<*, RequestBody>? {
            if (type != JsonObject::class.java) return null
            return Converter<JsonObject, RequestBody> { value ->
                gsonConNulls.toJson(value).toRequestBody(JSON)
            }
        }
    }

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

    internal val retrofit: Retrofit = Retrofit.Builder()
        .addConverterFactory(JsonObjectConNullsConverterFactory)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .baseUrl(com.example.plag_out.BuildConfig.BACKEND_URL)
        .client(client)
        .build()

    /** Pin each retry to one immutable session; never send A's queued payload as B. */
    fun forPresenceRetry(owner: String): GDDService {
        val retryClient = OkHttpClient.Builder().addInterceptor { chain ->
            val session = SupabaseProvider.client.auth.currentSessionOrNull()
            if (session?.user?.id != owner) throw java.io.IOException("presence_session_changed")
            chain.proceed(chain.request().newBuilder()
                .header("Authorization", "Bearer ${session.accessToken}").build())
        }.build()
        return retrofit.newBuilder().client(retryClient).build().create(GDDService::class.java)
    }

    val gddService: GDDService = retrofit.create(GDDService::class.java)
}
