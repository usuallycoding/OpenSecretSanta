package com.usuallycoding.opensecretsanta.data

import kotlin.test.Test
import kotlin.test.assertEquals

class LocalNetworkTest {
    private fun assertLocal(vararg urls: String) = urls.forEach { assertEquals(true, isLocalNetworkAddress(it), it) }

    private fun assertPublic(vararg urls: String) = urls.forEach { assertEquals(false, isLocalNetworkAddress(it), it) }

    @Test
    fun thisMachineIsLocal() = assertLocal(
        "http://localhost:8080", "http://LOCALHOST", "http://127.0.0.1:8080", "http://127.1.2.3",
        "http://[::1]:8080", "http://10.0.2.2:8080",
    )

    @Test
    fun privateIpv4RangesAreLocal() = assertLocal(
        "http://10.0.0.1", "http://10.255.255.255", "http://172.16.0.1", "http://172.31.255.255",
        "http://192.168.1.20:8080", "http://169.254.10.10",
    )

    @Test
    fun justOutsidePrivateRangesIsPublic() = assertPublic(
        "http://172.15.0.1", "http://172.32.0.1", "http://192.169.0.1", "http://11.0.0.1", "http://8.8.8.8",
        "http://169.253.0.1",
    )

    @Test
    fun localNamesAreLocal() = assertLocal("http://santa.local:8080", "http://SANTA.LOCAL", "http://homeserver", "http://nas:8080/path")

    @Test
    fun publicNamesAreNotLocal() = assertPublic(
        "https://santa.example.org", "https://santa.example.org:8443/api", "http://example.com", "https://localhost.example.org",
        "https://my.local.example.org",
    )

    @Test
    fun ipv6PrivateRangesAreLocalAndPublicOnesAreNot() {
        assertLocal("http://[fe80::1]:8080", "http://[fd12:3456::1]", "http://[fc00::1]")
        assertPublic("http://[2001:4860:4860::8888]", "https://[2606:4700::1111]:443")
    }

    @Test
    fun portPathQueryAndUserInfoAreIgnored() {
        assertLocal("http://192.168.1.20:8080/api?x=1#top", "http://user:pass@192.168.1.20/", "https://nas?x=y")
        assertPublic("https://santa.example.org:8080/192.168.1.20", "https://user@santa.example.org/", "https://santa.example.org?host=localhost")
    }

    @Test
    fun malformedAddressesAreNotLocal() = assertPublic("", "not a url", "192.168.1.20", "//192.168.1.20", "http://", "http://:8080", "://x")

    @Test
    fun aDottedNameThatLooksLikeAnAddressButIsNotOneIsJudgedAsAName() = assertPublic("http://10.0.0.256", "http://10.0.0", "http://1.2.3.4.5")
}
