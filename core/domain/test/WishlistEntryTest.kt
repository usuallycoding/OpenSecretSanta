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

    private fun entry(title: String? = "Wool socks", note: String? = null, vararg urls: String) = WishlistEntry.of(title, note, urls.toList())

    private fun linkEntry(url: String?) = WishlistEntry.ofFirstLink("Wool socks", null, url)

    // --- title, note, link basics ---

    @Test
    fun aTitleAloneIsEnough() {
        val e = entry()
        assertEquals("Wool socks", e.title)
        assertNull(e.note)
        assertEquals(emptyList(), e.urls)
    }

    @Test
    fun whitespaceIsTrimmedAndBlankOptionalsBecomeEmpty() {
        val e = entry("  Wool socks  ", "   ", "\n")
        assertEquals("Wool socks", e.title)
        assertNull(e.note)
        assertEquals(emptyList(), e.urls)
    }

    @Test
    fun noteAndLinkAreKept() {
        val e = entry(note = "Grey, size M", urls = arrayOf("https://shop.example/socks"))
        assertEquals("Grey, size M", e.note)
        assertEquals(listOf("https://shop.example/socks"), e.urls)
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
        assertEquals(listOf(long), entry(urls = arrayOf(long)).urls)
        assertEquals("urls[0]", fieldOf { entry(urls = arrayOf(long + "a")) })
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
        for (link in valid) assertEquals(listOf(link), entry(urls = arrayOf(link)).urls, link)
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
        for (link in invalid) {
            assertEquals("urls[0]", fieldOf { entry(urls = arrayOf(link)) }, link)
            assertEquals("url", fieldOf { linkEntry(link) }, link)
        }
    }

    @Test
    fun surroundingWhitespaceAroundAValidLinkIsTrimmedNotRejected() {
        assertEquals(listOf("https://shop.example/"), entry(urls = arrayOf("  https://shop.example/\n")).urls)
    }

    @Test
    fun aLabelOverSixtyThreeCharactersIsRejected() {
        assertEquals("urls[0]", fieldOf { entry(urls = arrayOf("https://${"a".repeat(64)}.example/")) })
        entry(urls = arrayOf("https://${"a".repeat(63)}.example/"))
    }

    @Test
    fun invalidLinkDoesNotHideAValidTitleError() {
        assertEquals("title", fieldOf { entry(title = "", urls = arrayOf("javascript:alert(1)")) })
    }

    // --- several links ---

    private fun links(count: Int) = Array(count) { "https://shop.example/$it" }

    @Test
    fun tenLinksAreKeptInTheOrderGiven() {
        assertEquals(links(10).toList(), entry(urls = links(10)).urls)
    }

    @Test
    fun elevenLinksAreRejectedNamingUrls() {
        assertEquals("urls", fieldOf { entry(urls = links(11)) })
    }

    @Test
    fun tooManyLinksIsReportedBeforeABadOne() {
        assertEquals("urls", fieldOf { entry(urls = links(10) + "javascript:alert(1)") })
    }

    @Test
    fun blanksAreDroppedAndRepeatsKeptOnceAtTheirFirstPlace() {
        val e = entry(urls = arrayOf("https://a.example", "   ", "https://b.example", " https://a.example "))
        assertEquals(listOf("https://a.example", "https://b.example"), e.urls)
    }

    @Test
    fun repeatsAndBlanksDoNotCountTowardsTheLimit() {
        assertEquals(10, entry(urls = links(10) + links(10) + "  ").urls.size)
    }

    @Test
    fun aBadLinkIsNamedByItsPlaceAmongTheKeptLinks() {
        assertEquals("urls[1]", fieldOf { entry(urls = arrayOf("https://a.example", "ftp://host/file")) })
        assertEquals("urls[1]", fieldOf { entry(urls = arrayOf("", "https://a.example", "ftp://host/file")) })
    }

    // --- an app that knows one link ---

    @Test
    fun anOlderAppsLinkStandsForTheFirstLinkOnly() {
        val e = linkEntry(" https://new.example ")
        assertEquals(listOf("https://new.example"), e.urls)
        assertEquals(listOf("https://new.example", "https://b.example", "https://c.example"), e.linksOver(listOf("https://old.example", "https://b.example", "https://c.example")))
    }

    @Test
    fun anOlderAppWithNoLinkRemovesTheFirstLinkAndKeepsTheRest() {
        assertEquals(listOf("https://b.example"), linkEntry(null).linksOver(listOf("https://old.example", "https://b.example")))
        assertEquals(listOf("https://b.example"), linkEntry("  ").linksOver(listOf("https://old.example", "https://b.example")))
    }

    @Test
    fun anOlderAppsLinkAlreadyAmongTheOthersIsNotRepeated() {
        assertEquals(listOf("https://b.example"), linkEntry("https://b.example").linksOver(listOf("https://old.example", "https://b.example")))
    }

    @Test
    fun linksOfAnEntryWithUrlsReplaceEverything() {
        assertEquals(listOf("https://x.example"), entry(urls = arrayOf("https://x.example")).linksOver(listOf("https://a.example", "https://b.example")))
        assertEquals(emptyList(), entry().linksOver(listOf("https://a.example")))
    }
}
