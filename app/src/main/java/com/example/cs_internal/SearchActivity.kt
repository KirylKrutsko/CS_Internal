package com.example.cs_internal

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.HorizontalScrollView
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.view.children
import com.example.cs_internal.DataSingleton.TMDB_API_KEY
import com.example.cs_internal.DataSingleton.filmLists
import com.example.cs_internal.DataSingleton.tags
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SearchActivity : AppCompatActivity() {

    private lateinit var recyclerViewAdapter: SearchRecyclerViewAdapter
    private lateinit var searchView : SearchView
    private lateinit var sortLayout: LinearLayout
    private var init = false
    private lateinit var checkResultsText : TextView
    private var orderBy = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        val authenticator = FirebaseAuth.getInstance()
        val user = authenticator.currentUser
        if(user == null){
            val intentToLoginActivity = Intent(this, LoginActivity::class.java)
            startActivity(intentToLoginActivity)
            finish()
        }
        if(!user!!.isEmailVerified) {
            val intent = Intent(this, EmailVerificationActivity::class.java)
            startActivity(intent)
            finish()
        }
        val databaseUserRef = Firebase.database.reference.child("users/${user.uid}")
        val storageUserRef = Firebase.storage.reference.child("users/${user.uid}")

        val toolbar : Toolbar = findViewById(R.id.searchToolbar)
        toolbar.title = user.displayName
        setSupportActionBar(toolbar)

        val allFilmList : MutableList<FilmItem> = mutableListOf()
        allFilmList.addAll(filmLists[0])
        allFilmList.addAll(filmLists[1])
        allFilmList.addAll(filmLists[2])
        allFilmList.sortByDescending { it.getLastEditTime() }
        val recyclerView : CustomRecyclerView = findViewById(R.id.recyclerView)
        recyclerViewAdapter = SearchRecyclerViewAdapter(storageUserRef, this@SearchActivity, allFilmList)
        recyclerView.adapter = recyclerViewAdapter
        recyclerView.setButton(findViewById(R.id.scrollUpButton))

        toolbar.setNavigationOnClickListener {
            finish()
        }
        checkResultsText = findViewById(R.id.checkResultsText)
        sortLayout = findViewById(R.id.sortLayout)

        val sortLayout : LinearLayout = findViewById(R.id.sortLayout)
        val tagSortLayout : SortLinearLayout = sortLayout.findViewById(R.id.tagLinearLayout)
        val markSortLayout : SortLinearLayout = sortLayout.findViewById(R.id.markSortLayout)
        for(tag in tags){
            addTag(tag, tagSortLayout)
        }
        tagSortLayout.set(recyclerViewAdapter, true)
        markSortLayout.set(recyclerViewAdapter, false)

        val toSortTag = intent.getStringExtra("tagName")
        val tagNum = DataSingleton.tags.indexOf(toSortTag)
        if(tagNum in 0 until tagSortLayout.childCount){
            sortLayout.visibility = VISIBLE
            tagSortLayout.getChildAt(tagNum).performClick()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.search_toolbar_menu, menu)
        val searchItem = menu.findItem(R.id.action_search)
        searchView = searchItem.actionView as SearchView
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(query: String?): Boolean {
                searchView.clearFocus()
                if(query != null && query.isNotBlank()){
                    searchForMoviesInDatabase(query.trim())
                }
                return true
            }
            override fun onQueryTextChange(newText: String?): Boolean {
                recyclerViewAdapter.filterByTarget(newText.orEmpty().trim())
                return true
            }
        })
        init = true
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.order_by -> {
                setOrderByDialog()
            }
            R.id.action_database_search -> {
                val intent = Intent(this@SearchActivity, TmdbSearchActivity::class.java)
                startActivity(intent)
            }
            R.id.action_tag_sort -> {
                if(sortLayout.visibility == GONE){
                    sortLayout.visibility = VISIBLE
                }
                else if(sortLayout.visibility == VISIBLE){
                    sortLayout.visibility = GONE
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun addTag(name: String, tagScrollLayout: LinearLayout){
        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val itemView = inflater.inflate(R.layout.tag_list_item, null)
        val itemTextView : TextView = itemView.findViewById(R.id.textView)
        itemTextView.text = name
        itemView.findViewById<ImageButton>(R.id.imageButton).isClickable = false
        tagScrollLayout.addView(itemView, tagScrollLayout.childCount)
    }
    private fun setOrderByDialog(){
        var selectedOrder = orderBy
        val optionsArray = arrayOf(
            "edit time : newest first",
            "edit time : oldest first",
            "created time : newest first",
            "created time : oldest first",
            "alphabetical : A to Z",
            "alphabetical : Z to A"
        )
        val dialog = AlertDialog.Builder(this)
            .setTitle("Order by")
            .setSingleChoiceItems(optionsArray, selectedOrder){ _,item ->
                selectedOrder = item
            }
            .setPositiveButton("Apply"){_,_->
                recyclerViewAdapter.order(optionsArray[selectedOrder])
                orderBy = selectedOrder
            }
            .setNeutralButton("Cancel"){_,_->}
            .create()
        dialog.show()
    }
    fun searchForMoviesInDatabase(query : String){
        TmdbRetrofitClient.instance.searchMovies(TMDB_API_KEY, query).enqueue(object : Callback<TmdbSearchResponse> {
            override fun onResponse(call: Call<TmdbSearchResponse>, response: Response<TmdbSearchResponse>){
                val items = response.body()?.results ?: listOf()
                if(items.isEmpty()){
                    Toast.makeText(this@SearchActivity, "No results found", Toast.LENGTH_SHORT).show()
                }
                else{
                    checkResultsText.text = "${items.size} results found in the database.\nClick here to review"
                    checkResultsText.visibility = VISIBLE
                    checkResultsText.setOnClickListener {
                        checkResultsText.visibility = GONE
                        val intent = Intent(this@SearchActivity, TmdbSearchActivity::class.java)
                            .putExtra("response", response.body())
                            .putExtra("query", query)
                        startActivity(intent)
                    }
                }
            }
            override fun onFailure(call: Call<TmdbSearchResponse>, t: Throwable) {
                Toast.makeText(this@SearchActivity, "Search failed", Toast.LENGTH_SHORT).show()
                Toast.makeText(this@SearchActivity, t.message, Toast.LENGTH_LONG).show()
            }
        })
    }

}