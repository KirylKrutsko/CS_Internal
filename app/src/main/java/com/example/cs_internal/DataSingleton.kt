package com.example.cs_internal

object DataSingleton {
    const val TMDB_API_KEY = "fab125b41069e420843edff6259c5d60"
    val adapters : MutableList<MainRecyclerViewAdapter> = mutableListOf()
    val filmLists : List<MutableList<FilmItem>> = listOf(mutableListOf(), mutableListOf(), mutableListOf())
    val tags : MutableList<String> = mutableListOf()
}
