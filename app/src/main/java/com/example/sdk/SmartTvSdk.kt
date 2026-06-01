package com.example.sdk

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

class SmartTvSdk(
    private val scope: CoroutineScope
) {
    private var isConnected = false
    private var deviceId: String? = null
    private var stateChangeListener: ((String) -> Unit)? = null
    private var logListener: ((String) -> Unit)? = null

    fun connect(id: String) {
        deviceId = id
        isConnected = true
        Log.d("SmartTvSdk", "Connected to device: $id")
        log("[Socket/WS] CONNECTED to device_id: $id via wss://api.aetheros.tv/v1/connect")
        log("[REST API] GET /api/v1/device/state -> Connected to cluster: EU_WEST_2")
        notifyState("CONNECTED")
    }

    fun disconnect() {
        val id = deviceId
        isConnected = false
        deviceId = null
        log("[Socket/WS] DISCONNECTED from device_id: $id")
        notifyState("DISCONNECTED")
    }

    fun play(mediaId: String, title: String) {
        if (!isConnected) {
            log("[SDK WARN] No connected device! Auto-connecting to default device_id: tv_aether_89a")
            connect("tv_aether_89a")
        }
        log("[REST API] POST /api/v1/media/play -> Payload: {\"content_id\":\"$mediaId\", \"stream_token\":\"${generateSecureToken()}\"}")
        log("[MediaEngine] Initializing buffer stream for '$title'...")
        notifyState("VIDEO_PLAYING")
    }

    fun pause() {
        if (!isConnected) return
        log("[REST API] POST /api/v1/media/pause")
        log("[MediaEngine] Paused video feed, seeking target time locked.")
        notifyState("VIDEO_PAUSED")
    }

    fun resume() {
        if (!isConnected) return
        log("[REST API] POST /api/v1/media/play (resume)")
        log("[MediaEngine] Resumed video feed.")
        notifyState("VIDEO_PLAYING")
    }

    fun stop() {
        if (!isConnected) return
        log("[REST API] POST /api/v1/media/stop")
        log("[MediaEngine] Terminated content stream decoder.")
        notifyState("STOPPED")
    }

    fun seek(seconds: Int) {
        if (!isConnected) return
        log("[REST API] POST /api/v1/media/seek -> Payload: {\"seek_position_seconds\":$seconds}")
        log("[MediaEngine] Flushed frames, seeking to second $seconds.")
    }

    fun setVolume(level: Int) {
        if (!isConnected) return
        log("[REST API] POST /api/v1/media/volume -> Payload: {\"volume_level\":$level}")
        log("[HardwareBridge] Adjusted TDA-audio chip gain to $level%")
    }

    fun inputKey(action: String, direction: String?) {
        if (!isConnected) return
        val payload = if (direction != null) {
            "{\"action\":\"$action\", \"direction\":\"$direction\"}"
        } else {
            "{\"action\":\"$action\"}"
        }
        log("[REST API] POST /api/v1/device/input -> Payload: $payload")
        log("[HardwareBridge] Simulating remote input: $action" + (if (direction != null) " ($direction)" else ""))
    }

    fun installApp(packageName: String, appName: String) {
        log("[REST API] POST /api/v1/apps/install -> Payload: {\"package_name\":\"$packageName\"}")
        log("[AppLifecycle] App '$appName' added to installation queue. Downloading manifest and verified signature...")
    }

    fun launchApp(packageName: String, appName: String) {
        if (!isConnected) return
        log("[REST API] POST /api/v1/apps/launch -> Payload: {\"package_name\":\"$packageName\"}")
        log("[AppLifecycle] Launched native system container for '$appName'. State updated to: LAUNCHED")
        notifyState("APP_LAUNCHED_ENV:$packageName")
    }

    fun terminateApp(packageName: String, appName: String) {
        if (!isConnected) return
        log("[REST API] POST /api/v1/apps/terminate -> Payload: {\"package_name\":\"$packageName\"}")
        log("[AppLifecycle] Closed container process for '$appName'. State updated to: TERMINATED")
    }

    fun updateApp(packageName: String, appName: String) {
        log("[REST API] POST /api/v1/apps/update -> Payload: {\"package_name\":\"$packageName\"}")
        log("[AppLifecycle] Initialized background delta compilation for '$appName'. App state updated to: UPDATED")
    }

    fun triggerIoTTrigger(scene: String, triggerDescription: String) {
        log("[REST API] POST /api/v1/smarthome/trigger -> Payload: {\"scene_id\":\"$scene\", \"device_sync\":true}")
        log("[IoTBridge] Triggered automatic smart scene '$scene'. $triggerDescription")
    }

    fun onStateChange(listener: (String) -> Unit) {
        stateChangeListener = listener
    }

    fun onSdkLog(listener: (String) -> Unit) {
        logListener = listener
    }

    private fun log(message: String) {
        scope.launch(Dispatchers.Main) {
            logListener?.invoke(message)
        }
    }

    private fun notifyState(state: String) {
        scope.launch(Dispatchers.Main) {
            stateChangeListener?.invoke(state)
        }
    }

    private fun generateSecureToken(): String {
        return "streaming_tok_" + UUID.randomUUID().toString().substring(0, 8)
    }
}
