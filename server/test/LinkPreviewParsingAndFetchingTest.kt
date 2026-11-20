package com.usuallycoding.opensecretsanta.server

import com.usuallycoding.opensecretsanta.server.data.preview.HtmlMetadataParser
import com.usuallycoding.opensecretsanta.server.data.preview.HttpTransport
import com.usuallycoding.opensecretsanta.server.data.preview.RawHttpResponse
import com.usuallycoding.opensecretsanta.server.data.preview.SafeAddressValidator
import com.usuallycoding.opensecretsanta.server.data.preview.SafeHttpFetcher
import com.usuallycoding.opensecretsanta.server.data.preview.ThumbnailResizer
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.InetAddress
import java.net.URI
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LinkPreviewParsingAndFetchingTest {

    @Test
    fun safeAddressValidatorRejectsPrivateLoopbackLinkLocalAndAllowsPublic() {
        val blocked = listOf(
            "127.0.0.1",
            "10.0.0.5",
            "172.16.0.1",
            "192.168.1.1",
            "169.254.169.254",
            "100.64.0.1",
            "0.0.0.0",
            "224.0.0.1",
            "::1",
            "fc00::1",
            "fe80::1",
        )
        for (ip in blocked) {
            assertFalse(SafeAddressValidator.isSafePublicAddress(InetAddress.getByName(ip)), "Expected $ip to be blocked")
        }
        assertTrue(SafeAddressValidator.isSafePublicAddress(InetAddress.getByName("93.184.216.34")))
        assertTrue(SafeAddressValidator.isSafePublicAddress(InetAddress.getByName("2606:2800:220:1:248:1893:25c8:1946")))
    }

    @Test
    fun fetcherBlocksPrivateAddressesRedirectsLoopsAndOversizedImages() {
        val requested = mutableListOf<URI>()
        val transport = HttpTransport { uri, _, _, _ ->
            requested += uri
            when (uri.path) {
                "/public-redirect" -> RawHttpResponse(302, "http://169.254.169.254/latest/meta-data/", null, null, ByteArrayInputStream(ByteArray(0)))
                "/ok-redirect" -> RawHttpResponse(301, "/ok", null, null, ByteArrayInputStream(ByteArray(0)))
                "/loop" -> RawHttpResponse(302, "/loop", null, null, ByteArrayInputStream(ByteArray(0)))
                "/big-image" -> {
                    val payload = ByteArray(2048) { 1 }
                    RawHttpResponse(200, null, "image/jpeg", payload.size.toLong(), ByteArrayInputStream(payload))
                }
                "/ok" -> {
                    val html = "<html><head><title>Cozy Socks</title></head><body>hi</body></html>".toByteArray()
                    RawHttpResponse(200, null, "text/html; charset=utf-8", html.size.toLong(), ByteArrayInputStream(html))
                }
                else -> RawHttpResponse(404, null, null, null, ByteArrayInputStream(ByteArray(0)))
            }
        }
        val dns: (String) -> List<InetAddress> = { host ->
            when (host) {
                "shop.example" -> listOf(InetAddress.getByName("93.184.216.34"))
                else -> listOf(InetAddress.getByName(host))
            }
        }
        val fetcher = SafeHttpFetcher(transport = transport, resolveHost = dns, maxRedirects = 3)

        // Private IP is blocked before any HTTP request is made.
        assertNull(fetcher.fetchHtml("http://127.0.0.1/ok"))
        assertEquals(emptyList(), requested)

        // Redirect to link-local metadata IP is aborted without connecting to 169.254.169.254.
        assertNull(fetcher.fetchHtml("https://shop.example/public-redirect"))
        assertEquals(listOf(URI("https://shop.example/public-redirect")), requested)

        // Infinite redirect loop stops at maxRedirects (1 initial + 3 redirects = 4 requests).
        requested.clear()
        assertNull(fetcher.fetchHtml("https://shop.example/loop"))
        assertEquals(4, requested.size)

        // Oversized image is rejected.
        assertNull(fetcher.fetchImage("https://shop.example/big-image", maxBytes = 512))

        // Safe public redirect succeeds and reports final URI.
        val ok = fetcher.fetchHtml("https://shop.example/ok-redirect")
        assertNotNull(ok)
        assertEquals(URI("https://shop.example/ok"), ok.finalUri)
        assertTrue(ok.bytes.decodeToString().contains("Cozy Socks"))
    }

    @Test
    fun htmlMetadataParserExtractsOgTwitterTitleAndResolvesRelativeImage() {
        val ogHtml = """
            <html><head>
              <meta property="og:title" content="  Wool &amp; Cotton Socks  " />
              <meta content="Cozy Shop" property="og:site_name" />
              <meta property="og:image" content="/img/socks.jpg" />
              <title>Fallback Title</title>
            </head></html>
        """.trimIndent().toByteArray()

        val og = HtmlMetadataParser.parse(ogHtml, "text/html; charset=utf-8", URI("https://www.cozyshop.example/p/1"))
        assertNotNull(og)
        assertEquals("Wool & Cotton Socks", og.title)
        assertEquals("Cozy Shop", og.siteName)
        assertEquals("https://www.cozyshop.example/img/socks.jpg", og.imageUrl)

        val plainHtml = "<html><head><title>  Plain &#39;Title&#39; \n Here </title></head></html>".toByteArray()
        val plain = HtmlMetadataParser.parse(plainHtml, "text/html", URI("https://www.shop.example/item"))
        assertNotNull(plain)
        assertEquals("Plain 'Title' Here", plain.title)
        assertEquals("shop.example", plain.siteName)
        assertNull(plain.imageUrl)

        val twitterHtml = """
            <html><head>
              <meta name='twitter:title' content='Twitter Gift'>
              <meta name='twitter:image' content='https://cdn.shop.example/gift.png'>
            </head></html>
        """.trimIndent().toByteArray()
        val twitter = HtmlMetadataParser.parse(twitterHtml, "text/html", URI("https://shop.example/item"))
        assertNotNull(twitter)
        assertEquals("Twitter Gift", twitter.title)
        assertEquals("https://cdn.shop.example/gift.png", twitter.imageUrl)

        // Non-HTML content type is ignored.
        assertNull(HtmlMetadataParser.parse(plainHtml, "application/pdf", URI("https://shop.example/doc.pdf")))
    }

    @Test
    fun thumbnailResizerScalesDownPreservingAspectRatioAndRejectsUnsupportedTypes() {
        val wide = BufferedImage(800, 400, BufferedImage.TYPE_INT_RGB).apply {
            createGraphics().apply {
                color = Color.RED
                fillRect(0, 0, 800, 400)
                dispose()
            }
        }
        val wideBytes = ByteArrayOutputStream().also { ImageIO.write(wide, "png", it) }.toByteArray()
        val resized = ThumbnailResizer.resize(wideBytes, "image/png")
        assertNotNull(resized)
        assertEquals("image/jpeg", resized.contentType)
        val decoded = ImageIO.read(ByteArrayInputStream(resized.bytes))
        assertEquals(256, decoded.width)
        assertEquals(128, decoded.height)

        val alpha = BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB)
        val alphaBytes = ByteArrayOutputStream().also { ImageIO.write(alpha, "png", it) }.toByteArray()
        val resizedAlpha = ThumbnailResizer.resize(alphaBytes, "image/png; charset=binary")
        assertNotNull(resizedAlpha)
        assertEquals("image/png", resizedAlpha.contentType)

        assertNull(ThumbnailResizer.resize(wideBytes, "image/svg+xml"))
        assertNull(ThumbnailResizer.resize("not an image".toByteArray(), "image/jpeg"))
    }
}
