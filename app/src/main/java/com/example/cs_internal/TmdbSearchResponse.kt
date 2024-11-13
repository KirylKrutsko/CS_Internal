package com.example.cs_internal

import java.io.Serializable

data class TmdbSearchResponse (val results : List<TmdbItem>) : Serializable {
    data class TmdbItem (
        val id: Int,
        val title: String,
        val overview: String,
        val release_date: String,
        val poster_path: String?,
        val original_language : String
    ) : Serializable
}
