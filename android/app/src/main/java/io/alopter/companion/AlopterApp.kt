package io.alopter.companion

import android.app.Application
import io.alopter.companion.ai.BackendClient
import io.alopter.companion.auth.TokenVault

class AlopterApp : Application() {
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
