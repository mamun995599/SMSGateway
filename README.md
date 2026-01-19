# SMS Gateway by using kotline

> **Date:** 1/10/2026, 10:44:54 PM  
> **Chat ID:** 019ba8cb-c093-7651-b81c-e34a6735c922  
> **Source:** [LMArena](https://lmarena.ai/c/019ba8cb-c093-7651-b81c-e34a6735c922)  

### [2] ASSISTANT (claude-opus-4-5-20251101-thinking-32k)

<details>
<summary><strong>Thinking</strong></summary>

> The user wants me to create an Android app called "SMS Gateway" with the following features:
> 
> 1. GUI with toggle on/off switch
> 2. Two port fields: one for WebSocket server, one for HTTP server
> 3. Default ports: WebSocket = 8080, HTTP = 8070
> 4. Status section showing:
>    - Connected WebSocket devices with IPs
>    - HTTP request console logs
> 5. Permissions: SMS, notification, network access
> 6. Background service that survives app closure
> 7. Service remains alive even in idle state (need wake lock/foreground service)
> 8. Accessible via local network on all addresses
> 
> Let me create a comprehensive Android app structure:
> 
> 1. **Project Structure:**
>    - MainActivity.kt (UI)
>    - SmsGatewayService.kt (Foreground Service)
>    - WebSocketServer.kt
>    - HttpServer.kt
>    - SmsManager.kt
>    - Various adapters and utilities
> 
> 2. **Dependencies:**
>    - Ktor or NanoHTTPD for HTTP server
>    - Java-WebSocket for WebSocket server
> 
> 3. **Key Components:**
>    - Foreground notification to keep service alive
>    - WakeLock to prevent CPU sleep
>    - START_STICKY for service restart
> 
> Let me create the full implementation:
> 
> ```kotlin
> // build.gradle.kts (app level)
> // AndroidManifest.xml
> // MainActivity.kt
> // SmsGatewayService.kt
> // WebSocketServer.kt
> // HttpServer.kt
> // layouts
> // etc.
> ```
> 
> I'll provide a complete, working implementation.

</details>

# SMS Gateway Android App

I'll create a complete Android app with all the requested features. This is a comprehensive implementation.

## Project Structure

```
app/
├── src/main/
│   ├── java/com/smsgateway/
│   │   ├── MainActivity.kt
│   │   ├── SmsGatewayService.kt
│   │   ├── server/
│   │   │   ├── WebSocketServer.kt
│   │   │   └── HttpServer.kt
│   │   ├── adapter/
│   │   │   ├── ConnectedDevicesAdapter.kt
│   │   │   └── ConsoleLogAdapter.kt
│   │   ├── model/
│   │   │   ├── ConnectedDevice.kt
│   │   │   └── LogEntry.kt
│   │   └── util/
│   │       ├── NetworkUtils.kt
│   │       └── PreferenceManager.kt
│   ├── res/
│   │   ├── layout/
│   │   │   ├── activity_main.xml
│   │   │   ├── item_connected_device.xml
│   │   │   └── item_log_entry.xml
│   │   ├── drawable/
│   │   │   └── ...
│   │   └── values/
│   │       ├── strings.xml
│   │       ├── colors.xml
│   │       └── themes.xml
│   └── AndroidManifest.xml
├── build.gradle.kts
└── ...
```

## 1. build.gradle.kts (Project Level)

```kotlin
// Top-level build file
plugins {
    id("com.android.application") version "8.1.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.0" apply false
}
```

## 2. build.gradle.kts (App Level)

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.smsgateway"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.smsgateway"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
    kotlinOptions {
        jvmTarget = "17"
    }
    
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-service:2.7.0")
    
    // WebSocket Server
    implementation("org.java-websocket:Java-WebSocket:1.5.4")
    
    // HTTP Server (NanoHTTPD)
    implementation("org.nanohttpd:nanohttpd:2.3.1")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    
    // Gson for JSON
    implementation("com.google.code.gson:gson:2.10.1")
    
    // Local Broadcast
    implementation("androidx.localbroadcastmanager:localbroadcastmanager:1.1.0")
}
```

## 3. AndroidManifest.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <!-- SMS Permissions -->
    <uses-permission android:name="android.permission.SEND_SMS" />
    <uses-permission android:name="android.permission.READ_SMS" />
    <uses-permission android:name="android.permission.RECEIVE_SMS" />

    <!-- Network Permissions -->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
    <uses-permission android:name="android.permission.CHANGE_WIFI_STATE" />

    <!-- Background Service Permissions -->
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC" />
    <uses-permission android:name="android.permission.WAKE_LOCK" />
    <uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" />
    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />

    <!-- Notification Permission (Android 13+) -->
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.SMSGateway"
        android:name=".SmsGatewayApplication"
        tools:targetApi="34">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:launchMode="singleTop">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <service
            android:name=".SmsGatewayService"
            android:enabled="true"
            android:exported="false"
            android:foregroundServiceType="dataSync"
            android:stopWithTask="false" />

        <receiver
            android:name=".receiver.BootReceiver"
            android:enabled="true"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.BOOT_COMPLETED" />
                <action android:name="android.intent.action.QUICKBOOT_POWERON" />
            </intent-filter>
        </receiver>

        <receiver
            android:name=".receiver.SmsReceiver"
            android:enabled="true"
            android:exported="true"
            android:permission="android.permission.BROADCAST_SMS">
            <intent-filter android:priority="999">
                <action android:name="android.provider.Telephony.SMS_RECEIVED" />
            </intent-filter>
        </receiver>

    </application>

</manifest>
```

## 4. res/values/strings.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">SMS Gateway</string>
    <string name="server_status">Server Status</string>
    <string name="websocket_port">WebSocket Port</string>
    <string name="http_port">HTTP Port</string>
    <string name="default_ws_port">8080</string>
    <string name="default_http_port">8070</string>
    <string name="connected_devices">Connected Devices (WebSocket)</string>
    <string name="http_console">HTTP Console Log</string>
    <string name="server_running">Server Running</string>
    <string name="server_stopped">Server Stopped</string>
    <string name="no_devices_connected">No devices connected</string>
    <string name="no_logs">No logs yet</string>
    <string name="clear_logs">Clear Logs</string>
    <string name="ip_addresses">Server IP Addresses</string>
    <string name="notification_channel_name">SMS Gateway Service</string>
    <string name="notification_channel_desc">Keeps SMS Gateway running in background</string>
</resources>
```

## 5. res/values/colors.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="primary">#1976D2</color>
    <color name="primary_dark">#1565C0</color>
    <color name="primary_light">#BBDEFB</color>
    <color name="accent">#FF5722</color>
    <color name="text_primary">#212121</color>
    <color name="text_secondary">#757575</color>
    <color name="divider">#BDBDBD</color>
    <color name="background">#FAFAFA</color>
    <color name="card_background">#FFFFFF</color>
    <color name="success">#4CAF50</color>
    <color name="error">#F44336</color>
    <color name="warning">#FF9800</color>
    <color name="info">#2196F3</color>
    <color name="console_background">#1E1E1E</color>
    <color name="console_text">#00FF00</color>
</resources>
```

## 6. res/values/themes.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.SMSGateway" parent="Theme.Material3.Light.NoActionBar">
        <item name="colorPrimary">@color/primary</item>
        <item name="colorPrimaryDark">@color/primary_dark</item>
        <item name="colorAccent">@color/accent</item>
        <item name="android:statusBarColor">@color/primary_dark</item>
    </style>
</resources>
```

## 7. res/layout/activity_main.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.coordinatorlayout.widget.CoordinatorLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/background"
    tools:context=".MainActivity">

    <com.google.android.material.appbar.AppBarLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content">

        <com.google.android.material.appbar.MaterialToolbar
            android:id="@+id/toolbar"
            android:layout_width="match_parent"
            android:layout_height="?attr/actionBarSize"
            android:background="@color/primary"
            app:title="@string/app_name"
            app:titleTextColor="@android:color/white" />

    </com.google.android.material.appbar.AppBarLayout>

    <androidx.core.widget.NestedScrollView
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        app:layout_behavior="@string/appbar_scrolling_view_behavior">

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical"
            android:padding="16dp">

            <!-- Server Control Card -->
            <com.google.android.material.card.MaterialCardView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginBottom="16dp"
                app:cardCornerRadius="12dp"
                app:cardElevation="4dp">

                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:orientation="vertical"
                    android:padding="16dp">

                    <LinearLayout
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:gravity="center_vertical"
                        android:orientation="horizontal">

                        <LinearLayout
                            android:layout_width="0dp"
                            android:layout_height="wrap_content"
                            android:layout_weight="1"
                            android:orientation="vertical">

                            <TextView
                                android:layout_width="wrap_content"
                                android:layout_height="wrap_content"
                                android:text="@string/server_status"
                                android:textSize="18sp"
                                android:textStyle="bold"
                                android:textColor="@color/text_primary" />

                            <TextView
                                android:id="@+id/tvServerStatus"
                                android:layout_width="wrap_content"
                                android:layout_height="wrap_content"
                                android:text="@string/server_stopped"
                                android:textColor="@color/error"
                                android:textSize="14sp" />

                        </LinearLayout>

                        <com.google.android.material.switchmaterial.SwitchMaterial
                            android:id="@+id/switchServer"
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:scaleX="1.3"
                            android:scaleY="1.3" />

                    </LinearLayout>

                    <View
                        android:layout_width="match_parent"
                        android:layout_height="1dp"
                        android:layout_marginVertical="16dp"
                        android:background="@color/divider" />

                    <!-- Port Configuration -->
                    <LinearLayout
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:orientation="horizontal">

                        <com.google.android.material.textfield.TextInputLayout
                            android:layout_width="0dp"
                            android:layout_height="wrap_content"
                            android:layout_marginEnd="8dp"
                            android:layout_weight="1"
                            android:hint="@string/websocket_port"
                            style="@style/Widget.Material3.TextInputLayout.OutlinedBox">

                            <com.google.android.material.textfield.TextInputEditText
                                android:id="@+id/etWebSocketPort"
                                android:layout_width="match_parent"
                                android:layout_height="wrap_content"
                                android:inputType="number"
                                android:maxLength="5" />

                        </com.google.android.material.textfield.TextInputLayout>

                        <com.google.android.material.textfield.TextInputLayout
                            android:layout_width="0dp"
                            android:layout_height="wrap_content"
                            android:layout_marginStart="8dp"
                            android:layout_weight="1"
                            android:hint="@string/http_port"
                            style="@style/Widget.Material3.TextInputLayout.OutlinedBox">

                            <com.google.android.material.textfield.TextInputEditText
                                android:id="@+id/etHttpPort"
                                android:layout_width="match_parent"
                                android:layout_height="wrap_content"
                                android:inputType="number"
                                android:maxLength="5" />

                        </com.google.android.material.textfield.TextInputLayout>

                    </LinearLayout>

                </LinearLayout>

            </com.google.android.material.card.MaterialCardView>

            <!-- IP Addresses Card -->
            <com.google.android.material.card.MaterialCardView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginBottom="16dp"
                app:cardCornerRadius="12dp"
                app:cardElevation="4dp">

                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:orientation="vertical"
                    android:padding="16dp">

                    <TextView
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="@string/ip_addresses"
                        android:textSize="16sp"
                        android:textStyle="bold"
                        android:textColor="@color/text_primary" />

                    <TextView
                        android:id="@+id/tvIpAddresses"
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:layout_marginTop="8dp"
                        android:fontFamily="monospace"
                        android:textColor="@color/info"
                        android:textSize="14sp" />

                </LinearLayout>

            </com.google.android.material.card.MaterialCardView>

            <!-- Connected Devices Card -->
            <com.google.android.material.card.MaterialCardView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginBottom="16dp"
                app:cardCornerRadius="12dp"
                app:cardElevation="4dp">

                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:orientation="vertical"
                    android:padding="16dp">

                    <LinearLayout
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:gravity="center_vertical"
                        android:orientation="horizontal">

                        <TextView
                            android:layout_width="0dp"
                            android:layout_height="wrap_content"
                            android:layout_weight="1"
                            android:text="@string/connected_devices"
                            android:textSize="16sp"
                            android:textStyle="bold"
                            android:textColor="@color/text_primary" />

                        <TextView
                            android:id="@+id/tvDeviceCount"
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:background="@color/primary"
                            android:paddingHorizontal="12dp"
                            android:paddingVertical="4dp"
                            android:text="0"
                            android:textColor="@android:color/white"
                            android:textSize="12sp" />

                    </LinearLayout>

                    <androidx.recyclerview.widget.RecyclerView
                        android:id="@+id/rvConnectedDevices"
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:layout_marginTop="8dp"
                        android:maxHeight="200dp"
                        android:nestedScrollingEnabled="false" />

                    <TextView
                        android:id="@+id/tvNoDevices"
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:gravity="center"
                        android:padding="16dp"
                        android:text="@string/no_devices_connected"
                        android:textColor="@color/text_secondary" />

                </LinearLayout>

            </com.google.android.material.card.MaterialCardView>

            <!-- HTTP Console Card -->
            <com.google.android.material.card.MaterialCardView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                app:cardCornerRadius="12dp"
                app:cardElevation="4dp">

                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:orientation="vertical"
                    android:padding="16dp">

                    <LinearLayout
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:gravity="center_vertical"
                        android:orientation="horizontal">

                        <TextView
                            android:layout_width="0dp"
                            android:layout_height="wrap_content"
                            android:layout_weight="1"
                            android:text="@string/http_console"
                            android:textSize="16sp"
                            android:textStyle="bold"
                            android:textColor="@color/text_primary" />

                        <com.google.android.material.button.MaterialButton
                            android:id="@+id/btnClearLogs"
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:text="@string/clear_logs"
                            style="@style/Widget.Material3.Button.TextButton" />

                    </LinearLayout>

                    <androidx.recyclerview.widget.RecyclerView
                        android:id="@+id/rvConsoleLogs"
                        android:layout_width="match_parent"
                        android:layout_height="300dp"
                        android:layout_marginTop="8dp"
                        android:background="@color/console_background"
                        android:padding="8dp" />

                    <TextView
                        android:id="@+id/tvNoLogs"
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:gravity="center"
                        android:padding="16dp"
                        android:text="@string/no_logs"
                        android:textColor="@color/text_secondary"
                        android:visibility="gone" />

                </LinearLayout>

            </com.google.android.material.card.MaterialCardView>

        </LinearLayout>

    </androidx.core.widget.NestedScrollView>

</androidx.coordinatorlayout.widget.CoordinatorLayout>
```

## 8. res/layout/item_connected_device.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="horizontal"
    android:padding="12dp"
    android:gravity="center_vertical"
    android:background="?attr/selectableItemBackground">

    <View
        android:id="@+id/statusIndicator"
        android:layout_width="8dp"
        android:layout_height="8dp"
        android:background="@drawable/circle_green" />

    <LinearLayout
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_weight="1"
        android:layout_marginStart="12dp"
        android:orientation="vertical">

        <TextView
            android:id="@+id/tvDeviceIp"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:textSize="14sp"
            android:textColor="@color/text_primary"
            android:fontFamily="monospace" />

        <TextView
            android:id="@+id/tvConnectedTime"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:textSize="12sp"
            android:textColor="@color/text_secondary" />

    </LinearLayout>

</LinearLayout>
```

## 9. res/layout/item_log_entry.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="horizontal"
    android:paddingVertical="4dp">

    <TextView
        android:id="@+id/tvTimestamp"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:textColor="#888888"
        android:textSize="11sp"
        android:fontFamily="monospace" />

    <TextView
        android:id="@+id/tvMethod"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginStart="8dp"
        android:textSize="11sp"
        android:fontFamily="monospace"
        android:textStyle="bold" />

    <TextView
        android:id="@+id/tvLogMessage"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_weight="1"
        android:layout_marginStart="8dp"
        android:textColor="@color/console_text"
        android:textSize="11sp"
        android:fontFamily="monospace" />

</LinearLayout>
```

## 10. res/drawable/circle_green.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="oval">
    <solid android:color="@color/success" />
    <size
        android:width="8dp"
        android:height="8dp" />
</shape>
```

## 11. Model Classes

### model/ConnectedDevice.kt

```kotlin
package com.smsgateway.model

data class ConnectedDevice(
    val id: String,
    val ipAddress: String,
    val connectedAt: Long = System.currentTimeMillis()
)
```

### model/LogEntry.kt

```kotlin
package com.smsgateway.model

data class LogEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val method: String,
    val path: String,
    val clientIp: String,
    val statusCode: Int = 200,
    val message: String = ""
) {
    enum class LogType {
        INFO, SUCCESS, ERROR, WARNING
    }
    
    val type: LogType
        get() = when {
            statusCode in 200..299 -> LogType.SUCCESS
            statusCode in 400..499 -> LogType.WARNING
            statusCode >= 500 -> LogType.ERROR
            else -> LogType.INFO
        }
}
```

### model/SmsRequest.kt

```kotlin
package com.smsgateway.model

data class SmsRequest(
    val phoneNumber: String,
    val message: String
)

data class SmsResponse(
    val success: Boolean,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)
```

## 12. Utility Classes

### util/NetworkUtils.kt

```kotlin
package com.smsgateway.util

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
```

### util/PreferenceManager.kt

```kotlin
package com.smsgateway.util

import android.content.Context
import android.content.SharedPreferences

class PreferenceManager(context: Context) {
    
    companion object {
        private const val PREF_NAME = "sms_gateway_prefs"
        private const val KEY_WS_PORT = "websocket_port"
        private const val KEY_HTTP_PORT = "http_port"
        private const val KEY_SERVER_ENABLED = "server_enabled"
        
        const val DEFAULT_WS_PORT = 8080
        const val DEFAULT_HTTP_PORT = 8070
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    
    var webSocketPort: Int
        get() = prefs.getInt(KEY_WS_PORT, DEFAULT_WS_PORT)
        set(value) = prefs.edit().putInt(KEY_WS_PORT, value).apply()
    
    var httpPort: Int
        get() = prefs.getInt(KEY_HTTP_PORT, DEFAULT_HTTP_PORT)
        set(value) = prefs.edit().putInt(KEY_HTTP_PORT, value).apply()
    
    var serverEnabled: Boolean
        get() = prefs.getBoolean(KEY_SERVER_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_SERVER_ENABLED, value).apply()
}
```

## 13. Adapters

### adapter/ConnectedDevicesAdapter.kt

```kotlin
package com.smsgateway.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.smsgateway.databinding.ItemConnectedDeviceBinding
import com.smsgateway.model.ConnectedDevice
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ConnectedDevicesAdapter : ListAdapter<ConnectedDevice, ConnectedDevicesAdapter.DeviceViewHolder>(DeviceDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val binding = ItemConnectedDeviceBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return DeviceViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    class DeviceViewHolder(private val binding: ItemConnectedDeviceBinding) : 
        RecyclerView.ViewHolder(binding.root) {
        
        private val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        
        fun bind(device: ConnectedDevice) {
            binding.tvDeviceIp.text = device.ipAddress
            binding.tvConnectedTime.text = "Connected at ${dateFormat.format(Date(device.connectedAt))}"
        }
    }
    
    class DeviceDiffCallback : DiffUtil.ItemCallback<ConnectedDevice>() {
        override fun areItemsTheSame(oldItem: ConnectedDevice, newItem: ConnectedDevice): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: ConnectedDevice, newItem: ConnectedDevice): Boolean {
            return oldItem == newItem
        }
    }
}
```

### adapter/ConsoleLogAdapter.kt

```kotlin
package com.smsgateway.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.smsgateway.databinding.ItemLogEntryBinding
import com.smsgateway.model.LogEntry
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ConsoleLogAdapter : ListAdapter<LogEntry, ConsoleLogAdapter.LogViewHolder>(LogDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val binding = ItemLogEntryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return LogViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    class LogViewHolder(private val binding: ItemLogEntryBinding) : 
        RecyclerView.ViewHolder(binding.root) {
        
        private val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        
        fun bind(logEntry: LogEntry) {
            binding.tvTimestamp.text = "[${dateFormat.format(Date(logEntry.timestamp))}]"
            binding.tvMethod.text = logEntry.method
            binding.tvLogMessage.text = "${logEntry.path} from ${logEntry.clientIp} - ${logEntry.statusCode}"
            
            // Color code based on method
            val methodColor = when (logEntry.method) {
                "GET" -> Color.parseColor("#61AFEF")
                "POST" -> Color.parseColor("#98C379")
                "PUT" -> Color.parseColor("#E5C07B")
                "DELETE" -> Color.parseColor("#E06C75")
                else -> Color.parseColor("#ABB2BF")
            }
            binding.tvMethod.setTextColor(methodColor)
            
            // Color code based on status
            val statusColor = when (logEntry.type) {
                LogEntry.LogType.SUCCESS -> Color.parseColor("#98C379")
                LogEntry.LogType.WARNING -> Color.parseColor("#E5C07B")
                LogEntry.LogType.ERROR -> Color.parseColor("#E06C75")
                LogEntry.LogType.INFO -> Color.parseColor("#00FF00")
            }
            binding.tvLogMessage.setTextColor(statusColor)
        }
    }
    
    class LogDiffCallback : DiffUtil.ItemCallback<LogEntry>() {
        override fun areItemsTheSame(oldItem: LogEntry, newItem: LogEntry): Boolean {
            return oldItem.timestamp == newItem.timestamp
        }
        
        override fun areContentsTheSame(oldItem: LogEntry, newItem: LogEntry): Boolean {
            return oldItem == newItem
        }
    }
}
```

## 14. Server Classes

### server/SmsWebSocketServer.kt

```kotlin
package com.smsgateway.server

import android.content.Context
import android.content.Intent
import android.telephony.SmsManager
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.gson.Gson
import com.smsgateway.model.ConnectedDevice
import com.smsgateway.model.SmsRequest
import com.smsgateway.model.SmsResponse
import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import java.lang.Exception
import java.net.InetSocketAddress
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class SmsWebSocketServer(
    private val context: Context,
    port: Int
) : WebSocketServer(InetSocketAddress("0.0.0.0", port)) {
    
    companion object {
        private const val TAG = "SmsWebSocketServer"
        const val ACTION_DEVICE_CONNECTED = "com.smsgateway.DEVICE_CONNECTED"
        const val ACTION_DEVICE_DISCONNECTED = "com.smsgateway.DEVICE_DISCONNECTED"
        const val ACTION_DEVICES_UPDATE = "com.smsgateway.DEVICES_UPDATE"
        const val EXTRA_DEVICE = "device"
        const val EXTRA_DEVICES = "devices"
    }
    
    private val gson = Gson()
    private val connectedDevices = ConcurrentHashMap<WebSocket, ConnectedDevice>()
    
    override fun onOpen(conn: WebSocket, handshake: ClientHandshake) {
        val clientIp = conn.remoteSocketAddress?.address?.hostAddress ?: "unknown"
        val device = ConnectedDevice(
            id = UUID.randomUUID().toString(),
            ipAddress = clientIp
        )
        connectedDevices[conn] = device
        
        Log.d(TAG, "New connection from: $clientIp")
        
        // Send welcome message
        val welcomeMsg = mapOf(
            "type" to "connected",
            "message" to "Connected to SMS Gateway",
            "timestamp" to System.currentTimeMillis()
        )
        conn.send(gson.toJson(welcomeMsg))
        
        broadcastDevicesUpdate()
    }
    
    override fun onClose(conn: WebSocket, code: Int, reason: String, remote: Boolean) {
        val device = connectedDevices.remove(conn)
        Log.d(TAG, "Connection closed: ${device?.ipAddress}")
        broadcastDevicesUpdate()
    }
    
    override fun onMessage(conn: WebSocket, message: String) {
        Log.d(TAG, "Received message: $message")
        
        try {
            val request = gson.fromJson(message, Map::class.java)
            val action = request["action"] as? String
            
            when (action) {
                "send_sms" -> {
                    val phoneNumber = request["phone_number"] as? String
                    val smsMessage = request["message"] as? String
                    
                    if (phoneNumber != null && smsMessage != null) {
                        val result = sendSms(phoneNumber, smsMessage)
                        conn.send(gson.toJson(result))
                    } else {
                        val error = SmsResponse(false, "Missing phone_number or message")
                        conn.send(gson.toJson(error))
                    }
                }
                "ping" -> {
                    val pong = mapOf("type" to "pong", "timestamp" to System.currentTimeMillis())
                    conn.send(gson.toJson(pong))
                }
                else -> {
                    val error = mapOf("error" to "Unknown action: $action")
                    conn.send(gson.toJson(error))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing message", e)
            val error = SmsResponse(false, "Error: ${e.message}")
            conn.send(gson.toJson(error))
        }
    }
    
    override fun onError(conn: WebSocket?, ex: Exception) {
        Log.e(TAG, "WebSocket error", ex)
        conn?.let { 
            connectedDevices.remove(it)
            broadcastDevicesUpdate()
        }
    }
    
    override fun onStart() {
        Log.d(TAG, "WebSocket server started on port $port")
        connectionLostTimeout = 100
    }
    
    private fun sendSms(phoneNumber: String, message: String): SmsResponse {
        return try {
            val smsManager = context.getSystemService(SmsManager::class.java)
            
            if (message.length > 160) {
                val parts = smsManager.divideMessage(message)
                smsManager.sendMultipartTextMessage(phoneNumber, null, parts, null, null)
            } else {
                smsManager.sendTextMessage(phoneNumber, null, message, null, null)
            }
            
            SmsResponse(true, "SMS sent successfully to $phoneNumber")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send SMS", e)
            SmsResponse(false, "Failed to send SMS: ${e.message}")
        }
    }
    
    private fun broadcastDevicesUpdate() {
        val intent = Intent(ACTION_DEVICES_UPDATE).apply {
            putParcelableArrayListExtra(EXTRA_DEVICES, ArrayList(connectedDevices.values.map {
                android.os.Bundle().apply {
                    putString("id", it.id)
                    putString("ip", it.ipAddress)
                    putLong("connectedAt", it.connectedAt)
                }
            }))
        }
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
    }
    
    fun getConnectedDevices(): List<ConnectedDevice> = connectedDevices.values.toList()
    
    fun broadcastMessage(message: String) {
        broadcast(message)
    }
    
    fun getConnectionCount(): Int = connections.size
}
```

### server/SmsHttpServer.kt

```kotlin
package com.smsgateway.server

import android.content.Context
import android.content.Intent
import android.telephony.SmsManager
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.gson.Gson
import com.smsgateway.model.LogEntry
import com.smsgateway.model.SmsResponse
import fi.iki.elonen.NanoHTTPD
import java.io.IOException

class SmsHttpServer(
    private val context: Context,
    port: Int
) : NanoHTTPD("0.0.0.0", port) {
    
    companion object {
        private const val TAG = "SmsHttpServer"
        const val ACTION_LOG_ENTRY = "com.smsgateway.LOG_ENTRY"
        const val EXTRA_LOG_ENTRY = "log_entry"
    }
    
    private val gson = Gson()
    
    override fun serve(session: IHTTPSession): Response {
        val method = session.method.name
        val uri = session.uri
        val clientIp = session.remoteIpAddress
        
        Log.d(TAG, "$method $uri from $clientIp")
        
        val response = try {
            when {
                uri == "/" || uri == "/status" -> handleStatus(session)
                uri == "/send" || uri == "/send-sms" -> handleSendSms(session)
                uri == "/health" -> handleHealth()
                uri.startsWith("/api/") -> handleApiRequest(session)
                else -> handleNotFound(uri)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling request", e)
            createErrorResponse(500, "Internal Server Error: ${e.message}")
        }
        
        // Log the request
        val logEntry = LogEntry(
            method = method,
            path = uri,
            clientIp = clientIp,
            statusCode = response.status.requestStatus
        )
        broadcastLogEntry(logEntry)
        
        // Add CORS headers
        response.addHeader("Access-Control-Allow-Origin", "*")
        response.addHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS")
        response.addHeader("Access-Control-Allow-Headers", "Content-Type, Authorization")
        
        return response
    }
    
    private fun handleStatus(session: IHTTPSession): Response {
        val status = mapOf(
            "status" to "running",
            "server" to "SMS Gateway",
            "version" to "1.0",
            "timestamp" to System.currentTimeMillis(),
            "endpoints" to listOf(
                mapOf("method" to "GET", "path" to "/status", "description" to "Server status"),
                mapOf("method" to "GET", "path" to "/health", "description" to "Health check"),
                mapOf("method" to "GET/POST", "path" to "/send", "description" to "Send SMS"),
                mapOf("method" to "POST", "path" to "/api/sms/send", "description" to "Send SMS (API)")
            )
        )
        return createJsonResponse(status)
    }
    
    private fun handleHealth(): Response {
        val health = mapOf(
            "status" to "healthy",
            "timestamp" to System.currentTimeMillis()
        )
        return createJsonResponse(health)
    }
    
    private fun handleSendSms(session: IHTTPSession): Response {
        val phoneNumber: String?
        val message: String?
        
        when (session.method) {
            Method.GET -> {
                phoneNumber = session.parms["phone"] ?: session.parms["to"] ?: session.parms["number"]
                message = session.parms["message"] ?: session.parms["text"] ?: session.parms["msg"]
            }
            Method.POST -> {
                val contentLength = session.headers["content-length"]?.toIntOrNull() ?: 0
                val body = mutableMapOf<String, String>()
                
                try {
                    session.parseBody(body)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing body", e)
                }
                
                val postData = body["postData"]
                
                if (postData != null && postData.startsWith("{")) {
                    // JSON body
                    try {
                        val jsonBody = gson.fromJson(postData, Map::class.java)
                        phoneNumber = jsonBody["phone"] as? String 
                            ?: jsonBody["phone_number"] as? String 
                            ?: jsonBody["to"] as? String
                        message = jsonBody["message"] as? String 
                            ?: jsonBody["text"] as? String
                    } catch (e: Exception) {
                        return createErrorResponse(400, "Invalid JSON body")
                    }
                } else {
                    // Form data
                    phoneNumber = session.parms["phone"] ?: session.parms["to"] ?: session.parms["number"]
                    message = session.parms["message"] ?: session.parms["text"] ?: session.parms["msg"]
                }
            }
            Method.OPTIONS -> {
                return newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, "")
            }
            else -> {
                return createErrorResponse(405, "Method not allowed")
            }
        }
        
        if (phoneNumber.isNullOrBlank()) {
            return createErrorResponse(400, "Missing phone number. Use 'phone', 'to', or 'number' parameter")
        }
        
        if (message.isNullOrBlank()) {
            return createErrorResponse(400, "Missing message. Use 'message', 'text', or 'msg' parameter")
        }
        
        val result = sendSms(phoneNumber, message)
        val status = if (result.success) Response.Status.OK else Response.Status.INTERNAL_ERROR
        return newFixedLengthResponse(status, "application/json", gson.toJson(result))
    }
    
    private fun handleApiRequest(session: IHTTPSession): Response {
        return when (session.uri) {
            "/api/sms/send" -> handleSendSms(session)
            "/api/status" -> handleStatus(session)
            else -> handleNotFound(session.uri)
        }
    }
    
    private fun handleNotFound(uri: String): Response {
        val error = mapOf(
            "error" to "Not Found",
            "path" to uri,
            "message" to "The requested endpoint does not exist"
        )
        return newFixedLengthResponse(Response.Status.NOT_FOUND, "application/json", gson.toJson(error))
    }
    
    private fun sendSms(phoneNumber: String, message: String): SmsResponse {
        return try {
            val smsManager = context.getSystemService(SmsManager::class.java)
            
            if (message.length > 160) {
                val parts = smsManager.divideMessage(message)
                smsManager.sendMultipartTextMessage(phoneNumber, null, parts, null, null)
            } else {
                smsManager.sendTextMessage(phoneNumber, null, message, null, null)
            }
            
            SmsResponse(true, "SMS sent successfully to $phoneNumber")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send SMS", e)
            SmsResponse(false, "Failed to send SMS: ${e.message}")
        }
    }
    
    private fun createJsonResponse(data: Any): Response {
        return newFixedLengthResponse(Response.Status.OK, "application/json", gson.toJson(data))
    }
    
    private fun createErrorResponse(code: Int, message: String): Response {
        val error = mapOf("error" to message, "code" to code)
        val status = when (code) {
            400 -> Response.Status.BAD_REQUEST
            404 -> Response.Status.NOT_FOUND
            405 -> Response.Status.METHOD_NOT_ALLOWED
            else -> Response.Status.INTERNAL_ERROR
        }
        return newFixedLengthResponse(status, "application/json", gson.toJson(error))
    }
    
    private fun broadcastLogEntry(logEntry: LogEntry) {
        val intent = Intent(ACTION_LOG_ENTRY).apply {
            putExtra("timestamp", logEntry.timestamp)
            putExtra("method", logEntry.method)
            putExtra("path", logEntry.path)
            putExtra("clientIp", logEntry.clientIp)
            putExtra("statusCode", logEntry.statusCode)
        }
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
    }
}
```

## 15. Foreground Service

### SmsGatewayService.kt

```kotlin
package com.smsgateway

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.smsgateway.server.SmsHttpServer
import com.smsgateway.server.SmsWebSocketServer
import com.smsgateway.util.NetworkUtils
import com.smsgateway.util.PreferenceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.IOException

class SmsGatewayService : Service() {
    
    companion object {
        private const val TAG = "SmsGatewayService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "sms_gateway_channel"
        
        const val ACTION_START = "com.smsgateway.START"
        const val ACTION_STOP = "com.smsgateway.STOP"
        const val EXTRA_WS_PORT = "ws_port"
        const val EXTRA_HTTP_PORT = "http_port"
        
        fun start(context: Context, wsPort: Int, httpPort: Int) {
            val intent = Intent(context, SmsGatewayService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_WS_PORT, wsPort)
                putExtra(EXTRA_HTTP_PORT, httpPort)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        
        fun stop(context: Context) {
            val intent = Intent(context, SmsGatewayService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
    
    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    private var webSocketServer: SmsWebSocketServer? = null
    private var httpServer: SmsHttpServer? = null
    private var wakeLock: PowerManager.WakeLock? = null
    
    private var wsPort = PreferenceManager.DEFAULT_WS_PORT
    private var httpPort = PreferenceManager.DEFAULT_HTTP_PORT
    
    var isRunning = false
        private set
    
    inner class LocalBinder : Binder() {
        fun getService(): SmsGatewayService = this@SmsGatewayService
    }
    
    override fun onBind(intent: Intent): IBinder = binder
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        acquireWakeLock()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                wsPort = intent.getIntExtra(EXTRA_WS_PORT, PreferenceManager.DEFAULT_WS_PORT)
                httpPort = intent.getIntExtra(EXTRA_HTTP_PORT, PreferenceManager.DEFAULT_HTTP_PORT)
                startServers()
            }
            ACTION_STOP -> {
                stopServers()
                stopSelf()
            }
        }
        
        return START_STICKY
    }
    
    private fun startServers() {
        if (isRunning) return
        
        startForeground(NOTIFICATION_ID, createNotification())
        
        serviceScope.launch {
            try {
                // Start WebSocket Server
                webSocketServer = SmsWebSocketServer(applicationContext, wsPort).apply {
                    isReuseAddr = true
                    start()
                }
                Log.d(TAG, "WebSocket server started on port $wsPort")
                
                // Start HTTP Server
                httpServer = SmsHttpServer(applicationContext, httpPort).apply {
                    start()
                }
                Log.d(TAG, "HTTP server started on port $httpPort")
                
                isRunning = true
                updateNotification()
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start servers", e)
                stopServers()
            }
        }
    }
    
    private fun stopServers() {
        isRunning = false
        
        try {
            webSocketServer?.stop(1000)
            webSocketServer = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping WebSocket server", e)
        }
        
        try {
            httpServer?.stop()
            httpServer = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping HTTP server", e)
        }
        
        releaseWakeLock()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_desc)
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val stopIntent = Intent(this, SmsGatewayService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val ips = NetworkUtils.getAllIpAddresses().joinToString(", ")
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SMS Gateway Running")
            .setContentText("WS: $wsPort | HTTP: $httpPort")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("WebSocket Port: $wsPort\nHTTP Port: $httpPort\nIPs: $ips"))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_stop, "Stop", stopPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }
    
    private fun updateNotification() {
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, createNotification())
    }
    
    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "SmsGateway::WakeLock"
        ).apply {
            acquire(10*60*1000L /*10 minutes*/)
        }
    }
    
    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
        wakeLock = null
    }
    
    fun getWebSocketServer(): SmsWebSocketServer? = webSocketServer
    fun getHttpServer(): SmsHttpServer? = httpServer
    fun getWsPort(): Int = wsPort
    fun getHttpPort(): Int = httpPort
    
    override fun onDestroy() {
        stopServers()
        super.onDestroy()
    }
    
    override fun onTaskRemoved(rootIntent: Intent?) {
        // Service continues running even when app is removed from recents
        super.onTaskRemoved(rootIntent)
    }
}
```

## 16. Application Class

### SmsGatewayApplication.kt

```kotlin
package com.smsgateway

import android.app.Application
import com.smsgateway.util.PreferenceManager

class SmsGatewayApplication : Application() {
    
    lateinit var preferenceManager: PreferenceManager
        private set
    
    override fun onCreate() {
        super.onCreate()
        preferenceManager = PreferenceManager(this)
    }
}
```

## 17. Boot Receiver

### receiver/BootReceiver.kt

```kotlin
package com.smsgateway.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.smsgateway.SmsGatewayService
import com.smsgateway.util.PreferenceManager

class BootReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            
            val prefs = PreferenceManager(context)
            
            if (prefs.serverEnabled) {
                SmsGatewayService.start(
                    context,
                    prefs.webSocketPort,
                    prefs.httpPort
                )
            }
        }
    }
}
```

### receiver/SmsReceiver.kt

```kotlin
package com.smsgateway.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log

class SmsReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "SmsReceiver"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            
            for (sms in messages) {
                val sender = sms.displayOriginatingAddress
                val body = sms.messageBody
                
                Log.d(TAG, "SMS received from $sender: $body")
                
                // You can broadcast this to connected WebSocket clients
                // or store it for retrieval via HTTP
            }
        }
    }
}
```

## 18. Main Activity

### MainActivity.kt

```kotlin
package com.smsgateway

import android.Manifest
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.smsgateway.adapter.ConnectedDevicesAdapter
import com.smsgateway.adapter.ConsoleLogAdapter
import com.smsgateway.databinding.ActivityMainBinding
import com.smsgateway.model.ConnectedDevice
import com.smsgateway.model.LogEntry
import com.smsgateway.server.SmsHttpServer
import com.smsgateway.server.SmsWebSocketServer
import com.smsgateway.util.NetworkUtils
import com.smsgateway.util.PreferenceManager

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: PreferenceManager
    
    private lateinit var devicesAdapter: ConnectedDevicesAdapter
    private lateinit var logsAdapter: ConsoleLogAdapter
    
    private val logEntries = mutableListOf<LogEntry>()
    private val connectedDevices = mutableListOf<ConnectedDevice>()
    
    private var gatewayService: SmsGatewayService? = null
    private var serviceBound = false
    
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as SmsGatewayService.LocalBinder
            gatewayService = binder.getService()
            serviceBound = true
            updateUI()
        }
        
        override fun onServiceDisconnected(name: ComponentName?) {
            gatewayService = null
            serviceBound = false
        }
    }
    
    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                SmsWebSocketServer.ACTION_DEVICES_UPDATE -> {
                    updateConnectedDevices()
                }
                SmsHttpServer.ACTION_LOG_ENTRY -> {
                    val logEntry = LogEntry(
                        timestamp = intent.getLongExtra("timestamp", System.currentTimeMillis()),
                        method = intent.getStringExtra("method") ?: "",
                        path = intent.getStringExtra("path") ?: "",
                        clientIp = intent.getStringExtra("clientIp") ?: "",
                        statusCode = intent.getIntExtra("statusCode", 200)
                    )
                    addLogEntry(logEntry)
                }
            }
        }
    }
    
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        if (allGranted) {
            checkBatteryOptimization()
        } else {
            Toast.makeText(this, "Permissions required for SMS Gateway to work", Toast.LENGTH_LONG).show()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        prefs = PreferenceManager(this)
        
        setupRecyclerViews()
        setupListeners()
        requestPermissions()
        
        // Restore state
        binding.etWebSocketPort.setText(
            if (prefs.webSocketPort != PreferenceManager.DEFAULT_WS_PORT) 
                prefs.webSocketPort.toString() 
            else ""
        )
        binding.etHttpPort.setText(
            if (prefs.httpPort != PreferenceManager.DEFAULT_HTTP_PORT) 
                prefs.httpPort.toString() 
            else ""
        )
        binding.switchServer.isChecked = prefs.serverEnabled
    }
    
    override fun onStart() {
        super.onStart()
        bindService()
        registerReceivers()
        updateIpAddresses()
    }
    
    override fun onStop() {
        super.onStop()
        unbindService()
        unregisterReceivers()
    }
    
    private fun setupRecyclerViews() {
        devicesAdapter = ConnectedDevicesAdapter()
        binding.rvConnectedDevices.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = devicesAdapter
        }
        
        logsAdapter = ConsoleLogAdapter()
        binding.rvConsoleLogs.apply {
            layoutManager = LinearLayoutManager(this@MainActivity).apply {
                stackFromEnd = true
            }
            adapter = logsAdapter
        }
    }
    
    private fun setupListeners() {
        binding.switchServer.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                startServer()
            } else {
                stopServer()
            }
        }
        
        binding.btnClearLogs.setOnClickListener {
            logEntries.clear()
            logsAdapter.submitList(emptyList())
        }
    }
    
    private fun startServer() {
        val wsPort = binding.etWebSocketPort.text.toString().toIntOrNull() 
            ?: PreferenceManager.DEFAULT_WS_PORT
        val httpPort = binding.etHttpPort.text.toString().toIntOrNull() 
            ?: PreferenceManager.DEFAULT_HTTP_PORT
        
        // Validate ports
        if (wsPort == httpPort) {
            Toast.makeText(this, "WebSocket and HTTP ports must be different", Toast.LENGTH_SHORT).show()
            binding.switchServer.isChecked = false
            return
        }
        
        if (wsPort !in 1024..65535 || httpPort !in 1024..65535) {
            Toast.makeText(this, "Ports must be between 1024 and 65535", Toast.LENGTH_SHORT).show()
            binding.switchServer.isChecked = false
            return
        }
        
        prefs.webSocketPort = wsPort
        prefs.httpPort = httpPort
        prefs.serverEnabled = true
        
        SmsGatewayService.start(this, wsPort, httpPort)
        
        binding.tvServerStatus.text = getString(R.string.server_running)
        binding.tvServerStatus.setTextColor(ContextCompat.getColor(this, R.color.success))
        
        // Disable port fields when server is running
        binding.etWebSocketPort.isEnabled = false
        binding.etHttpPort.isEnabled = false
        
        updateIpAddresses()
    }
    
    private fun stopServer() {
        prefs.serverEnabled = false
        SmsGatewayService.stop(this)
        
        binding.tvServerStatus.text = getString(R.string.server_stopped)
        binding.tvServerStatus.setTextColor(ContextCompat.getColor(this, R.color.error))
        
        // Enable port fields
        binding.etWebSocketPort.isEnabled = true
        binding.etHttpPort.isEnabled = true
        
        connectedDevices.clear()
        devicesAdapter.submitList(emptyList())
        updateDeviceCount()
    }
    
    private fun bindService() {
        val intent = Intent(this, SmsGatewayService::class.java)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }
    
    private fun unbindService() {
        if (serviceBound) {
            unbindService(serviceConnection)
            serviceBound = false
        }
    }
    
    private fun registerReceivers() {
        val filter = IntentFilter().apply {
            addAction(SmsWebSocketServer.ACTION_DEVICES_UPDATE)
            addAction(SmsHttpServer.ACTION_LOG_ENTRY)
        }
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, filter)
    }
    
    private fun unregisterReceivers() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver)
    }
    
    private fun updateUI() {
        gatewayService?.let { service ->
            if (service.isRunning) {
                binding.switchServer.isChecked = true
                binding.tvServerStatus.text = getString(R.string.server_running)
                binding.tvServerStatus.setTextColor(ContextCompat.getColor(this, R.color.success))
                binding.etWebSocketPort.isEnabled = false
                binding.etHttpPort.isEnabled = false
                
                updateConnectedDevices()
            }
        }
    }
    
    private fun updateConnectedDevices() {
        gatewayService?.getWebSocketServer()?.let { server ->
            val devices = server.getConnectedDevices()
            connectedDevices.clear()
            connectedDevices.addAll(devices)
            devicesAdapter.submitList(devices.toList())
            updateDeviceCount()
            
            binding.tvNoDevices.visibility = if (devices.isEmpty()) 
                android.view.View.VISIBLE else android.view.View.GONE
            binding.rvConnectedDevices.visibility = if (devices.isNotEmpty()) 
                android.view.View.VISIBLE else android.view.View.GONE
        }
    }
    
    private fun updateDeviceCount() {
        binding.tvDeviceCount.text = connectedDevices.size.toString()
    }
    
    private fun addLogEntry(logEntry: LogEntry) {
        logEntries.add(logEntry)
        if (logEntries.size > 500) {
            logEntries.removeAt(0)
        }
        logsAdapter.submitList(logEntries.toList())
        binding.rvConsoleLogs.scrollToPosition(logEntries.size - 1)
    }
    
    private fun updateIpAddresses() {
        val wsPort = binding.etWebSocketPort.text.toString().toIntOrNull() 
            ?: PreferenceManager.DEFAULT_WS_PORT
        val httpPort = binding.etHttpPort.text.toString().toIntOrNull() 
            ?: PreferenceManager.DEFAULT_HTTP_PORT
        
        binding.tvIpAddresses.text = NetworkUtils.formatIpAddressesDisplay(wsPort, httpPort)
    }
    
    private fun requestPermissions() {
        val permissions = mutableListOf<String>()
        
        // SMS permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) 
            != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.SEND_SMS)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) 
            != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.READ_SMS)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) 
            != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.RECEIVE_SMS)
        }
        
        // Notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) 
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        
        if (permissions.isNotEmpty()) {
            permissionLauncher.launch(permissions.toTypedArray())
        } else {
            checkBatteryOptimization()
        }
    }
    
    private fun checkBatteryOptimization() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
            AlertDialog.Builder(this)
                .setTitle("Battery Optimization")
                .setMessage("For the server to run reliably in the background, please disable battery optimization for this app.")
                .setPositiveButton("Settings") { _, _ ->
                    try {
                        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                            data = Uri.parse("package:$packageName")
                        }
                        startActivity(intent)
                    } catch (e: Exception) {
                        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                        startActivity(intent)
                    }
                }
                .setNegativeButton("Later", null)
                .show()
        }
    }
}
```

## 19. Notification Icon (Create as drawable)

### res/drawable/ic_notification.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="#FFFFFF"
        android:pathData="M20,15.5c-1.25,0 -2.45,-0.2 -3.57,-0.57 -0.35,-0.11 -0.74,-0.03 -1.02,0.24l-2.2,2.2c-2.83,-1.44 -5.15,-3.75 -6.59,-6.59l2.2,-2.21c0.28,-0.26 0.36,-0.65 0.25,-1C8.7,6.45 8.5,5.25 8.5,4c0,-0.55 -0.45,-1 -1,-1H4c-0.55,0 -1,0.45 -1,1 0,9.39 7.61,17 17,17 0.55,0 1,-0.45 1,-1v-3.5c0,-0.55 -0.45,-1 -1,-1zM19,12h2c0,-4.97 -4.03,-9 -9,-9v2c3.87,0 7,3.13 7,7zM15,12h2c0,-2.76 -2.24,-5 -5,-5v2c1.66,0 3,1.34 3,3z"/>
</vector>
```

### res/drawable/ic_stop.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="#FFFFFF"
        android:pathData="M6,6h12v12H6z"/>
</vector>
```

## Usage Examples

### HTTP API

```bash
# Send SMS via GET
curl "http://192.168.1.100:8070/send?phone=+1234567890&message=Hello"

# Send SMS via POST (JSON)
curl -X POST "http://192.168.1.100:8070/send" \
  -H "Content-Type: application/json" \
  -d '{"phone": "+1234567890", "message": "Hello from API"}'

# Check status
curl "http://192.168.1.100:8070/status"

# Health check
curl "http://192.168.1.100:8070/health"
```

### WebSocket

```javascript
// Connect to WebSocket
const ws = new WebSocket('ws://192.168.1.100:8080');

ws.onopen = () => {
    console.log('Connected');
    
    // Send SMS
    ws.send(JSON.stringify({
        action: 'send_sms',
        phone_number: '+1234567890',
        message: 'Hello via WebSocket'
    }));
};

ws.onmessage = (event) => {
    console.log('Response:', JSON.parse(event.data));
};
```

## Key Features

| Feature | Implementation |
|---------|---------------|
| **Background Service** | Foreground service with sticky notification |
| **Wake Lock** | Keeps CPU active even when screen is off |
| **Boot Receiver** | Auto-starts on device boot if enabled |
| **Battery Optimization** | Prompts user to disable battery optimization |
| **CORS Support** | All HTTP responses include CORS headers |
| **Multi-network Binding** | Binds to `0.0.0.0` for all network interfaces |
| **WebSocket Clients** | Real-time list of connected devices |
| **HTTP Logging** | Console-style log for all HTTP requests |

This complete implementation provides a robust SMS Gateway that runs reliably in the background!

---

