package com.example.cs_internal

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.*
import androidx.appcompat.widget.Toolbar
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.SearchView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.viewpager.widget.ViewPager
import com.example.cs_internal.DataSingleton.adapters
import com.example.cs_internal.DataSingleton.filmLists
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage

class MainActivity : AppCompatActivity(){
    private lateinit var searchView : SearchView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var addButton : ImageButton
    private var init = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        requestForPermissions()
        setSupportActionBar(findViewById(R.id.toolbar))

        val authenticator = FirebaseAuth.getInstance()
        val user = authenticator.currentUser

        if(user == null){
            val intentToLoginActivity = Intent(this@MainActivity, LoginActivity::class.java)
            startActivity(intentToLoginActivity)
            finish()
        }
        if(!user!!.isEmailVerified) {
            val intent = Intent(this, EmailVerificationActivity::class.java)
            startActivity(intent)
            finish()
        }

        supportActionBar?.title = user.displayName

        val signOutButton : Button = findViewById(R.id.buttonSignOut)

        signOutButton.setOnClickListener(){
            authenticator.signOut()
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }

        val databaseUserRef : DatabaseReference = Firebase.database.reference.child("users/${user.uid}")
        val storageUserRef : StorageReference = Firebase.storage.reference.child("users/${user.uid}")

        val viewPager : ViewPager = findViewById(R.id.viewPager)
        viewPager.offscreenPageLimit = 2
        val pagerAdapter = PagerAdapter(supportFragmentManager, databaseUserRef, storageUserRef,this)
        pagerAdapter.setAdapters()
        viewPager.adapter = pagerAdapter
        val tabLayout : TabLayout = findViewById(R.id.tabLayout)
        tabLayout.setupWithViewPager(viewPager)

        tabLayout.getTabAt(0)?.setIcon(R.drawable.outline_watch_later_24)
        tabLayout.getTabAt(1)?.setIcon(R.drawable.outline_remove_red_eye_24)
        tabLayout.getTabAt(2)?.setIcon(R.drawable.baseline_done_all_24)

        swipeRefresh = findViewById(R.id.swipeRefresh)
        swipeRefresh.setDistanceToTriggerSync(800)
        updateFilms(databaseUserRef)
        swipeRefresh.setOnRefreshListener {
//                Toast.makeText(this, "", Toast.LENGTH_SHORT).show()
                updateFilms(databaseUserRef)
        }

        addButton = findViewById(R.id.add_button)
        addButton.setOnClickListener(){
            val currentPage = tabLayout.selectedTabPosition
            val intent = Intent(this@MainActivity, FilmActivity::class.java)
            intent.apply {
                putExtra("new", true)
                putExtra("pageNum", currentPage)
            }
            startActivity(intent)
        }

        val toolbar : Toolbar = findViewById(R.id.toolbar)
        toolbar.setNavigationOnClickListener {
            val intent = Intent(this@MainActivity, ProfileActivity::class.java)
            startActivity(intent)
        }
    }

    private fun requestForPermissions() {
        val requestPermissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {}
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            requestPermissions.launch(arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
            ))
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions.launch(arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES
            ))
        } else {
            requestPermissions.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE))
        }
    }

    private fun updateFilms(databaseUserRef : DatabaseReference) {
        swipeRefresh.isRefreshing = true
        for(i in 0 until 3) {
            adapters[i].resetList()
            val size = filmLists[i].size
            filmLists[i].clear()
            adapters[i].notifyItemRangeRemoved(0, size)
        }
        val toRetrieveRef = databaseUserRef.orderByChild("lastEditTime")
        toRetrieveRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (filmSnapshot in dataSnapshot.children) {
                    val ref = filmSnapshot.key
                    if(ref == null){
                        Toast.makeText(this@MainActivity, "Failed to download some changes.\nKey is null", Toast.LENGTH_SHORT).show()
                    } else {
                        val title = filmSnapshot.child("title").getValue(String::class.java) ?: ""
                        val type = filmSnapshot.child("type").getValue(Int::class.java) ?: 0
                        val desc = filmSnapshot.child("desc").getValue(String::class.java)
                        val rating = filmSnapshot.child("rating").getValue(String::class.java)
                        val imageLink = filmSnapshot.child("imageLink").getValue(String::class.java)
                        val comm = filmSnapshot.child("commentary").getValue(String::class.java)
                        val link = filmSnapshot.child("link").getValue(String::class.java)
                        val newFilm = FilmItem(title, type, ref, desc, rating, comm, imageLink, link)
                        filmSnapshot.child("tags").children.forEach { tagSnapshot->
                            val tag = tagSnapshot.getValue(String::class.java) ?: ""
                            newFilm.addTag(tag)
                            DataSingleton.tags.add(tag)
                        }
                        filmLists[type].add(0, newFilm)
                        adapters[type].notifyItemInserted(0)
                    }
                }
                swipeRefresh.isRefreshing = false
            }
            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(this@MainActivity, "Database connection error", Toast.LENGTH_SHORT).show()
                swipeRefresh.isRefreshing = false
            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        val searchItem = menu.findItem(R.id.action_search)
        searchView = searchItem?.actionView as SearchView
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(query: String?): Boolean {
                searchView.clearFocus()
                return true
            }
            override fun onQueryTextChange(newText: String?): Boolean {
                applyFilter(newText)
                return true
            }
        })

        searchItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                addButton.visibility = GONE
                swipeRefresh.isEnabled = false
                return true
            }
            override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                addButton.visibility = VISIBLE
                for(i in 0 until 3){
                    adapters[i].resetList()
                }
                swipeRefresh.isEnabled = true
                return true
            }
        })
        init = true
        return true
    }

    override fun onResume() {
        super.onResume()
        if(init){
            for(i in 0 until 3){
                adapters[i].resetList()
            }
            if(searchView.query.isNotEmpty()){
                applyFilter(searchView.query.toString())
            }
        }
    }


    private fun applyFilter(newText : String?){
        val target = newText.orEmpty().trim()
        for(i in 0 until 3){
            val filteredList = filmLists[i].mapNotNull { filmItem ->
                filmItem.searchAndSpan(target)
            }.toMutableList()
            adapters[i].setFilteredList(filteredList)
        }
    }

}

