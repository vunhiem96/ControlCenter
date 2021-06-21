package com.example.controlcenter.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.provider.Settings
import android.content.ContentResolver
import android.accounts.Account
import android.accounts.AccountManager
import android.bluetooth.BluetoothAdapter
import android.content.Context.CONNECTIVITY_SERVICE
import android.content.res.Configuration
import androidx.core.content.contentValuesOf
import android.media.AudioManager
import android.net.wifi.WifiManager
import androidx.core.content.ContextCompat.getSystemService
import android.content.Context.WIFI_SERVICE
import androidx.core.content.ContextCompat.getSystemService
import java.lang.reflect.Method
import android.net.wifi.WifiConfiguration


object Utils {

    fun setCheckControl(context: Context, value: Int) {
        val sharedPreference = context.getSharedPreferences("PREFERENCE_NAME", Context.MODE_PRIVATE)
        var editor = sharedPreference.edit()
        editor.putInt("on", value)
        editor.commit()
    }

    fun getCheckControl(context: Context): Int {
        val sharedPreference = context.getSharedPreferences("PREFERENCE_NAME", Context.MODE_PRIVATE)
        var i: Int = sharedPreference.getInt("on", 1)
        return i
    }

    fun setCheckIcon(context: Context, value: Int) {
        val sharedPreference = context.getSharedPreferences("PREFERENCE_NAME", Context.MODE_PRIVATE)
        var editor = sharedPreference.edit()
        editor.putInt("icon", value)
        editor.commit()
    }

    fun getCheckIcon(context: Context): Int {
        val sharedPreference = context.getSharedPreferences("PREFERENCE_NAME", Context.MODE_PRIVATE)
        var i: Int = sharedPreference.getInt("icon", 0)
        return i
    }

    fun CheckWifi(context: Context): Boolean {
        val manager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        var isWifi = manager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).isConnectedOrConnecting
        return isWifi
    }

    fun CheckPlane(context: Context): Boolean {
        return Settings.System.getInt(
            context.getContentResolver(),
            Settings.Global.AIRPLANE_MODE_ON, 0
        ) != 0
    }

    fun CheckNetwork(context: Context): Boolean {
        val manager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        var is3g = manager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).isConnectedOrConnecting
        return is3g
    }

    fun CheckBluetooth(context: Context): Boolean {
        var mBtAdapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        var isOn: Boolean = mBtAdapter.isEnabled
        return isOn
    }

    fun checkRotate(context: Context): Int {

        if (android.provider.Settings.System.getInt(
                context.contentResolver,
                Settings.System.ACCELEROMETER_ROTATION
            ) == 1
        ) {
            return 1
        } else
            return 0
    }

    fun checkAudio(context: Context): Int {
        var audioManager: AudioManager
        audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (audioManager.ringerMode == AudioManager.RINGER_MODE_SILENT) {
            return 1
        }

        return 0
    }

    fun getLight(context: Context): Int {
        return Settings.System.getInt(
            context.contentResolver,
            android.provider.Settings.System.SCREEN_BRIGHTNESS
        )
    }

    fun getVolume(context: Context): Int {
        var audioManager: AudioManager
        audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        var value = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        return value
    }

    fun checkHotspot(context: Context): Boolean {

        val wifimanager = context.getSystemService(WIFI_SERVICE) as WifiManager
        try {
            val method: Method = wifimanager.javaClass.getDeclaredMethod("isWifiApEnabled")
            return method.invoke(wifimanager) as Boolean
            return true
        } catch (e: Exception) {
            println("asd")
        }
        return false
    }

    fun turnOnHotSpot(context: Context) {
        val wifimanager = context.getSystemService(WIFI_SERVICE) as WifiManager
        val wifiConfiguration = WifiConfiguration()
        wifiConfiguration.SSID = "MyDummySSID"
        val method: Method = wifimanager.javaClass.getDeclaredMethod(
            "setWifiApEnabled",
            WifiConfiguration::class.java,
            java.lang.Boolean.TYPE
        )
        method.invoke(wifimanager, wifiConfiguration, true)
    }

    fun turnOffHotSpot(context: Context) {
        val wifimanager = context.getSystemService(WIFI_SERVICE) as WifiManager
        val wifiConfiguration = WifiConfiguration()
        wifiConfiguration.SSID = "MyDummySSID"
        val method: Method = wifimanager.javaClass.getDeclaredMethod(
            "setWifiApEnabled",
            WifiConfiguration::class.java,
            java.lang.Boolean.TYPE
        )
        method.invoke(wifimanager, wifiConfiguration, false)

    }

    fun SetSize(context: Context, i: Int) {
        val sharedPreference = context.getSharedPreferences("PREFERENCE_NAME", Context.MODE_PRIVATE)
        var editor = sharedPreference.edit()
        editor.putInt("size", i)
        editor.commit()
    }

    fun getSize(context: Context): Int {
        val sharedPreference = context.getSharedPreferences("PREFERENCE_NAME", Context.MODE_PRIVATE)
        var i: Int = sharedPreference.getInt("size", 0)
        return i
    }

    fun setPosition(context: Context, i: Int) {
        val sharedPreference = context.getSharedPreferences("PREFERENCE_NAME", Context.MODE_PRIVATE)
        var editor = sharedPreference.edit()
        editor.putInt("position", i)
        editor.commit()
    }

    fun getPosition(context: Context): Int {
        val sharedPreference = context.getSharedPreferences("PREFERENCE_NAME", Context.MODE_PRIVATE)
        var i: Int = sharedPreference.getInt("position", 1)
        return i
    }
    //

}


