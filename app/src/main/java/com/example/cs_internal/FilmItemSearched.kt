package com.example.cs_internal

import me.xdrop.fuzzywuzzy.FuzzySearch

class FilmItemSearched(filmItem: FilmItem) : FilmItem(filmItem), Comparable<FilmItemSearched> {

    private var matchValue : Double = 100.0
    fun getMatchValue() : Double {
        return matchValue
    }

    constructor(target: String, filmItem: FilmItem) : this(filmItem) {
        matchValue = calculateMatchValue(target)
    }

    private fun calculateMatchValue(target : String) : Double {
        if(target.isBlank()) return 100.0
        val words = target.split(" ")
        val mergedComments = getComments()
            .map { it.text }
            .filter { it.isNotBlank() }
            .joinToString(separator = " ")
        var titleScore = 0
        var descriptionScore = 0
        var commentaryScore = 0
        var commentsScore = 0
        var score : Int
        for (word in words) {
            score = FuzzySearch.partialRatio(word, getTitle())
            if(score > 70) titleScore += score

            if(!getDescription().isNullOrBlank()){
                score = FuzzySearch.partialRatio(word, getDescription())
                if(score > 70) descriptionScore += score
            }

            if(!getCommentary().isNullOrBlank()){
                score = FuzzySearch.partialRatio(word, getCommentary())
                if(score > 70) commentaryScore += score
            }

            if(mergedComments.isNotBlank()){
                score = FuzzySearch.partialRatio(word, mergedComments)
                if(score > 70) commentsScore += score
            }
        }

        return (0.5 * titleScore + 0.2 * descriptionScore + 0.2 * commentaryScore + 0.1 * commentsScore)
    }

    override fun compareTo(other: FilmItemSearched): Int {
        return (matchValue - other.matchValue).toInt()
    }
}