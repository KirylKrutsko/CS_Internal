package com.example.cs_internal

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.util.Patterns
import android.view.*
import android.view.View.*
import android.view.inputmethod.InputMethodManager
import android.webkit.URLUtil
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import com.example.cs_internal.DataSingleton.filmLists
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.storage
import kotlin.properties.Delegates


class FilmActivity : AppCompatActivity() {

    private lateinit var user: FirebaseUser
    private lateinit var databaseUserRef: DatabaseReference
    private lateinit var storageUserRef : StorageReference
    private lateinit var imageView: ImageView
    private lateinit var filmItem : FilmItem
    private var clickedPage by Delegates.notNull<Int>()
    private var clickedPosition by Delegates.notNull<Int>()

    private val PICK_IMAGE_REQUEST = 1
    private var toDelete = false
    private var changed = false
    private val handler = Handler()

    @SuppressLint("ClickableViewAccessibility")
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

        val title : EditText = findViewById(R.id.filmTitle)
        val description : EditText = findViewById(R.id.filmDescription)
        val rating : EditText = findViewById(R.id.filmRating)
        val commentary : EditText = findViewById(R.id.commentary)
        val optionsButton : ImageButton = findViewById(R.id.optionsButton)
        val backButton : ImageButton = findViewById(R.id.backButton)
        val addButton : ImageButton = findViewById(R.id.addButton)
        val descAdd : ImageButton = findViewById(R.id.addDesc)
        val ratingAdd : ImageButton = findViewById(R.id.addRating)
        val imageAdd : ImageButton = findViewById(R.id.addImage)
        val linkAdd : ImageButton = findViewById(R.id.addLinkButton)
        val tagAdd : ImageButton = findViewById(R.id.addTag)
        val tagLayout : LinearLayout = findViewById(R.id.tagLayout)
        val link : ImageButton = findViewById(R.id.link)
        imageView = findViewById(R.id.imageView)
        optionsButton.visibility = GONE


        val newFilm = intent.getBooleanExtra("new", false)
        clickedPage = intent.getIntExtra("pageNum", -1)
        val id = intent.getStringExtra("id")
        if(clickedPage !in 0 until 3){
            startFailed(false)
        }
        if(newFilm){
            imageAdd.isEnabled = false
            filmItem = FilmItem("", clickedPage, databaseUserRef, object : DatabaseSyncListener{
                override fun onSuccess() {
                    toDelete = true
                    imageAdd.isEnabled = true
                }
                override fun onFailure(exception: java.lang.Exception) {
                    startFailed(true)
                }
            })
            setSaveButtonForNew(backButton, title, rating, description, commentary, clickedPage)
        }
        else{
            var found = false
            for(i in filmLists[clickedPage].indices){
                if(filmLists[clickedPage][i].getDatabaseRef() == id){
                    found = true
                    filmItem = filmLists[clickedPage][i]
                    clickedPosition = i
                }
            }
            if(!found) startFailed(false)

            title.setText(filmItem.getTitle())
            if(filmItem.getRating() != null){
                rating.visibility = VISIBLE
                rating.setText(filmItem.getRating())
            }
            if(filmItem.getDescription() != null){
                description.visibility = VISIBLE
                description.setText(filmItem.getDescription())
            }
            if(filmItem.getCommentary() != null){
                commentary.setText(filmItem.getCommentary())
            }
            if(filmItem.getLink() != null){
                link.visibility = VISIBLE
            }
            for(tag in filmItem.getTags()){
                addTag(tag, tagLayout)
            }
            optionsButton.visibility = VISIBLE
            optionsButton.setOnClickListener { view ->
                val popupMenu = PopupMenu(this, view)
                popupMenu.menuInflater.inflate(R.menu.film_options_menu, popupMenu.menu)
                popupMenu.setOnMenuItemClickListener { menuItem ->
                    when (menuItem.itemId) {
                        R.id.action_delete -> {
                            filmItem.deleteFilm(databaseUserRef, storageUserRef, this, object : DatabaseSyncListener{
                                override fun onSuccess() {
                                    filmLists[clickedPage].removeAt(clickedPosition)
                                    DataSingleton.adapters[clickedPage].notifyItemRemoved(clickedPosition)
                                    Toast.makeText(this@FilmActivity, "Item deleted successfully!", Toast.LENGTH_SHORT).show()
//                                    val intent = Intent(this@FilmActivity, MainActivity::class.java)
//                                    startActivity(intent)
                                    finish()
                                }
                                override fun onFailure(exception: Exception) {
                                    Toast.makeText(this@FilmActivity, "Action failed.\n$exception", Toast.LENGTH_SHORT).show()
                                }
                            })
                            true
                        }
                        R.id.action_deleteImage -> {
                            filmItem.deleteImage(imageView, clickedPosition, databaseUserRef, storageUserRef, this, object : DatabaseSyncListener{
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

            filmItem.loadImageIfExists(imageView, this@FilmActivity, storageUserRef, object : DatabaseSyncListener{
                override fun onSuccess() {}
                override fun onFailure(exception: Exception) {
                    Toast.makeText(this@FilmActivity, "Failed to load image.\n${exception.message}", Toast.LENGTH_SHORT).show()
                }
            })
            title.addTextChangedListener {
                changed = true
                backButton.setImageResource(R.drawable.baseline_done_all_24)
            }
            rating.addTextChangedListener {
                changed = true
                backButton.setImageResource(R.drawable.baseline_done_all_24)
            }
            description.addTextChangedListener {
                changed = true
                backButton.setImageResource(R.drawable.baseline_done_all_24)
            }
            commentary.addTextChangedListener {
                changed = true
                backButton.setImageResource(R.drawable.baseline_done_all_24)
            }

            backButton.setOnClickListener {
                if(changed){
                    val enteredTitle = title.text.toString()
                    if(enteredTitle == ""){
                        Toast.makeText(this@FilmActivity, "Please enter title", Toast.LENGTH_SHORT).show()
                    }
                    else{
                        filmItem.setDescription(description.text.toString())
                        filmItem.setRating(rating.text.toString())
                        filmItem.setCommentary(commentary.text.toString())
                        filmItem.setTitle(enteredTitle)
                        filmItem.saveDataChange(databaseUserRef, object : DatabaseSyncListener{
                            override fun onSuccess() {
                                Toast.makeText(this@FilmActivity, "Saved!", Toast.LENGTH_SHORT).show()
                            }
                            override fun onFailure(exception: java.lang.Exception) {
                                Toast.makeText(this@FilmActivity, "Failed to save some changes.\n${exception.message}", Toast.LENGTH_SHORT).show()
                            }
                        })
                        filmLists[clickedPage].removeAt(clickedPosition)
                        DataSingleton.adapters[clickedPage].notifyItemRemoved(clickedPosition)
                        filmLists[clickedPage].add(0, filmItem)
                        DataSingleton.adapters[clickedPage].notifyItemInserted(0)
                        finish()
                    }
                }
                else finish()
            }
        }

        imageView.setOnClickListener {
            val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(galleryIntent, PICK_IMAGE_REQUEST)
        }

        link.setOnClickListener{
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(filmItem.getLink()))
            startActivity(intent)
        }

        var addClicked = false
        val buttonList = listOf(descAdd, ratingAdd, imageAdd, tagAdd, linkAdd)
        addButton.setOnClickListener(){
            if(addClicked){
                hide(buttonList, addButton)
            }
            else{
                show(buttonList, addButton)
            }
            addClicked = !addClicked
        }
        descAdd.setOnClickListener(){
            description.visibility = VISIBLE
            hide(buttonList, addButton)
            addClicked = false
        }
        ratingAdd.setOnClickListener(){
            rating.visibility = VISIBLE
            hide(buttonList, addButton)
            addClicked = false
        }
        tagAdd.setOnClickListener(){
            addClicked = false
            hide(buttonList, addButton)
            setTagsDialog(tagLayout, backButton)
        }
        imageAdd.setOnClickListener(){
            val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(galleryIntent, PICK_IMAGE_REQUEST)
            hide(buttonList, addButton)
            addClicked = false
        }
        linkAdd.setOnClickListener(){
            setLinkDialog(link, backButton)
            addClicked = false
            hide(buttonList, addButton)
        }

        val rootView = findViewById<View>(android.R.id.content)
        rootView.viewTreeObserver.addOnGlobalLayoutListener {
            val heightDiff = rootView.rootView.height - rootView.height
            if (heightDiff > 200) {
                hide(buttonList, addButton)
                addButton.visibility = GONE
            } else {
                handler.postDelayed({
                    addButton.visibility = VISIBLE
                }, 200)
            }
        }

        val scrollView : ScrollView = findViewById(R.id.scrollView)
        scrollView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                val focusedView = currentFocus
                if (focusedView is EditText) {
                    val outRect = Rect()
                    focusedView.getGlobalVisibleRect(outRect)
                    if (!outRect.contains(event.rawX.toInt(), event.rawY.toInt())) {
                        focusedView.clearFocus()
                        closeKeyboard(focusedView)
                    }
                }
            }
            false
        }
    }

    private fun closeKeyboard(focusedView : View) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(focusedView.windowToken, 0)
    }

    private fun hide(buttonList : List<ImageButton>, mainButton: ImageButton) {
        for(button in buttonList) button.visibility = GONE
        mainButton.rotation = 0f
    }
    private fun show(buttonList : List<ImageButton>, mainButton: ImageButton) {
        for(button in buttonList) button.visibility = VISIBLE
        mainButton.rotation = 45f
    }

    private fun setSaveButtonForNew(backButton: ImageButton, title: EditText, rating: EditText, description: EditText, commentary : EditText, clickedPage: Int){
        backButton.setImageResource(R.drawable.baseline_done_all_24)
        backButton.setOnClickListener {
            val enteredTitle = title.text.toString()
            if(enteredTitle == ""){
                Toast.makeText(this@FilmActivity, "Please enter title", Toast.LENGTH_SHORT).show()
            }
            else{
                toDelete = false
                filmItem.setCommentary(commentary.text.toString())
                filmItem.setDescription(description.text.toString())
                filmItem.setRating(rating.text.toString())
                filmItem.setTitle(enteredTitle)
                filmItem.saveDataChange(databaseUserRef, object : DatabaseSyncListener{
                    override fun onSuccess() {
                        Toast.makeText(this@FilmActivity, "Film saved to database successfully!", Toast.LENGTH_SHORT).show()
                    }
                    override fun onFailure(exception: java.lang.Exception) {
                        Toast.makeText(this@FilmActivity, "Changes might not be saved to database.\n${exception.message}", Toast.LENGTH_SHORT).show()
                    }
                })
                filmLists[clickedPage].add(0, filmItem)
                DataSingleton.adapters[clickedPage].notifyItemInserted(0)
                finish()
            }
//            val intent = Intent(this@FilmActivity, MainActivity::class.java)
//            startActivity(intent)
        }
    }

    private fun setLinkDialog(link : ImageButton, backButton: ImageButton){
        val view = EditText(this)
        view.setPadding(50, 30, 50, 30)
        val current = filmItem.getLink()
        if(current!=null){
            view.setText(current)
        }
        else{
            view.hint = "Paste you link here"
        }
        val dialog = AlertDialog.Builder(this)
            .setTitle("Set link")
            .setView(view)
            .setPositiveButton("SAVE"){ dialog, _ ->
                val enteredLink = view.text.toString().trim()
                if(enteredLink == ""){
                    filmItem.setLink(enteredLink)
                    link.visibility = GONE
                    changed = true
                    backButton.setImageResource(R.drawable.baseline_done_all_24)
                    dialog.dismiss()
                }
                else if(isValidUrl(enteredLink)){
                    filmItem.setLink(enteredLink)
                    link.visibility = VISIBLE
                    changed = true
                    backButton.setImageResource(R.drawable.baseline_done_all_24)
                    dialog.dismiss()
                }
                else{
                    Toast.makeText(this, "Invalid link format", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    setLinkDialog(link, backButton)
                }
            }
            .setNegativeButton("CANCEL"){ _, _ ->}
            .create()
        dialog.show()
    }

    private fun addTag(name: String, tagLayout: LinearLayout){
        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val itemView = inflater.inflate(R.layout.tag_list_item, null)

        val itemTextView : TextView = itemView.findViewById(R.id.textView)
        itemTextView.text = name

        tagLayout.addView(itemView, tagLayout.childCount)
    }

    private fun setTagsDialog(tagLayout: LinearLayout, backButton: ImageButton){
        val allTags = DataSingleton.tags.toTypedArray()
        val currentTags = filmItem.getTags()
        val bools : MutableList<Boolean> = mutableListOf()
        for(tag in allTags){
            if(currentTags.contains(tag)) bools.add(true)
            else bools.add(false)
        }
        val dialog  = AlertDialog.Builder(this)
            .setTitle("Select tags")
            .setMultiChoiceItems(allTags, bools.toBooleanArray()) { _, which, isChecked ->
                bools[which] = isChecked
            }
            .setPositiveButton("Confirm") { dialog, _ ->
                tagLayout.removeAllViews()
                filmItem.deleteAllTags()
                for(i in bools.indices){
                    if(bools[i]){
                        filmItem.addTag(allTags[i])
                        addTag(allTags[i], tagLayout)
                    }
                }
                changed = true
                backButton.setImageResource(R.drawable.baseline_done_all_24)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { _, _ -> }
            .setNeutralButton("Add new"){ dialog, _ ->
                dialog.dismiss()
                setNewTagDialog(tagLayout, backButton)
            }
            .create()
        dialog.show()
    }

    private fun setNewTagDialog(tagLayout: LinearLayout, backButton: ImageButton){
        val dialogView = LayoutInflater.from(this).inflate(R.layout.new_tag_dialog_layout, null)
        val tagInput : EditText = dialogView.findViewById(R.id.tagInput)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setTitle("Create a new tag")
            .setPositiveButton("Add"){ dialog, _ ->
                val enteredTag = tagInput.text.toString()
                if(enteredTag != "") {
                    DataSingleton.tags.add(enteredTag)
                    closeKeyboard(tagInput)
                    dialog.dismiss()
                    setTagsDialog(tagLayout, backButton)
                }
                else{
                    Toast.makeText(this, "Tag name cannot be empty", Toast.LENGTH_SHORT).show()
                    setNewTagDialog(tagLayout, backButton)
                }
            }
            .setNegativeButton("Cancel"){ dialog, _ ->
                closeKeyboard(tagInput)
                dialog.dismiss()
                setTagsDialog(tagLayout, backButton)
            }
            .create()
        dialog.show()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            val selectedImageUri : Uri? = data.data
            if(selectedImageUri != null){
                filmItem.setImageByURI(selectedImageUri, imageView, this@FilmActivity, storageUserRef, databaseUserRef, object : DatabaseSyncListener{
                    override fun onSuccess() {
                        DataSingleton.adapters[clickedPage].notifyItemChanged(clickedPosition)
                        Toast.makeText(this@FilmActivity, "Image saved successfully!", Toast.LENGTH_SHORT).show()
                    }
                    override fun onFailure(exception: java.lang.Exception) {
                        Toast.makeText(this@FilmActivity, "Action failed.\n${exception.message}", Toast.LENGTH_SHORT).show()
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

    private fun startFailed(reason : Boolean){
        if(reason){
            Toast.makeText(this, "Failed to create a new item in database", Toast.LENGTH_SHORT).show()
        }
        else{
            Toast.makeText(this, "Failed to start activity", Toast.LENGTH_SHORT).show()
        }
//        val intentToBack = Intent(this@FilmActivity, MainActivity::class.java)
//        startActivity(intentToBack)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        if(toDelete){
            Toast.makeText(this@FilmActivity, "delete", Toast.LENGTH_SHORT).show()
            filmItem.deleteFilm(databaseUserRef, storageUserRef, this@FilmActivity, object : DatabaseSyncListener{
                override fun onSuccess() {}
                override fun onFailure(exception: java.lang.Exception) {}
            })
        }
    }

    private fun isValidUrl(url: String?): Boolean {
        return url != null && Patterns.WEB_URL.matcher(url).matches() && URLUtil.isValidUrl(url)
    }
}