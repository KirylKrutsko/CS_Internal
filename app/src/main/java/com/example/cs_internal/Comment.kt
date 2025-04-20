package com.example.cs_internal

import com.google.gson.annotations.Expose

data class Comment(
    @Expose val time : Int,
    @Expose val text : String
    // expose values are included in exports
) : Comparable<Comment>{
    override fun compareTo(other: Comment) : Int {
        return this.time - other.time // used to sort comments inside film item
    }
}
