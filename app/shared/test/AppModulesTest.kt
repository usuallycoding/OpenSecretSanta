package com.usuallycoding.opensecretsanta

import com.usuallycoding.opensecretsanta.data.BackendConfig
import com.usuallycoding.opensecretsanta.di.initKoin
import org.koin.core.context.stopKoin
import org.koin.mp.KoinPlatform
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame

class AppModulesTest {

    @AfterTest
    fun tearDown() = stopKoin()

    @Test
    fun backendUrlSuppliedByHostIsResolvable() {
        initKoin(baseUrl = "http://example.test:8080")

        val config = KoinPlatform.getKoin().get<BackendConfig>()

        assertEquals("http://example.test:8080", config.baseUrl)
    }

    @Test
    fun secondInitIsIgnoredAndDoesNotFail() {
        assertNull(KoinPlatform.getKoinOrNull())
        initKoin(baseUrl = "http://first.test")
        val firstKoin = KoinPlatform.getKoin()

        initKoin(baseUrl = "http://second.test")

        assertNotNull(KoinPlatform.getKoinOrNull())
        assertSame(firstKoin, KoinPlatform.getKoin())
        assertEquals("http://first.test", firstKoin.get<BackendConfig>().baseUrl)
    }
}
