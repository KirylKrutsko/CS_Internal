package com.example.cs_internal

import java.lang.Exception

interface DatabaseSyncListener {
    fun onSuccess()
    fun onFailure(exception: Exception)
}