package com.example.cs_internal

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.Menu
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import com.example.cs_internal.DataSingleton.filmLists
import com.google.android.play.integrity.internal.c
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.storage
import com.squareup.picasso.Picasso
import java.io.ByteArrayOutputStream
import java.io.File
import java.lang.Float.max
import java.util.*


class FilmActivity : AppCompatActivity() {

    private lateinit var user: FirebaseUser
    private lateinit var databaseUserRef: DatabaseReference
    private lateinit var storageUserRef : StorageReference
    private lateinit var imageView: ImageView
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var filmItem : FilmItem
    private val PICK_IMAGE_REQUEST = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_film)

        val currentUser = FirebaseAuth.getInstance().currentUser
        if(currentUser == null) {
            val intentToLogin = Intent(this@FilmActivity, LoginActivity::class.java)
            startActivity(intentToLogin)
        }
        else user = currentUser
        databaseUserRef = FirebaseDatabase.getInstance().getReference("users/"+user.uid)
        storageUserRef = Firebase.storage.reference.child("users/"+user.uid)

        val clickedPage = intent.getIntExtra("pageNum", -1)
        val clickedPosition = intent.getIntExtra("itemPosition", -1)
        if(!(clickedPage in 0 until 3 && clickedPosition >= 0)){
            Toast.makeText(this, "Failed to start activity", Toast.LENGTH_SHORT).show()
            val intentToBack = Intent(this@FilmActivity, MainActivity::class.java)
            startActivity(intentToBack)
        }
        filmItem = filmLists[clickedPage][clickedPosition]

        val title : EditText = findViewById(R.id.filmTitle)
        val description : EditText = findViewById(R.id.filmDescription)
        val rating : EditText = findViewById(R.id.filmRating)
        val optionsButton : ImageButton = findViewById(R.id.optionsButton)
        val backButton : ImageButton = findViewById(R.id.backButton)
        val saveButton : ImageButton = findViewById(R.id.saveButton)
        title.setText(filmItem.getTitle())
        description.setText(filmItem.getDescription())
        rating.setText(filmItem.getRating())

        backButton.setOnClickListener(){
            val intentToMain = Intent(this@FilmActivity, MainActivity::class.java)
            startActivity(intentToMain)
        }

        title.addTextChangedListener {
            setSaveButton(saveButton, title, rating, description, clickedPage, clickedPosition)
        }
        rating.addTextChangedListener {
            setSaveButton(saveButton, title, rating, description, clickedPage, clickedPosition)
        }
        description.addTextChangedListener {
            setSaveButton(saveButton, title, rating, description, clickedPage, clickedPosition)
        }

        optionsButton.setOnClickListener { view ->
            val popupMenu = PopupMenu(this, view)
            popupMenu.menuInflater.inflate(R.menu.film_options_menu, popupMenu.menu)
            popupMenu.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.action_delete -> {
                        filmItem.deleteFilm(databaseUserRef, storageUserRef, this, object : DatabaseSyncListener{
                            override fun onSuccess() {
                                DataSingleton.adapters[clickedPage].notifyItemRemoved(clickedPosition)
                                Toast.makeText(this@FilmActivity, "Item deleted successfully!", Toast.LENGTH_SHORT).show()
                                val intent = Intent(this@FilmActivity, MainActivity::class.java)
                                startActivity(intent)
                                finish()
                            }
                            override fun onFailure(exception: Exception) {
                                Toast.makeText(this@FilmActivity, "Action failed.\n$exception", Toast.LENGTH_SHORT).show()
                            }
                        })
                        true
                    }
                    R.id.action_deleteImage -> {
                        filmItem.deleteImage(imageView, databaseUserRef, storageUserRef, this, object : DatabaseSyncListener{
                            override fun onSuccess() {
                                Toast.makeText(this@FilmActivity, "Image deleted successfully!", Toast.LENGTH_SHORT).show()
                            }
                            override fun onFailure(exception: Exception) {
                                Toast.makeText(this@FilmActivity, "Action failed.\n$exception", Toast.LENGTH_SHORT).show()
                            }
                        })
                        true
                    }
                    R.id.action_changeImage -> {
                        val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                        startActivityForResult(galleryIntent, PICK_IMAGE_REQUEST)
                        true
                    }
                    else -> false
                }
            }
            popupMenu.show()
        }

        imageView = findViewById(R.id.imageView)
        filmItem.loadImageIfExists(imageView, this@FilmActivity, storageUserRef, object : DatabaseSyncListener{
            override fun onSuccess() {}
            override fun onFailure(exception: Exception) {
                Toast.makeText(this@FilmActivity, "ex.\n$exception", Toast.LENGTH_SHORT).show()
            }
        })
        imageView.setOnClickListener(){
            val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(galleryIntent, PICK_IMAGE_REQUEST)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            val selectedImageUri : Uri? = data.data
            if(selectedImageUri != null){
                filmItem.setImageByURI(selectedImageUri, imageView, this@FilmActivity, storageUserRef, databaseUserRef, object : DatabaseSyncListener{
                    override fun onSuccess() {
                        Toast.makeText(this@FilmActivity, "Image saved successfully!", Toast.LENGTH_SHORT).show()
                    }
                    override fun onFailure(exception: java.lang.Exception) {
                        Toast.makeText(this@FilmActivity, "Action failed.\n$exception", Toast.LENGTH_SHORT).show()
                    }
                })
            } else{
                Toast.makeText(this, "Failed to get image URI", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.film_options_menu, menu)
        return true
    }

    private fun setSaveButton(saveButton : ImageButton, title : EditText, rating : EditText, description : EditText, clickedPage : Int, clickedPos : Int){
        saveButton.visibility = VISIBLE
        saveButton.setOnClickListener(){
            val enteredTitle = title.text.toString()
            val enteredDesc = description.text.toString()
            val enteredRating = rating.text.toString()
            filmItem.changeData(enteredTitle, enteredDesc, enteredRating, databaseUserRef, object : DatabaseSyncListener{
                override fun onSuccess() {
                    Toast.makeText(this@FilmActivity, "Changes saved successfully!", Toast.LENGTH_SHORT).show()
                }
                override fun onFailure(exception: java.lang.Exception) {
                    Toast.makeText(this@FilmActivity, "Some changes might not be saved!\n${exception}", Toast.LENGTH_SHORT).show()
                }
            })
            DataSingleton.adapters[clickedPage].notifyItemChanged(clickedPos)
            saveButton.visibility = GONE
        }
    }
}