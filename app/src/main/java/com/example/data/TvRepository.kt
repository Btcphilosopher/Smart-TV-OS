package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.first

class TvRepository(private val dao: TvOSDao) {

    val allProfiles: Flow<List<UserProfile>> = dao.getAllProfiles()
    val allApps: Flow<List<SmartTVApp>> = dao.getAllApps()
    val allMedia: Flow<List<TvMediaItem>> = dao.getAllMedia()
    val recentLogs: Flow<List<IoTLog>> = dao.getRecentLogs()
    val analytics: Flow<List<AnalyticsMetric>> = dao.getAnalytics()

    fun getHistory(profileId: Int): Flow<List<WatchHistory>> = dao.getHistoryByProfile(profileId)

    suspend fun insertProfile(profile: UserProfile) = dao.insertProfile(profile)
    suspend fun deleteProfile(profile: UserProfile) = dao.deleteProfile(profile)

    suspend fun insertApp(app: SmartTVApp) = dao.insertApp(app)
    suspend fun updateAppInstallState(packageName: String, state: String) = dao.updateAppInstallState(packageName, state)
    suspend fun deleteApp(packageName: String) = dao.deleteApp(packageName)

    suspend fun insertLog(log: IoTLog) = dao.insertLog(log)
    suspend fun clearLogs() = dao.clearLogs()

    suspend fun insertMetric(metric: AnalyticsMetric) = dao.insertMetric(metric)
    suspend fun insertHistory(history: WatchHistory) = dao.insertHistory(history)
    suspend fun clearHistory(profileId: Int) = dao.clearHistoryForProfile(profileId)

    suspend fun seedIfNeeded() {
        // Seeding profiles
        val profiles = allProfiles.first()
        if (profiles.isEmpty()) {
            dao.insertProfile(UserProfile(
                profileName = "Primary Account (Admin)",
                avatarColorHex = "#3B82F6", // Brilliant Blue
                parentalControlsEnabled = false,
                viewingLimitMinutes = 0,
                preferredGenre = "Sci-Fi"
            ))
            dao.insertProfile(UserProfile(
                profileName = "Junior Playroom (Kids)",
                avatarColorHex = "#10B981", // Emerald Green
                parentalControlsEnabled = true,
                viewingLimitMinutes = 45,
                preferredGenre = "Animation"
            ))
            dao.insertProfile(UserProfile(
                profileName = "Night Owl (Guest)",
                avatarColorHex = "#8B5CF6", // Lavender Purple
                parentalControlsEnabled = false,
                viewingLimitMinutes = 120,
                preferredGenre = "Thriller"
            ))
        }

        // Seeding apps
        val apps = allApps.first()
        if (apps.isEmpty()) {
            val initialApps = listOf(
                SmartTVApp("com.netflix.tv", "Netflix HD", "netflix", "installed", "10.4.2", 52.3),
                SmartTVApp("com.youtube.tv", "YouTube TV", "youtube", "installed", "4.15.1", 38.0),
                SmartTVApp("com.spotify.tv", "Spotify Stereo", "spotify", "installed", "2.1.0", 31.5),
                SmartTVApp("com.disney.tv", "Disney+ Cinema", "disney", "installed", "6.3.1", 44.2),
                SmartTVApp("com.twitch.tv", "Twitch Live", "twitch", "installed", "9.8.0", 28.1),
                SmartTVApp("com.crunchyroll.tv", "Crunchyroll Anime", "crunchyroll", "installed", "3.2.1", 35.6)
            )
            for (app in initialApps) {
                dao.insertApp(app)
            }
        }

        // Seeding media items
        val media = allMedia.first()
        if (media.isEmpty()) {
            val initialMedia = listOf(
                TvMediaItem(
                    id = "movie_1",
                    title = "Aether: Infinite Space",
                    category = "Movie",
                    genre = "Sci-Fi",
                    streamUrl = "https://interactive-media.com/stream/aether_infinite.mp4",
                    durationSeconds = 7200, // 2 Hours
                    rating = "PG-13",
                    description = "A deep-space expedition ventures beyond the event horizon to unlock the mysteries of quantum synchronization.",
                    thumbnailUrl = ""
                ),
                TvMediaItem(
                    id = "movie_2",
                    title = "Chronos Paradigm",
                    category = "Movie",
                    genre = "Thriller",
                    streamUrl = "https://interactive-media.com/stream/chronos.mp4",
                    durationSeconds = 6400,
                    rating = "R",
                    description = "When a researcher hacks into a Smart TV feedback loop, she discovers a television protocol capable of rewinding time by 100 milliseconds.",
                    thumbnailUrl = ""
                ),
                TvMediaItem(
                    id = "show_1",
                    title = "The Hackathon Diaries",
                    category = "TV Show",
                    genre = "Comedy",
                    streamUrl = "https://interactive-media.com/stream/diaries_s1.mp4",
                    durationSeconds = 1500, // 25 Min
                    rating = "TV-PG",
                    description = "Five backend architects try to solve simulated TV latency constraints on three caffeine and cold pizza slices.",
                    thumbnailUrl = ""
                ),
                TvMediaItem(
                    id = "show_2",
                    title = "Pixel Odyssey",
                    category = "TV Show",
                    genre = "Animation",
                    streamUrl = "https://interactive-media.com/stream/pixels.mp4",
                    durationSeconds = 1200, // 20 Min
                    rating = "TV-Y7",
                    description = "An adaptive graphics processor accidentally develops self-awareness and sets out on an odyssey through various TV protocols.",
                    thumbnailUrl = ""
                ),
                TvMediaItem(
                    id = "live_1",
                    title = "Aether News 24/7",
                    category = "Live Channel",
                    genre = "News",
                    streamUrl = "https://live-broadcast.com/hls/aether_news.m3u8",
                    durationSeconds = 0, // Infinite
                    rating = "G",
                    description = "Real-time updates regarding internet networks, global embedded software standards, and API engineering breakthroughs.",
                    thumbnailUrl = ""
                ),
                TvMediaItem(
                    id = "live_2",
                    title = "Lofi Retro TV Beats",
                    category = "Live Channel",
                    genre = "Music",
                    streamUrl = "https://live-broadcast.com/hls/lofi_retro.m3u8",
                    durationSeconds = 0,
                    rating = "G",
                    description = "Relaxing, ambient synthesizers playing continuously for code production, design planning, and late night focus states.",
                    thumbnailUrl = ""
                ),
                TvMediaItem(
                    id = "playlist_1",
                    title = "Cyberpunk Essentials Bundle",
                    category = "Playlist",
                    genre = "Sci-Fi",
                    streamUrl = "https://interactive-media.com/bundle/cyberpunk.m3u8",
                    durationSeconds = 9600,
                    rating = "R",
                    description = "Hand-picked compilations of cinematic clips, synthwave beats, and embedded software logs.",
                    thumbnailUrl = ""
                )
            )
            dao.insertMedia(initialMedia)
        }
    }
}
