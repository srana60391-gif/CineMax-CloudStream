package com.example

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.Jsoup
import org.json.JSONObject

class ExampleProvider : MainAPI() {
    override var mainUrl = "https://archive.org"
    override var name = "CineMax"
    override var lang = "en"
    override val supportedTypes = setOf(TvType.Movie)
    override val hasMainPage = true

    private data class Film(
        val id: String,
        val title: String,
        val file: String
    )

    private val films = listOf(
        Film("his_girl_friday", "His Girl Friday", "his_girl_friday_512kb.mp4"),
        Film("sita_sings_the_blues", "Sita Sings the Blues", "SITA_SINGS_MOVIE_ONLY.mp4"),
        Film("the_pied_piper_of_hamelin", "The Pied Piper of Hamelin", "pied_512kb.mp4"),
        Film("house_on_haunted_hill", "House on Haunted Hill", "house_on_haunted_hill_512kb.mp4")
    )

    override suspend fun search(query: String): List<SearchResponse> {
        return films.filter {
            it.title.contains(query, ignoreCase = true)
        }.map {
            newMovieSearchResponse(it.title, it.id, TvType.Movie) {
                this.posterUrl = "https://archive.org/services/img/${it.id}"
            }
        }
    }

    override suspend fun load(url: String): LoadResponse? {
        val film = films.find { it.id == url } ?: return null
        val metadataUrl = "$mainUrl/metadata/${film.id}"
        val json = JSONObject(Jsoup.connect(metadataUrl).ignoreContentType(true).execute().body())
        val metadata = json.optJSONObject("metadata")
        val title = metadata?.optString("title")?.takeIf { it.isNotBlank() } ?: film.title
        val description = metadata?.optString("description") ?: ""

        return newMovieLoadResponse(
            title,
            film.id,
            TvType.Movie,
            "$mainUrl/download/${film.id}/${film.file}"
        ) {
            this.posterUrl = "$mainUrl/services/img/${film.id}"
            this.plot = description
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val film = films.find { it.id == data } ?: return false
        val videoUrl = "$mainUrl/download/${film.id}/${film.file}"

        callback(
            ExtractorLink(
                name,
                name,
                videoUrl,
                mainUrl,
                Qualities.P480,
                emptyMap(),
                null,
                ExtractorLinkType.VIDEO,
                emptyList()
            )
        )
        return true
    }
}
