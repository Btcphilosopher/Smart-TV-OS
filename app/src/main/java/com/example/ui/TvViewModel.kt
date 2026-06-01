package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.sdk.SmartTvSdk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.random.Random

class TvViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = TvRepository(database.tvOSDao())

    // Sdk Binding
    private val sdk = SmartTvSdk(viewModelScope)

    // Data Flows from Room
    val userProfiles: StateFlow<List<UserProfile>> = repository.allProfiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val apps: StateFlow<List<SmartTVApp>> = repository.allApps
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val mediaCatalog: StateFlow<List<TvMediaItem>> = repository.allMedia
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentLogs: StateFlow<List<IoTLog>> = repository.recentLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val analyticsMetrics: StateFlow<List<AnalyticsMetric>> = repository.analytics
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun getHistory(profileId: Int): Flow<List<WatchHistory>> {
        return repository.getHistory(profileId)
    }

    // Active State variables
    val selectedProfile = MutableStateFlow<UserProfile?>(null)
    val activeMedia = MutableStateFlow<TvMediaItem?>(null)
    val playbackStatus = MutableStateFlow("STOPPED") // "STOPPED", "VIDEO_PLAYING", "VIDEO_PAUSED", "BUFFERING"
    val playbackPosition = MutableStateFlow(0)
    val activeAppPackage = MutableStateFlow<String?>(null) // null = Launcher Home Screen

    val volume = MutableStateFlow(40)
    val isMuted = MutableStateFlow(false)
    val subtitlesEnabled = MutableStateFlow(false)
    val audioTrack = MutableStateFlow("English (Dolby Atmos 7.1)")

    // Telemetry System
    val cpuTemp = MutableStateFlow(42.5)
    val ramUsedPercent = MutableStateFlow(32.0)
    val networkQuality = MutableStateFlow("Signal: Excellent (94 Mbps)")
    val bufferPercent = MutableStateFlow(100)

    // Multi-device sync states
    val syncedOnMobileDevice = MutableStateFlow(false)
    val syncStatusText = MutableStateFlow("Standalone TV Display")
    val currentRoomLightIntensity = MutableStateFlow(100) // 0 to 100%
    val windowBlindsClosed = MutableStateFlow(false)

    // Developer console logs
    private val _sdkConsoleLogs = MutableStateFlow<List<String>>(emptyList())
    val sdkConsoleLogs: StateFlow<List<String>> = _sdkConsoleLogs

    // Active Coroutine Job for stream ticker
    private var streamTickerJob: Job? = null
    // Active simulation loop job
    private var simulationLoopJob: Job? = null

    // For voice automation controller
    val voiceCommandText = MutableStateFlow("")
    val voiceFeedbackText = MutableStateFlow("")

    init {
        viewModelScope.launch(Dispatchers.IO) {
            repository.seedIfNeeded()
            // Set first profile as default
            val loadedProfiles = repository.allProfiles.first()
            if (loadedProfiles.isNotEmpty()) {
                selectedProfile.value = loadedProfiles.first()
            }
            sdk.connect("tv_aetheros_v2")
        }

        // Hook up SDK Listener
        sdk.onStateChange { state ->
            handleStateFromSdk(state)
        }

        sdk.onSdkLog { logMsg ->
            val dt = java.time.format.DateTimeFormatter.ISO_LOCAL_TIME.format(java.time.LocalTime.now()).substring(0, 8)
            _sdkConsoleLogs.value = listOf("[$dt] $logMsg") + _sdkConsoleLogs.value
        }

        // Start real-time hardware telemetry fluctuating
        startHardwareSimulation()
    }

    private fun handleStateFromSdk(state: String) {
        if (state.startsWith("APP_LAUNCHED_ENV:")) {
            val pkg = state.removePrefix("APP_LAUNCHED_ENV:")
            activeAppPackage.value = pkg
            activeMedia.value = null
            playbackStatus.value = "STOPPED"
            addLocalLog("Executed launch on TV OS layer. Container context switched to application target: $pkg", "APP")
        } else {
            when (state) {
                "VIDEO_PLAYING" -> {
                    playbackStatus.value = "VIDEO_PLAYING"
                    addLocalLog("System Renderer -> Decrypting secure buffer. Audio/Video synced. Smart Home Triggered.", "MEDIA")
                    triggerSmartHomeAction("PLAYING")
                    startPlaybackTicker()
                }
                "VIDEO_PAUSED" -> {
                    playbackStatus.value = "VIDEO_PAUSED"
                    addLocalLog("Renderer Paused -> Saved frames cached.", "MEDIA")
                    triggerSmartHomeAction("PAUSED")
                    stopPlaybackTicker()
                }
                "STOPPED" -> {
                    playbackStatus.value = "STOPPED"
                    addLocalLog("Renderer Terminated -> Display container returned to standard launcher.", "MEDIA")
                    playbackPosition.value = 0
                    triggerSmartHomeAction("STOPPED")
                    stopPlaybackTicker()
                }
                "CONNECTED" -> {
                    addLocalLog("WSS gateway protocol upgrade completed successfully.", "SOCKET")
                }
                "DISCONNECTED" -> {
                    addLocalLog("WSS channel severed cleanly.", "SOCKET")
                }
            }
        }
    }

    private fun startPlaybackTicker() {
        streamTickerJob?.cancel()
        streamTickerJob = viewModelScope.launch {
            while (playbackStatus.value == "VIDEO_PLAYING") {
                delay(1000)
                val currentMedia = activeMedia.value
                if (currentMedia != null) {
                    // Check local parental limit rules
                    val profile = selectedProfile.value
                    if (profile != null && profile.parentalControlsEnabled && profile.viewingLimitMinutes > 0) {
                        val limitSeconds = profile.viewingLimitMinutes * 60
                        if (playbackPosition.value >= limitSeconds) {
                            playbackStatus.value = "STOPPED"
                            addLocalLog("Parental limits exceeded (${profile.viewingLimitMinutes} min limit reached). Locking content stream.", "SECURITY")
                            sdk.stop()
                            voiceFeedbackText.value = "Viewing Limit Restrict Active!"
                            break
                        }
                    }

                    if (currentMedia.durationSeconds > 0 && playbackPosition.value >= currentMedia.durationSeconds) {
                        // End of stream
                        playbackPosition.value = 0
                        playbackStatus.value = "STOPPED"
                        addLocalLog("Stream reached terminal frame index. Saving completed metrics.", "MEDIA")
                        saveAnalyticsMetric(currentMedia, "COMPLETED")
                        break
                    } else {
                        playbackPosition.value += 1
                        // Periodically record watch continuation logs (e.g., every 10 seconds)
                        if (playbackPosition.value % 10 == 0) {
                            saveWatchHistory(currentMedia, playbackPosition.value)
                            saveAnalyticsMetric(currentMedia, "PARTIAL")
                        }
                    }
                } else {
                    // Live streams have no terminal duration, they just run
                    playbackPosition.value += 1
                }
            }
        }
    }

    private fun stopPlaybackTicker() {
        streamTickerJob?.cancel()
    }

    private fun startHardwareSimulation() {
        simulationLoopJob?.cancel()
        simulationLoopJob = viewModelScope.launch(Dispatchers.Default) {
            while (true) {
                delay(2000)
                // CPU temp slightly changes
                val p = Random.nextDouble(-0.8, 0.8)
                val baseCpu = if (playbackStatus.value == "VIDEO_PLAYING") 58.0 else 40.0
                cpuTemp.value = (baseCpu + Random.nextDouble(1.0, 5.0)).coerceIn(35.0, 75.0)

                // RAM fluctuated
                val baseRam = if (playbackStatus.value == "VIDEO_PLAYING") 55.0 else 28.0
                ramUsedPercent.value = (baseRam + Random.nextDouble(1.0, 3.0)).coerceIn(10.0, 95.0)

                // Network Quality
                val signalStrs = listOf(
                    "Signal: Excellent (96 Mbps)",
                    "Signal: Excellent (89 Mbps)",
                    "Signal: Good (62 Mbps)",
                    "Signal: Unstable (12 Mbps)"
                )
                val idx = if (Random.nextFloat() > 0.9) Random.nextInt(signalStrs.size) else 0
                networkQuality.value = signalStrs[idx]

                // Buffer Percent
                if (playbackStatus.value == "VIDEO_PLAYING") {
                    val currentBuf = bufferPercent.value
                    if (currentBuf < 100) {
                        bufferPercent.value = (currentBuf + Random.nextInt(5, 15)).coerceAtMost(100)
                    } else if (Random.nextFloat() > 0.8) {
                        bufferPercent.value = Random.nextInt(75, 99)
                    }
                } else {
                    bufferPercent.value = 100
                }
            }
        }
    }

    fun selectUserProfile(profile: UserProfile) {
        selectedProfile.value = profile
        addLocalLog("Context Profile Synced. Switching restrictions to: ${profile.profileName}", "SECURITY")
        // If content is currently playing and restriction is active, adjust
        if (playbackStatus.value == "VIDEO_PLAYING" && profile.parentalControlsEnabled) {
            val media = activeMedia.value
            if (media != null && media.rating == "R") {
                sdk.stop()
                addLocalLog("Parental Block: Selected profile is forbidden from playing R-Rated film '${media.title}'.", "SECURITY")
                voiceFeedbackText.value = "Access Denied: R-Rated restrictions apply."
            }
        }
    }

    fun toggleMute() {
        isMuted.value = !isMuted.value
        addLocalLog("Adjusted system volume state to: ${if (isMuted.value) "MUTED" else "UNMUTED"}", "HARDWARE")
    }

    fun adjustVolume(increase: Boolean) {
        val current = volume.value
        val next = if (increase) (current + 5).coerceAtMost(100) else (current - 5).coerceAtLeast(0)
        volume.value = next
        sdk.setVolume(next)
    }

    fun playMedia(media: TvMediaItem) {
        val currentProfile = selectedProfile.value
        if (currentProfile != null && currentProfile.parentalControlsEnabled && media.rating == "R") {
            addLocalLog("Access Blocked: Parental Locks prevent execution of ${media.title} (${media.rating}).", "SECURITY")
            voiceFeedbackText.value = "Denied: R rating locked."
            return
        }

        activeAppPackage.value = null
        activeMedia.value = media
        playbackPosition.value = 0
        playbackStatus.value = "BUFFERING"
        
        viewModelScope.launch {
            delay(800) // simulate buffer delay
            sdk.play(media.id, media.title)
        }
    }

    fun stopPlayback() {
        sdk.stop()
    }

    fun pausePlayback() {
        sdk.pause()
    }

    fun resumePlayback() {
        sdk.resume()
    }

    fun seekMedia(seconds: Int) {
        val totalSecs = activeMedia.value?.durationSeconds ?: 3600
        val target = (playbackPosition.value + seconds).coerceIn(0, totalSecs)
        playbackPosition.value = target
        sdk.seek(target)
    }

    fun sendRemoteInput(action: String, direction: String? = null) {
        sdk.inputKey(action, direction)
        // Adjust state if action maps directly
        if (action == "HOME") {
            activeAppPackage.value = null
            activeMedia.value = null
            playbackStatus.value = "STOPPED"
        } else if (action == "BACK") {
            if (playbackStatus.value != "STOPPED") {
                sdk.stop()
            } else if (activeAppPackage.value != null) {
                activeAppPackage.value = null
            }
        }
    }

    fun installNewApp(pkg: String, name: String) {
        viewModelScope.launch {
            sdk.installApp(pkg, name)
            delay(1500)
            val app = SmartTVApp(
                packageName = pkg,
                appName = name,
                iconName = "default_ic",
                installState = "installed",
                version = "1.0.0",
                sizeMb = Random.nextDouble(15.0, 60.0)
            )
            repository.insertApp(app)
            addLocalLog("App sandbox package compilation complete. App '$name' registered on hardware layer.", "APP")
        }
    }

    fun uninstallApp(pkg: String, name: String) {
        viewModelScope.launch {
            sdk.terminateApp(pkg, name)
            repository.deleteApp(pkg)
            if (activeAppPackage.value == pkg) {
                activeAppPackage.value = null
            }
            addLocalLog("Purged app directories cleanly. Package '$pkg' uninstalled.", "APP")
        }
    }

    fun updateApp(pkg: String, name: String) {
        viewModelScope.launch {
            sdk.updateApp(pkg, name)
            repository.updateAppInstallState(pkg, "updated")
            addLocalLog("Patch delta compilation complete for '$name'. System processes updated.", "APP")
        }
    }

    fun launchAppPkg(app: SmartTVApp) {
        sdk.launchApp(app.packageName, app.appName)
    }

    // Smart Home IoT Controller integrations
    private fun triggerSmartHomeAction(tvState: String) {
        viewModelScope.launch {
            when (tvState) {
                "PLAYING" -> {
                    currentRoomLightIntensity.value = 15
                    windowBlindsClosed.value = true
                    sdk.triggerIoTTrigger("CINEMA_THEATER_MODE", "Dimmed overhead smart light intensity to 15%, initiated vertical window blind closing rotation.")
                    val log = IoTLog(logMessage = "Cinema Setup: Overhead lighting dimmed to 15%. Windows closed.", actionType = "LIGHTING")
                    repository.insertLog(log)
                }
                "PAUSED" -> {
                    currentRoomLightIntensity.value = 60
                    sdk.triggerIoTTrigger("AMBIENT_STANDBY", "Increased overhead smart light intensity to 60% for safety.")
                    val log = IoTLog(logMessage = "Standby Scene: Overhead lighting warm raised to 60%. Informational.", actionType = "LIGHTING")
                    repository.insertLog(log)
                }
                "STOPPED" -> {
                    currentRoomLightIntensity.value = 100
                    windowBlindsClosed.value = false
                    sdk.triggerIoTTrigger("LIVING_ROOM_NORMAL", "Overhead lights brightened to 100%, vertical blinds opened.")
                    val log = IoTLog(logMessage = "Welcome Home: Dimmer raised to 100%. Window shutters opened.", actionType = "LIGHTING")
                    repository.insertLog(log)
                }
            }
        }
    }

    fun customIoTTrigger(actionType: String, actionMsg: String) {
        viewModelScope.launch {
            val log = IoTLog(logMessage = actionMsg, actionType = actionType)
            repository.insertLog(log)
            addLocalLog("Smart Home IoT hook: $actionMsg", "IOT")
        }
    }

    fun clearAllIoTLogs() {
        viewModelScope.launch {
            repository.clearLogs()
        }
    }

    // Sync state
    fun toggleDeviceSync() {
        val current = syncedOnMobileDevice.value
        syncedOnMobileDevice.value = !current
        if (!current) {
            syncStatusText.value = "Synced with Mobile UI-Hub"
            addLocalLog("Handoff established: Peer connection open. Dynamic token synchronizer verified.", "SOCKET")
        } else {
            syncStatusText.value = "Standalone TV Display"
            addLocalLog("Handoff channel severed cleanly. State storage locked to TV hardware.", "SOCKET")
        }
    }

    fun syncCurrentPlaybackToDevice() {
        val media = activeMedia.value
        if (media == null) {
            addLocalLog("Sync Failed: Navigation Layer is not active on a movie or TV track.", "SOCKET")
            return
        }
        addLocalLog("Continuous Watch Sync: Cast request successful. Dispatched stream handoff packet containing media_id: ${media.id} and offset: ${playbackPosition.value}s.", "SOCKET")
    }

    // Helper logging systems
    private fun addLocalLog(message: String, category: String) {
        viewModelScope.launch {
            val log = IoTLog(logMessage = "[$category] $message", actionType = category)
            repository.insertLog(log)
        }
    }

    private suspend fun saveWatchHistory(media: TvMediaItem, durationSpent: Int) {
        val profile = selectedProfile.value ?: return
        val currentHistory = WatchHistory(
            profileId = profile.id,
            mediaId = media.id,
            mediaTitle = media.title,
            watchedDurationSeconds = durationSpent
        )
        repository.insertHistory(currentHistory)
    }

    private suspend fun saveAnalyticsMetric(media: TvMediaItem, status: String) {
        val metric = AnalyticsMetric(
            mediaId = media.id,
            mediaTitle = media.title,
            viewDurationSeconds = playbackPosition.value,
            completionStatus = status,
            deviceSessionId = "session_" + UUID.randomUUID().toString().take(6)
        )
        repository.insertMetric(metric)
    }

    fun clearAllHistory() {
        val profile = selectedProfile.value ?: return
        viewModelScope.launch {
            repository.clearHistory(profile.id)
            addLocalLog("History purged for profile id ${profile.id}.", "SECURITY")
        }
    }

    // Voice & Command Compiler AI Layer
    fun compileVoiceCommand(command: String) {
        voiceCommandText.value = command
        viewLifecycleOwnerVoiceExecution(command)
    }

    private fun viewLifecycleOwnerVoiceExecution(cmd: String) {
        val cleanCmd = cmd.trim().lowercase()
        viewModelScope.launch {
            addLocalLog("Voice Engine processing command packet: '$cmd'", "VOICE")
            delay(400) // slight AI compilation latency simulation

            when {
                cleanCmd.contains("play") -> {
                    // Try to match a media title
                    val allM = repository.allMedia.first()
                    val target = allM.find { mediaItem ->
                        cleanCmd.contains(mediaItem.title.lowercase()) || mediaItem.title.lowercase().contains(cleanCmd.replace("play ", ""))
                    }
                    if (target != null) {
                        playMedia(target)
                        voiceFeedbackText.value = "Executing: Playing '${target.title}'"
                    } else {
                        // Play first default available item
                        if (allM.isNotEmpty()) {
                            playMedia(allM.first())
                            voiceFeedbackText.value = "Match not found. Streaming default: '${allM.first().title}'"
                        } else {
                            voiceFeedbackText.value = "Failed: Channel database is unpopulated."
                        }
                    }
                }
                cleanCmd.contains("pause") || cleanCmd.contains("freeze") -> {
                    sdk.pause()
                    voiceFeedbackText.value = "Executing command: Pausing media stream."
                }
                cleanCmd.contains("resume") || cleanCmd.contains("continue") -> {
                    sdk.resume()
                    voiceFeedbackText.value = "Executing command: Streaming resumed."
                }
                cleanCmd.contains("stop") || cleanCmd.contains("terminate") -> {
                    sdk.stop()
                    voiceFeedbackText.value = "Executing command: Terminal state activated."
                }
                cleanCmd.contains("mute") -> {
                    toggleMute()
                    voiceFeedbackText.value = "Volume Muted successfully."
                }
                cleanCmd.contains("volume") -> {
                    val numbers = """\d+""".toRegex().find(cleanCmd)?.value?.toIntOrNull()
                    if (numbers != null) {
                        val boundedNum = numbers.coerceIn(0, 100)
                        volume.value = boundedNum
                        sdk.setVolume(boundedNum)
                        voiceFeedbackText.value = "Volume matched directly to: $boundedNum%"
                    } else {
                        adjustVolume(increase = true)
                        voiceFeedbackText.value = "Step volume level up compiled."
                    }
                }
                cleanCmd.contains("launch") || cleanCmd.contains("open") -> {
                    val allApps = repository.allApps.first()
                    val targetApp = allApps.find { app ->
                        cleanCmd.contains(app.appName.lowercase()) || app.appName.lowercase().contains(cleanCmd.replace("launch ", "").replace("open ", ""))
                    }
                    if (targetApp != null) {
                        launchAppPkg(targetApp)
                        voiceFeedbackText.value = "Sandbox open request: Launching '${targetApp.appName}'"
                    } else {
                        voiceFeedbackText.value = "App matching signature was not found in register."
                    }
                }
                cleanCmd.contains("profile") || cleanCmd.contains("user") -> {
                    val allProfiles = repository.allProfiles.first()
                    val matchedProfile = allProfiles.find { prof ->
                        cleanCmd.contains(prof.profileName.lowercase()) || prof.profileName.lowercase().contains(cleanCmd.replace("profile ", ""))
                    }
                    if (matchedProfile != null) {
                        selectUserProfile(matchedProfile)
                        voiceFeedbackText.value = "Context switched to: ${matchedProfile.profileName}"
                    } else {
                        voiceFeedbackText.value = "No corresponding user profile detected."
                    }
                }
                else -> {
                    voiceFeedbackText.value = "AI Syntax Unrecognized. Try 'Play Aether', 'Launch YouTube', or 'Volume 75'"
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        simulationLoopJob?.cancel()
        streamTickerJob?.cancel()
    }
}
