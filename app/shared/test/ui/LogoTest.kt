package com.usuallycoding.opensecretsanta.ui

import androidx.compose.ui.graphics.Color
import com.usuallycoding.opensecretsanta.ui.login.logoIsDark
import com.usuallycoding.opensecretsanta.ui.theme.backgroundDark
import com.usuallycoding.opensecretsanta.ui.theme.backgroundLight
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LogoTest {
    @Test
    fun theAppsDarkBackgroundGetsTheDarkLogoAndItsLightOneTheLightLogo() {
        assertTrue(logoIsDark(backgroundDark))
        assertFalse(logoIsDark(backgroundLight))
    }

    @Test
    fun blackAndWhiteAreTheExtremes() {
        assertTrue(logoIsDark(Color.Black))
        assertFalse(logoIsDark(Color.White))
    }

    @Test
    fun dynamicColoursAreSortedByHowLightTheyAre() {
        // tinted backgrounds from a wallpaper: a pale one and a deep one
        assertFalse(logoIsDark(Color(0xFFFFF0F5)))
        assertFalse(logoIsDark(Color(0xFFDDE8FF)))
        assertTrue(logoIsDark(Color(0xFF14202B)))
        assertTrue(logoIsDark(Color(0xFF3B1F2B)))
    }

    @Test
    fun aMidToneGoesToTheSideItIsNearer() {
        assertFalse(logoIsDark(Color(0xFFC8C8C8)))
        assertTrue(logoIsDark(Color(0xFF606060)))
    }
}
