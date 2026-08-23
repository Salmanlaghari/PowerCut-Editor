package com.powercut.editor.data

import java.util.List
import java.util.UUID

// Data structure for downloadable items
data class DownloadItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val category: String, // "Effects", "Transitions", "Templates"
    val sizeMb: Int,
    val downloadUrl: String,
    val md5Hash: String
)

// Manifest containing all available items
object DownloadManifest {
    const val BASE_URL = "https://github.com/Salmanlaghari/PowerCut-Editor/releases/latest"

    init {
        // This would fetch from GitHub Releases API in real implementation
        val items = listOf(
            // Example Effects entries
            DownloadItem(
                id = "blur_effect",
                name = "Motion Blur",
                category = "Effects",
                sizeMb = 5,
                downloadUrl = "${BASE_URL}/effects/blur.zip",
                md5Hash = "a1b2c3d4e5f67890"
            ),
            // Example Transition entries
            DownloadItem(
                id = "heart_mask",
                name = "Heart Reveal",
                category = "Transitions",
                sizeMb = 2,
                downloadUrl = "${BASE_URL}/transitions/heart.zip",
                md5Hash = "b2c3d4e5f6a17890"
            ),
            // Example Template entries
            DownloadItem(
                id = "tiktok_template",
                name = "TikTok Trending",
                category = "Templates",
                sizeMb = 10,
                downloadUrl = "${BASE_URL}/templates/tiktok.zip",
                md5Hash = "c3d4e5f6a1b29078"
            )
        )
    }

    fun getAllItems(): List<DownloadItem> = items

    // In real implementation: fetch from GitHub releases using HttpURLConnection
    private fun fetchManifestFromGitHub(): List<DownloadItem> {
        // Simulated fetch for now
        return items
    }
}