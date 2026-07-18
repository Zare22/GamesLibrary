package hr.kotwave.gameslibrary.mirror.host

import java.net.Inet4Address
import java.net.NetworkInterface

/** Declaration order is the default-pick preference order. */
enum class LanAddressKind { WIFI, ETHERNET, OTHER, VIRTUAL }

/** One usable IPv4 address the hosting screen can offer; [kind] picks the chip label. */
data class LanAddress(val ip: String, val kind: LanAddressKind)

/**
 * The machine's usable IPv4 LAN addresses, best default first: private-range before public,
 * Wi-Fi before Ethernet before unknown adapters, VPN/virtual adapters always last.
 */
fun enumerateLanAddresses(): List<LanAddress> = orderLanAddresses(
    NetworkInterface.getNetworkInterfaces().asSequence()
        .filter { runCatching { it.isUp && !it.isLoopback }.getOrDefault(false) }
        .flatMap { nic ->
            val kind = classifyAdapter(nic.name, nic.displayName ?: "")
            nic.inetAddresses.asSequence()
                .filterIsInstance<Inet4Address>()
                .filter { !it.isLoopbackAddress && !it.isLinkLocalAddress }
                .mapNotNull { address -> address.hostAddress?.let { LanAddress(it, kind) } }
        }
        .distinctBy { it.ip }
        .toList(),
)

internal fun orderLanAddresses(addresses: List<LanAddress>): List<LanAddress> =
    addresses.sortedWith(
        compareBy<LanAddress> { it.kind == LanAddressKind.VIRTUAL }
            .thenByDescending { isPrivateLanIp(it.ip) }
            .thenBy { it.kind.ordinal },
    )

/** RFC 1918 private ranges; CGNAT (100.64/10, e.g. Tailscale) deliberately does not count. */
internal fun isPrivateLanIp(ip: String): Boolean {
    val octets = ip.split(".").mapNotNull { it.toIntOrNull() }
    if (octets.size != 4) return false
    return when {
        octets[0] == 10 -> true
        octets[0] == 172 && octets[1] in 16..31 -> true
        octets[0] == 192 && octets[1] == 168 -> true
        else -> false
    }
}

private val VIRTUAL_MARKERS = listOf(
    "vpn", "virtual", "vmware", "vbox", "hyper-v", "vethernet",
    "wireguard", "tailscale", "zerotier", "docker", "wsl", "tunnel", "tap-windows",
)

private val WIFI_MARKERS = listOf("wlan", "wi-fi", "wifi", "wireless", "802.11")

internal fun classifyAdapter(name: String, displayName: String): LanAddressKind {
    val haystack = "$name $displayName".lowercase()
    return when {
        VIRTUAL_MARKERS.any { it in haystack } -> LanAddressKind.VIRTUAL
        name.startsWith("tun") || name.startsWith("tap") -> LanAddressKind.VIRTUAL
        WIFI_MARKERS.any { it in haystack } -> LanAddressKind.WIFI
        name.startsWith("eth") || "ethernet" in haystack -> LanAddressKind.ETHERNET
        else -> LanAddressKind.OTHER
    }
}
