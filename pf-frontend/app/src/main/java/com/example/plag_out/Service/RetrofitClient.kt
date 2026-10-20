package com.example.plag_out.Service

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
import com.example.plag_out.CuentaSuspendidaEventBus
import com.example.plag_out.Pagina
import com.example.plag_out.SesionExpiradaEventBus
import com.example.plag_out.SupabaseProvider
import com.google.gson.reflect.TypeToken
import io.github.jan.supabase.auth.auth
import java.lang.reflect.ParameterizedType

object RetrofitClient {


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
        // Soporta tanto respuestas paginadas con objeto {"items": [...], "total": N}
        // como respuestas que devuelven directamente un arreglo JSON [...]
        .registerTypeAdapter(Pagina::class.java, JsonDeserializer<Pagina<*>> { json, typeOfT, context ->
            val parameterizedType = typeOfT as? ParameterizedType
            val itemType = parameterizedType?.actualTypeArguments?.firstOrNull() ?: Any::class.java
            val listType = TypeToken.getParameterized(List::class.java, itemType).type

            if (json.isJsonArray) {
                val list: List<Any> = context.deserialize(json, listType) ?: emptyList()
                Pagina(items = list, total = list.size)
            } else if (json.isJsonObject) {
                val obj = json.asJsonObject
                val itemsJson = obj.get("items")
                val items: List<Any>? = if (itemsJson != null && !itemsJson.isJsonNull) {
                    context.deserialize(itemsJson, listType)
                } else null
                val total = obj.get("total")?.takeIf { !it.isJsonNull }?.asInt ?: (items?.size ?: 0)
                Pagina(items = items, total = total)
            } else {
                Pagina(items = emptyList(), total = 0)
            }
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
            val response = chain.proceed(builder.build())
            if (response.code == 403 && esCuentaSuspendida(response)) {
                CuentaSuspendidaEventBus.avisar()
            }
            // Sin token el 401 es esperable (todavía no hay sesión): no hay nada que renovar
            if (response.code == 401 && token != null) {
                SesionExpiradaEventBus.avisar()
            }
            response
        }
        .build()

    // peekBody no consume el cuerpo: el código que hizo el request lo sigue pudiendo leer.
    private fun esCuentaSuspendida(response: okhttp3.Response): Boolean =
        runCatching { response.peekBody(2048).string() }
            .getOrDefault("")
            .contains(CuentaSuspendidaEventBus.DETALLE)

    internal val retrofit: Retrofit = Retrofit.Builder()
        .addConverterFactory(GsonConverterFactory.create(gson))
        .baseUrl(com.example.plag_out.BuildConfig.BACKEND_URL)
        .client(client)
        .build()

    /** Pin each retry to one immutable session; never send A's queued payload as B. */
    fun forPresenceRetry(owner: String): GDDService {
        val retryClient = OkHttpClient.Builder().addInterceptor(presenceSessionInterceptor(owner) {
            val session = SupabaseProvider.client.auth.currentSessionOrNull()
            session?.user?.id?.let { it to session.accessToken }
        }).build()
        return retrofit.newBuilder().client(retryClient).build().create(GDDService::class.java)
    }

    val gddService: GDDService = retrofit.create(GDDService::class.java)
}

internal fun presenceSessionInterceptor(owner: String, session: () -> Pair<String, String>?) =
    okhttp3.Interceptor { chain ->
        val current = session()
        if (current == null || current.first != owner) throw java.io.IOException("presence_session_changed")
        chain.proceed(chain.request().newBuilder()
            .header("Authorization", "Bearer ${current.second}").build())
    }
