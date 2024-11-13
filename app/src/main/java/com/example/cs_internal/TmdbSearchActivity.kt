package com.example.cs_internal

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageButton
import android.widget.SearchView
import android.widget.Toast
import com.example.cs_internal.DataSingleton.TMDB_API_KEY
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class TmdbSearchActivity : AppCompatActivity() {

    private lateinit var adapter : TmdbSearchRecyclerViewAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tmdb_search)
        setResult(RESULT_OK)

        val query = intent.getStringExtra("query")
        val response = intent.getSerializableExtra("response") as TmdbSearchResponse?
        val toAddPage = intent.getIntExtra("pageNum", 0)
        val recyclerView : CustomRecyclerView = findViewById(R.id.recyclerView)
        adapter = TmdbSearchRecyclerViewAdapter(response?.results?.toMutableList() ?: mutableListOf(), this, toAddPage)
        recyclerView.adapter = adapter
        recyclerView.setButton(findViewById(R.id.scrollUpButton))

        val searchView : SearchView = findViewById(R.id.searchView)
        searchView.setQuery(query, false)
        searchView.requestFocus()
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(query: String?): Boolean {
                searchView.clearFocus()
                if(!query.isNullOrEmpty()) searchInDatabase(query)
                return true
            }
            override fun onQueryTextChange(newText: String?): Boolean { return false }
        })

        val backButton : ImageButton = findViewById(R.id.backButton)
        backButton.setOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }
    }

    private fun searchInDatabase(query: String) {
        TmdbRetrofitClient.instance.searchMovies(TMDB_API_KEY, query).enqueue(object : Callback<TmdbSearchResponse> {
            override fun onResponse(call: Call<TmdbSearchResponse>, response: Response<TmdbSearchResponse>){
                val items = response.body()?.results ?: listOf()
                if(items.isEmpty()){
                    Toast.makeText(this@TmdbSearchActivity, "No results found", Toast.LENGTH_SHORT).show()
                }
                else{
                    adapter.changeList(items.toMutableList())
                }
            }
            override fun onFailure(call: Call<TmdbSearchResponse>, t: Throwable) {
                Toast.makeText(this@TmdbSearchActivity, "Search failed", Toast.LENGTH_SHORT).show()
                Toast.makeText(this@TmdbSearchActivity, t.message, Toast.LENGTH_LONG).show()
            }
        })
    }

    override fun onPause() {
        super.onPause()
        finish()
    }
}