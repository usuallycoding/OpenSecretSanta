package com.usuallycoding.opensecretsanta

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.usuallycoding.opensecretsanta.data.BackendConfig
import com.usuallycoding.opensecretsanta.ui.App
import org.koin.mp.KoinPlatform

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val baseUrl = KoinPlatform.getKoin().get<BackendConfig>().baseUrl
        setContent {
            App(gate = { content -> LocalNetworkGate(baseUrl, content) })
        }
    }
}
