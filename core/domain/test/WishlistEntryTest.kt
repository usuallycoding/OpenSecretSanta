package com.usuallycoding.opensecretsanta.domain

import com.usuallycoding.opensecretsanta.domain.error.DomainException
import com.usuallycoding.opensecretsanta.domain.wishlist.WishlistEntry
import com.usuallycoding.opensecretsanta.domain.wishlist.WishlistLimits
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class WishlistEntryTest {

    private fun fieldOf(block: () -> Unit) = assertFailsWith<DomainException.ValidationFailed> { block() }.field

    private fun entry(title: String? = "Wool socks", note: String? = null, url: String? = null) = WishlistEntry.of(title, note, url)

    // --- title, note, link basics ---

    @Test
    fun aTitleAloneIsEnough() {
        val e = entry()
        assertEquals("Wool socks", e.title)
        assertNull(e.note)
        assertNull(e.url)
    }

    @Test
    fun whitespaceIsTrimmedAndBlankOptionalsBecomeEmpty() {
        val e = entry(title = "  Wool socks  ", note = "   ", url = "\n")
        assertEquals("Wool socks", e.title)
        assertNull(e.note)
        assertNull(e.url)
    }

    @Test
    fun noteAndLinkAreKept() {
        val e = entry(note = "Grey, size M", url = "https://shop.example/socks")
        assertEquals("Grey, size M", e.note)
        assertEquals("https://shop.example/socks", e.url)
    }

    @Test
    fun blankMissingOrTooLongTitlesAreRejectedNamingTheField() {
        for (bad in listOf("", "   ", null, "t".repeat(WishlistLimits.MAX_TITLE_LENGTH + 1))) {
            assertEquals("title", fieldOf { entry(title = bad) }, bad?.take(10))
        }
        assertEquals(200, entry(title = "t".repeat(200)).title.length)
    }

    @Test
    fun noteLengthIsLimitedTo1000() {
        assertEquals(1000, entry(note = "n".repeat(1000)).note!!.length)
        assertEquals("note", fieldOf { entry(note = "n".repeat(1001)) })
    }

    @Test
    fun linkLengthIsLimitedTo2000() {
        val long = "https://shop.example/" + "a".repeat(2000 - "https://shop.example/".length)
        assertEquals(2000, long.length)
        assertEquals(long, entry(url = long).url)
        assertEquals("url", fieldOf { entry(url = long + "a") })
    }

    // --- links ---

    @Test
    fun webAddressesAreAccepted() {
        val valid = listOf(
            "https://shop.example/item?id=7",
            "HTTP://shop.example",
            "http://shop.example",
            "https://sub.shop-example.co.uk/path/to/item#section",
            "https://shop.example:8443/item",
            "http://localhost",
            "http://localhost:3000/x",
            "http://192.168.1.10/item",
            "https://xn--bcher-kva.example/",
            "https://shop.example?query=only",
            "https://a.b",
        )
        for (link in valid) assertEquals(link, entry(url = link).url, link)
    }

    @Test
    fun otherSchemesAndMalformedLinksAreRejectedNamingTheField() {
        val invalid = listOf(
            "javascript:alert(1)",
            "JavaScript:alert(1)",
            "data:text/html,x",
            "file:///etc/passwd",
            "ftp://host/file",
            "mailto:someone@example.com",
            "//host/path",
            "/relative/path",
            "shop.example/item",
            "not a url",
            "https://",
            "https:///path",
            "http://:80/",
            "https://shop.example:abc/",
            "https://shop.example:99999/",
            "https://shop.example:/",
            "https://user:pass@shop.example/",
            "https://user@shop.example/",
            "https://shop example/",
            "https://shop.example/a b",
            "https://-bad.example/",
            "https://bad-.example/",
            "https://a..b/",
            "https://.example/",
            "https://shop.example./",
            "https://[::1]/",
            "https://bücher.example/",
            "https://shop.example/\u0000",
            "https://shop.example/a\nb",
            "http:/shop.example",
        )
        for (link in invalid) assertEquals("url", fieldOf { entry(url = link) }, link)
    }

    @Test
    fun surroundingWhitespaceAroundAValidLinkIsTrimmedNotRejected() {
        assertEquals("https://shop.example/", entry(url = "  https://shop.example/\n").url)
    }

    @Test
    fun aLabelOverSixtyThreeCharactersIsRejected() {
        assertEquals("url", fieldOf { entry(url = "https://${"a".repeat(64)}.example/") })
        entry(url = "https://${"a".repeat(63)}.example/")
    }

    @Test
    fun invalidLinkDoesNotHideAValidTitleError() {
        assertEquals("title", fieldOf { entry(title = "", url = "javascript:alert(1)") })
    }
}
