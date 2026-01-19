package com.mamun.smsgateway.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Collections

object NetworkUtils {

    fun getAllIpAddresses(): List<String> {
        val ipAddresses = mutableListOf<String>()

        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (networkInterface in interfaces) {
                if (networkInterface.isLoopback || !networkInterface.isUp) continue

                val addresses = Collections.list(networkInterface.inetAddresses)
                for (address in addresses) {
                    if (address is Inet4Address && !address.isLoopbackAddress) {
                        ipAddresses.add(address.hostAddress ?: continue)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (ipAddresses.isEmpty()) {
            ipAddresses.add("127.0.0.1")
        }

        return ipAddresses
    }

    fun getWifiIpAddress(context: Context): String? {
        val wifiManager = context.applicationContext
            .getSystemService(Context.WIFI_SERVICE) as WifiManager
        val wifiInfo = wifiManager.connectionInfo
        val ipInt = wifiInfo.ipAddress

        if (ipInt == 0) return null

        return String.format(
            "%d.%d.%d.%d",
            ipInt and 0xff,
            ipInt shr 8 and 0xff,
            ipInt shr 16 and 0xff,
            ipInt shr 24 and 0xff
        )
    }

    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE)
                as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun formatIpAddressesDisplay(wsPort: Int, httpPort: Int): String {
        val ips = getAllIpAddresses()
        val sb = StringBuilder()

        ips.forEachIndexed { index, ip ->
            sb.append("• $ip\n")
            sb.append("  WebSocket: ws://$ip:$wsPort\n")
            sb.append("  HTTP: http://$ip:$httpPort")
            if (index < ips.size - 1) sb.append("\n\n")
        }

        return sb.toString()
    }
}