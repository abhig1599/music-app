package com.abhi.musicapp.playback

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.MediaRouter2
import android.os.Build
import android.provider.Settings

/** Where media audio is actually routed right now. UNKNOWN is shown as a neutral icon, never a guess. */
enum class OutputKind { SPEAKER, PHONE, WIRED_HEADPHONES, BLUETOOTH, USB, UNKNOWN }

internal fun classifyOutput(type: Int): OutputKind = when (type) {
    AudioDeviceInfo.TYPE_BUILTIN_SPEAKER, AudioDeviceInfo.TYPE_BUILTIN_SPEAKER_SAFE -> OutputKind.SPEAKER
    AudioDeviceInfo.TYPE_BUILTIN_EARPIECE -> OutputKind.PHONE
    AudioDeviceInfo.TYPE_WIRED_HEADPHONES, AudioDeviceInfo.TYPE_WIRED_HEADSET -> OutputKind.WIRED_HEADPHONES
    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, AudioDeviceInfo.TYPE_BLUETOOTH_SCO, AudioDeviceInfo.TYPE_BLE_HEADSET,
    AudioDeviceInfo.TYPE_BLE_SPEAKER, AudioDeviceInfo.TYPE_BLE_BROADCAST, AudioDeviceInfo.TYPE_HEARING_AID -> OutputKind.BLUETOOTH
    AudioDeviceInfo.TYPE_USB_HEADSET, AudioDeviceInfo.TYPE_USB_DEVICE, AudioDeviceInfo.TYPE_USB_ACCESSORY -> OutputKind.USB
    else -> OutputKind.UNKNOWN
}

object AudioOutput {
    private val mediaAttrs = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()

    /** Routed device for media (API 33+ only). Below 33 there is no exact routed-device query, so UNKNOWN. */
    fun current(context: Context): OutputKind {
        if (Build.VERSION.SDK_INT < 33) return OutputKind.UNKNOWN
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val d = am.getAudioDevicesForAttributes(mediaAttrs)
        return if (d.isEmpty()) OutputKind.UNKNOWN else classifyOutput(d.first().type)
    }

    fun register(context: Context, onChange: () -> Unit): () -> Unit {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val cb = object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded(added: Array<out AudioDeviceInfo>?) = onChange()
            override fun onAudioDevicesRemoved(removed: Array<out AudioDeviceInfo>?) = onChange()
        }
        am.registerAudioDeviceCallback(cb, null)
        return { am.unregisterAudioDeviceCallback(cb) }
    }

    /** System output picker on API 34+; otherwise (or if it refuses) the phone's sound settings. Returns the label to show. */
    fun openPicker(context: Context): String {
        if (Build.VERSION.SDK_INT >= 34) {
            val shown = try { MediaRouter2.getInstance(context).showSystemOutputSwitcher() } catch (e: Exception) { false }
            if (shown) return "Choose output"
        }
        return try {
            context.startActivity(Intent(Settings.ACTION_SOUND_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            "Opened sound settings (this phone has no in-app output picker)"
        } catch (e: Exception) { "Could not open output settings" }
    }
}
