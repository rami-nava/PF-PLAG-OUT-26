package com.example.plag_out

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

import io.github.jan.supabase.auth.FlowType

object SupabaseProvider {
    val client: SupabaseClient by lazy {
        createSupabaseClient(
            // Vienen del build (app/build.gradle.kts): cambiar de proyecto no toca el código.
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_PUBLISHABLE_KEY
        ) {
            install(Auth) {
                scheme = "plagout"
                host = "reset-password"
                flowType = FlowType.IMPLICIT
            }
            install(Postgrest)
        }
    }
}
