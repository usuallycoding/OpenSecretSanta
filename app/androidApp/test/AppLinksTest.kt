package com.usuallycoding.opensecretsanta

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AppLinksTest {
    @Test
    fun anHttpsAddressOfTheWebAppGivesItsFragment() {
        assertEquals("/groups/7", linkFragment("https", WEB_APP_HOST, "/groups/7"))
    }

    @Test
    fun anotherSchemeOrHostGivesNothing() {
        assertNull(linkFragment("http", WEB_APP_HOST, "/groups/7"))
        assertNull(linkFragment("https", "evil.example", "/groups/7"))
        assertNull(linkFragment("https", "santa.example.com.evil.example", "/groups/7"))
        assertNull(linkFragment(null, null, "/groups/7"))
    }

    @Test
    fun anAddressWithoutAFragmentGivesNothing() {
        assertNull(linkFragment("https", WEB_APP_HOST, null))
    }
}
