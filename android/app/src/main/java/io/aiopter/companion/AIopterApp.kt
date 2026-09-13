package io.aiopter.companion

import android.app.Application
import io.aiopter.companion.ai.BackendClient
import io.aiopter.companion.auth.TokenVault

class AIopterApp : Application() {
    lateinit var tokenVault: TokenVault
        private set
    lateinit var backendClient: BackendClient
        private set

    override fun onCreate() {
        super.onCreate()
        tokenVault = TokenVault(this)
        backendClient = BackendClient(BuildConfig.API_BASE_URL, tokenVault)
    }
}
