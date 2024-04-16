package com.example.cs_internal

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException

fun SQLiteDatabase.safeQuery(tableName: String, columns: Array<String>?, selection: String?, selectionArgs: Array<String>?, groupBy: String?, having: String?, orderBy: String?): Cursor? {
    return try {
        this.query(tableName, columns, selection, selectionArgs, groupBy, having, orderBy)
    } catch (e: SQLiteException) {
        // Handle the error, you can log it or display a message to the user
        e.printStackTrace()
        null // or return an empty cursor
    }
}
