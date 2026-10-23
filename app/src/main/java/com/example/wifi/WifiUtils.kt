package com.example.wifi

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiNetworkSpecifier
import android.os.Build
import android.provider.Settings
import com.example.model.WifiData

object WifiUtils {

    fun openWifiSettings(context: Context) {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                Intent(Settings.Panel.ACTION_WIFI).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            } catch (e: Exception) {
                Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            }
        } else {
            Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        }
        context.startActivity(intent)
    }

    /**
     * Attempts a connection request via WifiNetworkSpecifier on Android 10+ (API 29+).
     * Android displays a system dialog prompting the user to confirm joining the network.
     */
    fun requestWifiConnection(
        context: Context,
        wifiData: WifiData,
        onPromptShown: () -> Unit,
        onFailed: (String) -> Unit
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val specifierBuilder = WifiNetworkSpecifier.Builder()
                    .setSsid(wifiData.ssid)

                if (wifiData.isHidden) {
                    specifierBuilder.setIsHiddenSsid(true)
                }

                when (wifiData.securityType.uppercase()) {
                    "WPA", "WPA2", "WPA3", "WPA/WPA2" -> {
                        if (wifiData.password.isNotEmpty()) {
                            specifierBuilder.setWpa2Passphrase(wifiData.password)
                        }
                    }
                    // For open networks, no password is set
                }

                val specifier = specifierBuilder.build()
                val networkRequest = NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .setNetworkSpecifier(specifier)
                    .build()

                val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                connectivityManager.requestNetwork(networkRequest, object : ConnectivityManager.NetworkCallback() {
                    // System UI takes over from here
                })
                onPromptShown()
            } catch (e: Exception) {
                onFailed(e.localizedMessage ?: "Failed to initiate Wi-Fi connection request")
            }
        } else {
            // Pre-Android 10
            openWifiSettings(context)
            onPromptShown()
        }
    }
}
