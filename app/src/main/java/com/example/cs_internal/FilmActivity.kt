package com.example.cs_internal

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Patterns
import android.view.*
import android.view.View.*
import android.view.inputmethod.InputMethodManager
import android.webkit.URLUtil
import android.widget.*
import androidx.appcompat.widget.Toolbar
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.setPadding
import androidx.core.widget.addTextChangedListener
import com.example.cs_internal.DataSingleton.TMDB_API_KEY
import com.example.cs_internal.DataSingleton.filmLists
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.storage
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.properties.Delegates


class FilmActivity : AppCompatActivity() {

    private lateinit var user: FirebaseUser
    private lateinit var databaseUserRef: DatabaseReference
    private lateinit var storageUserRef : StorageReference
    private lateinit var filmItem : FilmItem
    private lateinit var imageView : ImageView
    private lateinit var markImage : MenuItem
    private lateinit var toolbar : Toolbar
    private lateinit var title : EditText
    private lateinit var description : EditText
    private lateinit var rating : EditText
    private lateinit var commentary : EditText
    private lateinit var yearInput : EditText
    private lateinit var timecodeInput : TimecodeInput
    private lateinit var ratingStar : ImageView
    private lateinit var tagLayout : LinearLayout
    private lateinit var mainLinearLayout : LinearLayout
    private lateinit var commentsBreak : TextView
    private lateinit var playLayout : LinearLayout

    private var newFilm by Delegates.notNull<Boolean>()
    private var clickedPage by Delegates.notNull<Int>()
    private var clickedPosition by Delegates.notNull<Int>()

    private val PICK_IMAGE_REQUEST = 1
    private val TMDB_ACTIVITY_REQUEST = 2
    private val PICK_JSON_REQUEST = 3

    private var toDelete = false
    private var changed = false
    private var addClicked = false
    private val handler = Handler()
    private var comments : MutableList<MutableComment> = mutableListOf()

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

        toolbar = findViewById(R.id.toolbar)
        toolbar.title = ""
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener {
            if(title.text.toString().isBlank()){
                Toast.makeText(this, "Please enter title", Toast.LENGTH_SHORT).show()
            }
            else {
                toDelete = false
                finish()
            }
        }

        title = toolbar.findViewById(R.id.title)

        description = findViewById(R.id.filmDescription)
        rating = findViewById(R.id.filmRating)
        commentary = findViewById(R.id.commentary)
        yearInput = findViewById(R.id.releaseYear)
        ratingStar = findViewById(R.id.imageStar)

        val addButton : ImageButton = findViewById(R.id.addButton)
        val descAdd : ImageButton = findViewById(R.id.addDesc)
        val ratingAdd : ImageButton = findViewById(R.id.addRating)
        val imageAdd : ImageButton = findViewById(R.id.addImage)
        val linkAdd : ImageButton = findViewById(R.id.addLinkButton)
        val tagAdd : ImageButton = findViewById(R.id.addTag)
        val commentAdd : ImageButton = findViewById(R.id.addComment)
        val yearAdd : ImageButton = findViewById(R.id.addYear)
        val markAdd : ImageButton = findViewById(R.id.addMark)

        tagLayout = findViewById(R.id.tagLayout)
        mainLinearLayout = findViewById(R.id.mainLinearLayout)
        val scrollView : ScrollView = findViewById(R.id.scrollView)
        val searchResultsText : TextView = findViewById(R.id.searchResultsText)
        val closeResultsButton : ImageButton = findViewById(R.id.closeResultsButton)
        commentsBreak = findViewById(R.id.commentsBreak)

        val playButton : ImageButton = findViewById(R.id.playButton)
        val seasonPlay : EditText = findViewById(R.id.seasonPlay)
        val seasonText : TextView = findViewById(R.id.seasonPlayText)
        val seriesPlay : EditText = findViewById(R.id.seriesPlay)
        val seriesText : TextView = findViewById(R.id.seriesPlayText)
        val hoursPlay : EditText = findViewById(R.id.hoursPlay)
        val minutesPlay : EditText = findViewById(R.id.minutesPlay)
        val secondsPlay : EditText = findViewById(R.id.secondsPlay)
        val linkButton : ImageButton = findViewById(R.id.link)
        playLayout = findViewById(R.id.playLayout)
        timecodeInput = TimecodeInput(seasonPlay, seriesPlay, hoursPlay, minutesPlay, secondsPlay, seasonText, seriesText)

        imageView = findViewById(R.id.imageView)

        newFilm = intent.getBooleanExtra("new", false)
        clickedPage = intent.getIntExtra("pageNum", -1)
        if(clickedPage !in 0 until 4) startFailed(1)

        if(newFilm){
            imageAdd.isEnabled = false
            filmItem = FilmItem("", clickedPage, databaseUserRef, object : DatabaseSyncListener{
                override fun onSuccess() {
                    toDelete = true
                    imageAdd.isEnabled = true
                }
                override fun onFailure(exception: java.lang.Exception) {
                    startFailed(0)
                }
            })
            toolbar.navigationIcon = ContextCompat.getDrawable(this, R.drawable.done_v_svgrepo_com)
            val newTitle = intent.getStringExtra("title")
            if(newTitle != null){
                val newDesc = intent.getStringExtra("desc")
                val newImageUrl = intent.getStringExtra("image")
                val year = intent.getStringExtra("year") ?: ""
                title.setText(newTitle)
                description.visibility = VISIBLE
                description.setText(newDesc)
                if(year.isNotEmpty()){
                    yearInput.visibility = VISIBLE
                    yearInput.setText(year)
                }
                if(newImageUrl != null){
                    filmItem.setImageByURI(Uri.parse(newImageUrl), imageView, this@FilmActivity, storageUserRef, databaseUserRef, object : DatabaseSyncListener{
                        override fun onSuccess() {}
                        override fun onFailure(exception: java.lang.Exception) {
                            Toast.makeText(this@FilmActivity, "Failed to save image", Toast.LENGTH_SHORT).show()
                            Toast.makeText(this@FilmActivity, exception.message, Toast.LENGTH_LONG).show()
                        }
                    })
                }
            }
            else{
                title.setOnFocusChangeListener { _, hasFocus ->
                    if(!hasFocus && title.text.toString().isNotBlank()){
                        searchForTitleInDatabase(title.text.toString(), searchResultsText, closeResultsButton)
                    }
                }
            }
        }
        else{
            if(clickedPage != 3){
                clickedPosition = intent.getIntExtra("clickedPosition", 0)
                filmItem = filmLists[clickedPage][clickedPosition]
            }
            else{ // from sort activity
                val id = intent.getStringExtra("id")
                var found = false
                for(page in 0 until 3){
                    for(i in filmLists[page].indices){
                        if(filmLists[page][i].getDatabaseRef() == id){
                            found = true
                            filmItem = filmLists[page][i]
                            clickedPage = page
                            clickedPosition = i
                            break
                        }
                    }
                    if(found) break
                }
                if(!found) startFailed(2)
            }

            setAllFields()

            title.addTextChangedListener {
                onItemChanged()
            }
            rating.addTextChangedListener {
                onItemChanged()
            }
            description.addTextChangedListener {
                onItemChanged()
            }
            commentary.addTextChangedListener {
                onItemChanged()
            }
            yearInput.addTextChangedListener {
                onItemChanged()
            }
        }

        imageView.setOnClickListener {
            val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(galleryIntent, PICK_IMAGE_REQUEST)
        }

        playButton.setOnClickListener{
            val link = filmItem.getLink()
            if(link == null){
                Toast.makeText(this@FilmActivity, "This film does not have a link to follow", Toast.LENGTH_LONG).show()
            }
            else{
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
                startActivity(intent)
            }
        }
        linkButton.setOnClickListener{
            setLinkDialog()
        }

        val buttonList = listOf(descAdd, ratingAdd, imageAdd, tagAdd, linkAdd, commentAdd, yearAdd, markAdd)
        addButton.setOnClickListener(){
            if(addClicked){
                hideButtons(buttonList, addButton)
            }
            else{
                showButtons(buttonList, addButton)
            }
        }
        descAdd.setOnClickListener(){
            description.visibility = VISIBLE
            hideButtons(buttonList, addButton)
        }
        ratingAdd.setOnClickListener(){
            rating.visibility = VISIBLE
            ratingStar.visibility = VISIBLE
            hideButtons(buttonList, addButton)
        }
        tagAdd.setOnClickListener(){
            hideButtons(buttonList, addButton)
            setTagsDialog(tagLayout)
        }
        imageAdd.setOnClickListener(){
            val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(galleryIntent, PICK_IMAGE_REQUEST)
            hideButtons(buttonList, addButton)
        }
        linkAdd.setOnClickListener(){
            setNewLinkDialog()
            hideButtons(buttonList, addButton)
        }
        yearAdd.setOnClickListener {
            yearInput.visibility = VISIBLE
            hideButtons(buttonList, addButton)
        }
        markAdd.setOnClickListener {
            setMarkPicker()
            hideButtons(buttonList, addButton)
        }
        commentAdd.setOnClickListener {
            hideButtons(buttonList, addButton)
            if(comments.isEmpty()) commentsBreak.visibility = VISIBLE
            val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            val itemView = inflater.inflate(R.layout.comment_list_item, null)
            val seasonText : TextView = itemView.findViewById(R.id.seasonText)
            val seriesText : TextView = itemView.findViewById(R.id.seriesText)
            val season : EditText = itemView.findViewById(R.id.seasonInput)
            val series : EditText = itemView.findViewById(R.id.seriesInput)
            val hours : EditText = itemView.findViewById(R.id.hours_input)
            val minutes : EditText = itemView.findViewById(R.id.minutes_input)
            val seconds : EditText = itemView.findViewById(R.id.seconds_input)
            val text : EditText = itemView.findViewById(R.id.commentEditText)
            val comment = MutableComment(season, series, hours, minutes, seconds, text, seasonText, seriesText)
            comment.setup(text, filmItem.isASeries)
            comments.add(comment)
            mainLinearLayout.addView(itemView, mainLinearLayout.childCount)
            onItemChanged()
        }

        val rootView = findViewById<View>(android.R.id.content)
        rootView.viewTreeObserver.addOnGlobalLayoutListener {
            val heightDiff = rootView.rootView.height - rootView.height
            if (heightDiff > 200) {
                hideButtons(buttonList, addButton)
                addButton.visibility = GONE
            } else {
                handler.postDelayed({
                    addButton.visibility = VISIBLE
                }, 200)
            }
        }

        val overviewBreak : TextView = findViewById(R.id.overviewBreak)
        val overviewLayout : ConstraintLayout = findViewById(R.id.constraintLayout)
        overviewLayout.viewTreeObserver.addOnGlobalLayoutListener {
            if(overviewLayout.height > 0){
                overviewBreak.visibility = VISIBLE
            }
        }

        scrollView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                val focusedView = currentFocus
                if (focusedView is EditText) {
                    val outRect = Rect()
                    focusedView.getGlobalVisibleRect(outRect)
                    if (!outRect.contains(event.rawX.toInt(), event.rawY.toInt())) {
                        closeKeyboard(focusedView)
                    }
                }
                if(addClicked) hideButtons(buttonList, addButton)
            }
            false
        }
    }


    private fun setAllFields(){
        title.setText(filmItem.getTitle())
        if(filmItem.getRating() != null){
            rating.visibility = VISIBLE
            ratingStar.visibility = VISIBLE
            rating.setText(filmItem.getRating())
        }
        if(filmItem.getDescription() != null){
            description.visibility = VISIBLE
            description.setText(filmItem.getDescription())
        }
        if(filmItem.getCommentary() != null){
            commentary.setText(filmItem.getCommentary())
        }
        if(filmItem.getYear() != null){
            yearInput.setText(filmItem.getYear().toString())
            yearInput.visibility = VISIBLE
        }
        for(tag in filmItem.getTags()){
            addTag(tag, tagLayout)
        }
        addComments(filmItem.getComments(), mainLinearLayout, commentsBreak)
        filmItem.loadImageIfExists(imageView, this@FilmActivity, storageUserRef, object : DatabaseSyncListener{
            override fun onSuccess() {}
            override fun onFailure(exception: Exception) {
                Toast.makeText(this@FilmActivity, "Failed to load image.", Toast.LENGTH_SHORT).show()
                Toast.makeText(this@FilmActivity, exception.message, Toast.LENGTH_LONG).show()
            }
        })

        if(clickedPage > 0){
            playLayout.visibility = VISIBLE
            val time = filmItem.getWatchTime()
            if(time != null){
                timecodeInput.setTime(time)
            }
            timecodeInput.setup(null, filmItem.isASeries)
            if(!filmItem.isASeries) timecodeInput.expandSeriesText()
        }
        else playLayout.visibility = GONE
    }

    private fun saveDataChange(){
        Toast.makeText(this, "Saving...", Toast.LENGTH_SHORT).show()
        try {
            filmItem.setTitle(title.text.toString())
            filmItem.setDescription(description.text.toString())
            filmItem.setRating(rating.text.toString())
            filmItem.setCommentary(commentary.text.toString())
            filmItem.setYear(yearInput.text.toString().toIntOrNull())
            filmItem.deleteAllComments()
            for(comment in comments){
                filmItem.addComment(comment.getComment())
            }
            filmItem.sortComments()
            filmItem.setWatchTime(timecodeInput.getTime())
            filmItem.saveDataChange(databaseUserRef, object : DatabaseSyncListener{
                override fun onSuccess() {
                    Toast.makeText(this@FilmActivity, "Saved!", Toast.LENGTH_SHORT).show()
                }
                override fun onFailure(exception: java.lang.Exception) {
                    Toast.makeText(this@FilmActivity, "Failed to save some changes.", Toast.LENGTH_SHORT).show()
                    Toast.makeText(this@FilmActivity, exception.message, Toast.LENGTH_LONG).show()
                }
            })
            if(!newFilm){
                filmLists[clickedPage].removeAt(clickedPosition)
                DataSingleton.adapters[clickedPage].notifyItemRemoved(clickedPosition)
            }
            filmLists[clickedPage].add(0, filmItem)
            DataSingleton.adapters[clickedPage].notifyItemInserted(0)
        } catch (e : Exception){
            Toast.makeText(this@FilmActivity, "Failed.", Toast.LENGTH_SHORT).show()
            Toast.makeText(this@FilmActivity, e.message, Toast.LENGTH_LONG).show()
        }
    }


    private fun closeKeyboard(focusedView : View) {
        focusedView.clearFocus()
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(focusedView.windowToken, 0)
    }

    private fun hideButtons(buttonList : List<ImageButton>, mainButton: ImageButton) {
        for(button in buttonList) button.visibility = GONE
        mainButton.rotation = 0f
        addClicked = false
    }
    private fun showButtons(buttonList : List<ImageButton>, mainButton: ImageButton) {
        for(button in buttonList) button.visibility = VISIBLE
        mainButton.rotation = 45f
        addClicked = true
    }

    private fun setConfirmDeleteDialog() {
        val view = TextView(this)
        view.setPadding(50)
        view.setText("Once you press delete button, the removed item cannot be restored!\nAre you sure to continue?")
        view.setTextColor(Color.RED)
        val dialog = AlertDialog.Builder(this)
            .setTitle("Delete film?")
            .setView(view)
            .setPositiveButton("Delete"){ _,_ ->
                if(!newFilm){
                    filmLists[clickedPage].removeAt(clickedPosition)
                    DataSingleton.adapters[clickedPage].notifyItemRemoved(clickedPosition)
                }
                changed = false
                newFilm = false
                toDelete = false
                filmItem.deleteFilm(databaseUserRef, storageUserRef, this, object : DatabaseSyncListener{
                    override fun onSuccess() {
                        Toast.makeText(this@FilmActivity, "Item deleted successfully!", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    override fun onFailure(exception: Exception) {
                        Toast.makeText(this@FilmActivity, "Action failed.", Toast.LENGTH_SHORT).show()
                        Toast.makeText(this@FilmActivity, exception.message, Toast.LENGTH_LONG).show()
                    }
                })
            }
            .setNeutralButton("Cancel"){_,_ ->}
            .create()
        dialog.show()
    }

    private fun setNewLinkDialog(){
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
                    onItemChanged()
                    dialog.dismiss()
                }
                else if(isValidUrl(enteredLink)){
                    filmItem.setLink(enteredLink)
                    onItemChanged()
                    dialog.dismiss()
                }
                else{
                    Toast.makeText(this, "Invalid link format", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    setNewLinkDialog()
                }
            }
            .setNegativeButton("CANCEL"){ _, _ ->}
            .create()
        dialog.show()
    }

    private fun setLinkDialog(){
        val link = filmItem.getLink()
        if(link == null) {
            setNewLinkDialog()
            return
        }
        val view = EditText(this)
        view.setPadding(50, 30, 50, 30)
        view.setText(link)
        val dialog = AlertDialog.Builder(this)
            .setTitle("Film link")
            .setView(view)
            .setPositiveButton("FOLLOW"){ _, _ ->
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
                startActivity(intent)
            }
            .setNeutralButton("CHANGE"){ _, _ ->
                setNewLinkDialog()
            }
            .setNegativeButton("CANCEL"){ _, _ ->}
            .create()
        dialog.show()
    }

    private fun addTag(name: String, tagLayout: LinearLayout){
        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val itemView = inflater.inflate(R.layout.tag_list_item, null)
        val textView : TextView = itemView.findViewById(R.id.textView)
        val button : ImageButton = itemView.findViewById(R.id.imageButton)
        textView.text = name
        tagLayout.addView(itemView, tagLayout.childCount)
        button.setOnClickListener {
            val intent = Intent(this@FilmActivity, SearchActivity::class.java)
                .putExtra("tagName", name)
            startActivity(intent)
        }
    }
    private fun addComments(filmComments : Array<Comment>, layout: LinearLayout, commentsText : TextView) {
        if(filmComments.isEmpty()) return
        commentsText.visibility = VISIBLE
        var prevSeries = -1
        var prevSeason = -1
        for(i in filmComments.indices){
            val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            val itemView = inflater.inflate(R.layout.comment_list_item, null)
            val seasonText : TextView = itemView.findViewById(R.id.seasonText)
            val seriesText : TextView = itemView.findViewById(R.id.seriesText)
            val season : EditText = itemView.findViewById(R.id.seasonInput)
            val series : EditText = itemView.findViewById(R.id.seriesInput)
            val hours : EditText = itemView.findViewById(R.id.hours_input)
            val minutes : EditText = itemView.findViewById(R.id.minutes_input)
            val seconds : EditText = itemView.findViewById(R.id.seconds_input)
            val text : EditText = itemView.findViewById(R.id.commentEditText)
            val comment = MutableComment(
                season, series, hours, minutes, seconds, text,
                seasonText, seriesText, prevSeason, prevSeries,
                filmComments[i].time, filmComments[i].text
            )
            prevSeason = comment.curSeason()
            prevSeries = comment.curSeries()
            comments.add(comment)
            layout.addView(itemView, layout.childCount)
        }
    }

    private fun setTagsDialog(tagLayout: LinearLayout){
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
                onItemChanged()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { _, _ -> }
            .setNeutralButton("Add new"){ dialog, _ ->
                dialog.dismiss()
                setNewTagDialog(tagLayout)
            }
            .create()
        dialog.show()
    }

    private fun setNewTagDialog(tagLayout: LinearLayout){
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
                    setTagsDialog(tagLayout)
                }
                else{
                    Toast.makeText(this, "Tag name cannot be empty", Toast.LENGTH_SHORT).show()
                    setNewTagDialog(tagLayout)
                }
            }
            .setNegativeButton("Cancel"){ dialog, _ ->
                closeKeyboard(tagInput)
                dialog.dismiss()
                setTagsDialog(tagLayout)
            }
            .create()
        dialog.show()
    }

    private fun setMarkPicker(){
        val view = LayoutInflater.from(this).inflate(R.layout.mark_picker_layout, null)
        val dialog = AlertDialog.Builder(this)
            .setTitle("Select mark")
            .setView(view)
            .setNeutralButton("Cancel"){_,_ ->}
            .setNegativeButton("Delete mark"){_,_->
                filmItem.setMark(null)
                markImage.setVisible(false)
                onItemChanged()
            }
            .create()
        dialog.show()
        view.findViewById<ImageButton>(R.id.mark1).setOnClickListener {
            filmItem.setMark(0)
            markImage.setIcon(R.drawable.emoji_1_svgrepo_com)
            markImage.setVisible(true)
            dialog.dismiss()
            onItemChanged()
        }
        view.findViewById<ImageButton>(R.id.mark2).setOnClickListener {
            filmItem.setMark(1)
            markImage.setIcon(R.drawable.emoji_2_svgrepo_com)
            markImage.setVisible(true)
            dialog.dismiss()
            onItemChanged()
        }
        view.findViewById<ImageButton>(R.id.mark3).setOnClickListener {
            filmItem.setMark(2)
            markImage.setIcon(R.drawable.emoji_3_svgrepo_com)
            markImage.setVisible(true)
            dialog.dismiss()
            onItemChanged()
        }
        view.findViewById<ImageButton>(R.id.mark4).setOnClickListener {
            filmItem.setMark(3)
            markImage.setIcon(R.drawable.emoji_4_svgrepo_com)
            markImage.setVisible(true)
            dialog.dismiss()
            onItemChanged()
        }
        view.findViewById<ImageButton>(R.id.mark5).setOnClickListener {
            filmItem.setMark(4)
            markImage.setIcon(R.drawable.emoji_5_svgrepo_com)
            markImage.setVisible(true)
            dialog.dismiss()
            onItemChanged()
        }
    }

    private fun setChangeTypeDialog(){
        var checkedItem = 0
        if(filmItem.isASeries) checkedItem = 1
        val dialog = AlertDialog.Builder(this)
        dialog.setTitle("Choose type")
            .setSingleChoiceItems(arrayOf("Movie", "TV series"), checkedItem) { _, item ->
                checkedItem = item
            }
            .setNeutralButton("Cavcel"){_,_->}
            .setPositiveButton("Set"){ _,_ ->
                if(filmItem.isASeries != (checkedItem == 1)){
                    filmItem.isASeries = (checkedItem == 1)
                    if(clickedPage == 1){
                        timecodeInput.setup(null, filmItem.isASeries)
                        if(!filmItem.isASeries) timecodeInput.expandSeriesText()
                    }
                    var prevSeries = -1
                    var prevSeason = -1
                    for(comment in comments){
                        comment.reset(prevSeason, prevSeries)
                        prevSeason = comment.curSeason()
                        prevSeries = comment.curSeries()
                    }
                    onItemChanged()
                }
            }
            .create()
        dialog.show()
    }

    private fun setConfirmExitDialog(){
        val view = TextView(this)
        view.setPadding(50)
        view.setText("All your entered changes will not be saved!\nAre you sure to exit?")
        val dialog = AlertDialog.Builder(this)
            .setTitle("Exit without saving?")
            .setView(view)
            .setPositiveButton("Exit"){ _,_ ->
                changed = false
                finish()
            }
            .setNeutralButton("Cancel"){_,_ ->}
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
                        if(!newFilm){
                            DataSingleton.adapters[clickedPage].notifyItemChanged(clickedPosition)
                        }
                        Toast.makeText(this@FilmActivity, "Image saved successfully!", Toast.LENGTH_SHORT).show()
                    }
                    override fun onFailure(exception: java.lang.Exception) {
                        Toast.makeText(this@FilmActivity, "Action failed.", Toast.LENGTH_SHORT).show()
                        Toast.makeText(this@FilmActivity, exception.message, Toast.LENGTH_LONG).show()
                    }
                })
            } else{
                Toast.makeText(this, "Failed to get image URI", Toast.LENGTH_SHORT).show()
            }
        }
        else if (requestCode == PICK_JSON_REQUEST && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                try {
                    val inputStream = contentResolver.openInputStream(uri)
                    val json = inputStream?.bufferedReader().use { it?.readText() }
                    inputStream?.close()

                    val ref = filmItem.getDatabaseRef()
                    filmItem = Gson().fromJson(json, FilmItem::class.java)
                    filmItem.setReference(ref)
                    clickedPage = filmItem.getType()
                    setAllFields()

                    for(tag in filmItem.getTags()){
                        if(!DataSingleton.tags.contains(tag)){
                            DataSingleton.tags.add(tag)
                        }
                    }

                } catch (e: IOException) {
                    Toast.makeText(this, "Error parsing json file.\n${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
        else if(requestCode == TMDB_ACTIVITY_REQUEST && resultCode == Activity.RESULT_OK){
            finish()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.film_toolbar_menu, menu)
        markImage = menu.findItem(R.id.markImage)
        when(filmItem.getMark()){
            0 -> markImage.setIcon(R.drawable.emoji_1_svgrepo_com)
            1 -> markImage.setIcon(R.drawable.emoji_2_svgrepo_com)
            2 -> markImage.setIcon(R.drawable.emoji_3_svgrepo_com)
            3 -> markImage.setIcon(R.drawable.emoji_4_svgrepo_com)
            4 -> markImage.setIcon(R.drawable.emoji_5_svgrepo_com)
            else -> markImage.setVisible(false)
        }
        if(!newFilm){
            val importButton = menu.findItem(R.id.action_import)
            importButton.setVisible(false)
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.markImage ->{
                setMarkPicker()
                return true
            }
            R.id.action_delete -> {
                setConfirmDeleteDialog()
                return true
            }
            R.id.action_deleteImage -> {
                filmItem.deleteImage(imageView, clickedPosition, databaseUserRef, storageUserRef, this, object : DatabaseSyncListener{
                    override fun onSuccess() {
                        Toast.makeText(this@FilmActivity, "Image deleted successfully!", Toast.LENGTH_SHORT).show()
                    }
                    override fun onFailure(exception: Exception) {
                        Toast.makeText(this@FilmActivity, "Action failed.", Toast.LENGTH_SHORT).show()
                        Toast.makeText(this@FilmActivity, exception.message, Toast.LENGTH_LONG).show()
                    }
                })
                return true
            }
            R.id.action_changeImage -> {
                val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                startActivityForResult(galleryIntent, PICK_IMAGE_REQUEST)
                return true
            }
            R.id.action_changeType -> {
                setChangeTypeDialog()
                return true
            }
            R.id.action_exit -> {
                setConfirmExitDialog()
                return true
            }
            R.id.action_export -> {
                val jsonString = GsonBuilder()
                    .excludeFieldsWithoutExposeAnnotation()
                    .create()
                    .toJson(filmItem)
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val fileName = "${filmItem.getTitle().replace("\\W+".toRegex(), "_")}.json"
                val file = File(downloadsDir, fileName)
                try {
                    FileOutputStream(file).use { outputStream ->
                        outputStream.write(jsonString.toByteArray())
                    }
                    Toast.makeText(this, "Json file successfully saved to Downloads!", Toast.LENGTH_SHORT).show()
                } catch (e: IOException) {
                    Toast.makeText(this, "json file writing error\n${e.message}", Toast.LENGTH_LONG).show()
                }
                return true
            }
            R.id.action_import -> {
                val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                    type = "application/json"
                    addCategory(Intent.CATEGORY_OPENABLE)
                }
                startActivityForResult(Intent.createChooser(intent, "Select a JSON file"), PICK_JSON_REQUEST)
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    private fun onItemChanged(){
        if(!changed){
            changed = true
            toolbar.navigationIcon = ContextCompat.getDrawable(this, R.drawable.done_v_svgrepo_com)
        }
    }

    private fun startFailed(reason : Int){
        if(reason == 0){
            Toast.makeText(this, "Failed to create a new item in database", Toast.LENGTH_SHORT).show()
        }
        else{
            Toast.makeText(this, "Failed to start activity : $reason", Toast.LENGTH_SHORT).show()
        }
        finish()
    }

    override fun onDestroy() {
        if(toDelete){
            //Toast.makeText(this@FilmActivity, "del", Toast.LENGTH_SHORT).show()
            filmItem.deleteFilm(databaseUserRef, storageUserRef, this@FilmActivity, object : DatabaseSyncListener{
                override fun onSuccess() {}
                override fun onFailure(exception: java.lang.Exception) {}
            })
        }
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }

    override fun onPause() {
        super.onPause()
        if((changed || newFilm) && !toDelete){
            saveDataChange()
        }
    }

    private fun isValidUrl(url: String?): Boolean {
        return url != null && Patterns.WEB_URL.matcher(url).matches() && URLUtil.isValidUrl(url)
    }

    private fun searchForTitleInDatabase(title : String, resultsTextView: TextView, closeResultsButton : ImageButton){
        TmdbRetrofitClient.instance.searchMovies(TMDB_API_KEY, title).enqueue(object : Callback<TmdbSearchResponse>{
            override fun onResponse(call: Call<TmdbSearchResponse>, response: Response<TmdbSearchResponse>) {
                val items = response.body()?.results ?: listOf()
                if(items.isEmpty()){
                    Toast.makeText(this@FilmActivity,
                        "No results found in database.\nYou can enter item manually.",
                        Toast.LENGTH_LONG).show()
                }
                else{
                    resultsTextView.visibility = VISIBLE
                    resultsTextView.text = "There are ${items.size} results found in the database.\nClick here to review"
                    resultsTextView.visibility = VISIBLE
                    resultsTextView.setOnClickListener {
                        val intent = Intent(this@FilmActivity, TmdbSearchActivity::class.java)
                            .putExtra("response", response.body())
                            .putExtra("query", title)
                            .putExtra("pageNum", clickedPage)
                        startActivityForResult(intent, TMDB_ACTIVITY_REQUEST)
                    }
                    closeResultsButton.visibility = VISIBLE
                    closeResultsButton.setOnClickListener {
                        resultsTextView.visibility = GONE
                        closeResultsButton.visibility = GONE
                    }
                }
            }
            override fun onFailure(call: Call<TmdbSearchResponse>, t: Throwable) {
                Toast.makeText(this@FilmActivity,
                    "Search failed.\nYou can enter item manually.",
                    Toast.LENGTH_LONG).show()
            }
        })
    }

    private inner class TimeInputTextWatcher(
        private val currentEditText: EditText,
        private val nextEditText: EditText?,
        private val prevEditText: EditText?,
        private val type : Int
    ) : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            onItemChanged()
            if (currentEditText.text.length == 2) {
                check()
                toNext()
            }
        }
        override fun afterTextChanged(s: Editable?) {}
        private fun check(){
            val value = currentEditText.text.toString().toIntOrNull() ?: 0
            if (type == 3 || type == 4)
            {
                if (value > 59) {
                    currentEditText.setText("59")
                }
            }
            else if (type < 2 && value < 10){
                currentEditText.setText(value.toString())
            }
        }
        private fun toNext(){
            if(nextEditText != null) {
                nextEditText.requestFocus()
                if(type != 5) nextEditText.selectAll()
            }
            else closeKeyboard(currentEditText)
        }
    }

    private inner class MutableComment(
        private val season: EditText,
        private val series: EditText,
        private val hours: EditText,
        private val minutes: EditText,
        private val seconds: EditText,
        private val text: EditText,
        private val seasonText: TextView,
        private val seriesText: TextView
    ) : TimecodeInput(season, series, hours, minutes, seconds, seasonText, seriesText) {
        constructor(
            season: EditText, series: EditText, hours: EditText, minutes: EditText, seconds: EditText, text: EditText,
            seasonText: TextView, seriesText: TextView,
            prevSeason : Int, prevSeries : Int,
            timeToSet: Int, textToSet: String
        ) : this(season, series, hours, minutes, seconds, text, seasonText, seriesText){
            setTime(timeToSet)
            text.setText(textToSet)
            if( !filmItem.isASeries || (curSeason() == prevSeason && curSeries() == prevSeries)){
                setup(text, false)
            }
            else setup(text, true)
            text.addTextChangedListener {
                onItemChanged()
            }
        }
        fun getComment() : Comment {
            return Comment(getTime(), getText())
        }
        private fun getText() : String {
            return text.text.toString()
        }

        fun reset(prevSeason : Int, prevSeries : Int) {
            if( !filmItem.isASeries || (curSeason() == prevSeason && curSeries() == prevSeries)){
                setup(text, false)
            }
            else setup(text, true)
            text.addTextChangedListener {
                onItemChanged()
            }
        }
    }

    private open inner class TimecodeInput(
        private val season : EditText,
        private val series : EditText,
        private val hours : EditText,
        private val minutes : EditText,
        private val seconds : EditText,
        private val seasonText: TextView,
        private val seriesText: TextView
    ) {
        fun setup(text: EditText?, includeSeries : Boolean) {
            if(includeSeries){
                seriesText.text = "series"
                season.visibility = VISIBLE
                series.visibility = VISIBLE
                seriesText.visibility = VISIBLE
                seasonText.visibility = VISIBLE
            }
            else{
                season.visibility = GONE
                series.visibility = GONE
                seriesText.visibility = GONE
                seasonText.visibility = GONE
            }
            season.addTextChangedListener(TimeInputTextWatcher(season, series, null, 0))
            series.addTextChangedListener(TimeInputTextWatcher(series, hours, season, 1))
            hours.addTextChangedListener(TimeInputTextWatcher(hours, minutes, series,2))
            minutes.addTextChangedListener(TimeInputTextWatcher(minutes, seconds, hours, 3))
            seconds.addTextChangedListener(TimeInputTextWatcher(seconds, text, minutes, 4))
        }
        fun expandSeriesText() {
            seriesText.text = "Currently watching at :"
            seriesText.visibility = VISIBLE
        }
        fun setTime(timeToSet : Int) {
            var time = timeToSet
            var str = (time%60).toString()
            if(str.length == 1) str = "0$str"
            seconds.setText(str)
            time/=60
            str = (time%60).toString()
            if(str.length == 1) str = "0$str"
            minutes.setText(str)
            time/=60
            str = (time%100).toString()
            if(str.length == 1) str = "0$str"
            hours.setText(str)
            time/=100
            series.setText((time%100).toString())
            season.setText((time/100).toString())
        }
        fun getTime() : Int {
            var toReturn = season.text.toString().toIntOrNull() ?: 0
            toReturn *= 100
            toReturn += series.text.toString().toIntOrNull() ?: 0
            toReturn *= 100
            toReturn += hours.text.toString().toIntOrNull() ?: 0
            toReturn *= 60
            toReturn += minutes.text.toString().toIntOrNull() ?: 0
            toReturn *= 60
            toReturn += seconds.text.toString().toIntOrNull() ?: 0
            return toReturn
        }
        fun curSeason() : Int {
            return season.text.toString().toIntOrNull() ?: 0
        }
        fun curSeries() : Int {
            return series.text.toString().toIntOrNull() ?: 0
        }

    }
}