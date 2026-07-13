package com.example.plag_out

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

object SupabaseProvider {
    private const val SUPABASE_URL = "https://djhrmrvcffolxzvcqlux.supabase.co"
    private const val SUPABASE_ANON_KEY = "sb_publishable_tB5w0haD6JUc2O4ORfWlqA_UWdh5yU9"

    val client: SupabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = SUPABASE_URL,
            supabaseKey = SUPABASE_ANON_KEY
        ) {
            install(Auth)
            install(Postgrest)
        }
    }
}
