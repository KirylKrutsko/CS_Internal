package com.example.cs_internal

object DataSingleton {
    val adapters : MutableList<RecyclerViewAdapter> = mutableListOf()
    val filmLists : List<MutableList<FilmItem>> = listOf(mutableListOf(), mutableListOf(), mutableListOf())
    val tags : MutableSet<String> = mutableSetOf()
}
