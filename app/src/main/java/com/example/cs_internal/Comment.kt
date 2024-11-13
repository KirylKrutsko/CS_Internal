package com.example.cs_internal

import com.google.gson.annotations.Expose

data class Comment(
    @Expose val time : Int,
    @Expose val text : String
) : Comparable<Comment>{
    override fun compareTo(other: Comment) : Int {
        return this.time - other.time
    }
}
