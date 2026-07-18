package hr.kotwave.gameslibrary.mirror.host

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LanAddressesTest {

    @Test
    fun classifiesWindowsStyleAdapters() {
        assertEquals(LanAddressKind.WIFI, classifyAdapter("wlan3", "Intel(R) Wi-Fi 6E AX211 160MHz"))
        assertEquals(LanAddressKind.ETHERNET, classifyAdapter("eth5", "Realtek PCIe GbE Family Controller"))
        assertEquals(LanAddressKind.VIRTUAL, classifyAdapter("eth2", "VirtualBox Host-Only Ethernet Adapter"))
        assertEquals(LanAddressKind.VIRTUAL, classifyAdapter("eth7", "vEthernet (WSL (Hyper-V firewall))"))
        assertEquals(LanAddressKind.VIRTUAL, classifyAdapter("tun0", "WireGuard Tunnel"))
        assertEquals(LanAddressKind.VIRTUAL, classifyAdapter("wlan9", "Tailscale Tunnel"))
        assertEquals(LanAddressKind.OTHER, classifyAdapter("net1", "Some USB Adapter"))
    }

    @Test
    fun privateRangesAreRfc1918Only() {
        assertTrue(isPrivateLanIp("192.168.1.24"))
        assertTrue(isPrivateLanIp("10.0.0.7"))
        assertTrue(isPrivateLanIp("172.16.0.1"))
        assertTrue(isPrivateLanIp("172.31.255.254"))
        assertFalse(isPrivateLanIp("172.32.0.1"))
        assertFalse(isPrivateLanIp("100.101.102.103"))
        assertFalse(isPrivateLanIp("8.8.8.8"))
        assertFalse(isPrivateLanIp("not-an-ip"))
    }

    @Test
    fun ordersVirtualLastThenPrivateFirstThenByKind() {
        // A VPN adapter's private 10.x address must never beat a real adapter, even a public one.
        val virtual = LanAddress("10.8.0.2", LanAddressKind.VIRTUAL)
        val publicWifi = LanAddress("100.101.102.103", LanAddressKind.WIFI)
        val ethernet = LanAddress("10.0.0.7", LanAddressKind.ETHERNET)
        val wifi = LanAddress("192.168.1.24", LanAddressKind.WIFI)
        val other = LanAddress("192.168.7.3", LanAddressKind.OTHER)

        assertEquals(
            listOf(wifi, ethernet, other, publicWifi, virtual),
            orderLanAddresses(listOf(virtual, publicWifi, ethernet, wifi, other)),
        )
    }

    @Test
    fun enumerationYieldsDistinctIpv4Addresses() {
        val addresses = enumerateLanAddresses()

        assertEquals(addresses.map { it.ip }.distinct(), addresses.map { it.ip })
        addresses.forEach { assertTrue(Regex("""\d{1,3}(\.\d{1,3}){3}""").matches(it.ip), it.ip) }
    }
}
