package com.example.cs_internal

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.viewpager.widget.ViewPager
import com.example.cs_internal.DataSingleton.filmLists
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.lang.Exception

class MainActivity : AppCompatActivity() {
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

        val viewPager : ViewPager = findViewById(R.id.viewPager)
        val pagerAdapter = PagerAdapter(supportFragmentManager, databaseUserRef, this)
        pagerAdapter.setAdapters()
        viewPager.adapter = pagerAdapter
        val tabLayout : TabLayout = findViewById(R.id.tabLayout)
        tabLayout.setupWithViewPager(viewPager)

        tabLayout.getTabAt(0)?.setIcon(R.drawable.outline_watch_later_24)
        tabLayout.getTabAt(1)?.setIcon(R.drawable.outline_remove_red_eye_24)
        tabLayout.getTabAt(2)?.setIcon(R.drawable.baseline_done_all_24)

        updateFilms(databaseUserRef, filmLists, viewPager)
        val refreshButton : Button = findViewById(R.id.refresh)
        refreshButton.setOnClickListener(){
            updateFilms(databaseUserRef, filmLists, viewPager)
        }

        val addButton : ImageButton = findViewById(R.id.add_button)
        val addFilmLayout : ConstraintLayout = findViewById(R.id.add_film_layout)
        addButton.setOnClickListener(){
            refreshButton.visibility = GONE
            signOutButton.visibility = GONE
            addFilmLayout.visibility = VISIBLE

            var selectedOption = 0
            val options = resources.getStringArray(R.array.watch_options)
            val spinner : Spinner = addFilmLayout.findViewById(R.id.spinner)
            spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, options)
            spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    selectedOption = position
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }

            val titleText : EditText = addFilmLayout.findViewById(R.id.enterFilmTitle)
            val ratingText : EditText = addFilmLayout.findViewById(R.id.enterFilmRating)
            val descriptionText : EditText = addFilmLayout.findViewById(R.id.enterFilmDescription)

            val addFilmButton : Button = addFilmLayout.findViewById(R.id.addFilmButton)
            addFilmButton.setOnClickListener(){
                val enteredTitle : String = titleText.text.toString()
                val enteredRating : String = ratingText.text.toString()
                val enteredDescription : String = descriptionText.text.toString()

                val newFilmItem = FilmItem(enteredTitle, enteredDescription, enteredRating, selectedOption, "", databaseUserRef, object : DatabaseSyncListener{
                    override fun onSuccess() {
                        Toast.makeText(this@MainActivity, "Film saved to database successfully!", Toast.LENGTH_SHORT).show()
                    }
                    override fun onFailure(exception: Exception) {
                        Toast.makeText(this@MainActivity, "Film might not be saved to database.\n$exception", Toast.LENGTH_SHORT).show()
                    }
                })

                filmLists[selectedOption].add(0, newFilmItem)
                DataSingleton.adapters[selectedOption].notifyItemInserted(0)

                titleText.text.clear()
                ratingText.text.clear()
                descriptionText.text.clear()

                addFilmLayout.visibility = GONE
                signOutButton.visibility = VISIBLE
                refreshButton.visibility = VISIBLE
            }
            val closeButton : ImageButton = findViewById(R.id.closeAddButton)
            closeButton.setOnClickListener(){
                addFilmLayout.visibility = GONE
                refreshButton.visibility = VISIBLE
                signOutButton.visibility = VISIBLE
            }
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

    private fun updateFilms(databaseUserRef : DatabaseReference, filmLists : List<MutableList<FilmItem>>, viewPager : ViewPager) {
        for(i in 0 until 3) {
            val size = filmLists[i].size
            filmLists[i].clear()
            DataSingleton.adapters[i].notifyItemRangeRemoved(0, size)
        }
        val toRetrieveRef = databaseUserRef.orderByKey()
        toRetrieveRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (filmSnapshot in dataSnapshot.children) {
                    val ref = filmSnapshot.key
                    if(ref == null){
                        Toast.makeText(this@MainActivity, "Failed to download some changes.\nSnapshot key is null", Toast.LENGTH_SHORT).show()
                    } else {
                        val title = filmSnapshot.child("title").getValue(String::class.java) ?: ""
                        val desc = filmSnapshot.child("desc").getValue(String::class.java) ?: ""
                        val rating = filmSnapshot.child("rating").getValue(String::class.java) ?: ""
                        val imageLink = filmSnapshot.child("imageLink").getValue(String::class.java) ?: ""
                        val type = filmSnapshot.child("type").getValue(Int::class.java) ?: 0
                        filmLists[type].add(0, FilmItem(title, desc, rating, type, imageLink, ref))
                        DataSingleton.adapters[type].notifyItemInserted(0)
                    }
                }
            }
            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(this@MainActivity, "Database get request rejected", Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        return true
    }
}
