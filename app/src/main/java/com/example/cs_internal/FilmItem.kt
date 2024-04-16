package com.example.cs_internal

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.widget.Adapter
import android.widget.ImageView
import android.widget.Toast
import com.google.firebase.database.*
import com.google.firebase.storage.StorageReference
import com.squareup.picasso.Picasso
import org.xml.sax.DTDHandler
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.Serializable
import java.util.*
import kotlin.collections.HashMap

class FilmItem(
    private var title: String,
    private var description: String,
    private var rating: String,
    private var type: Int,
    private var imageLink : String,
    private var databaseRef : String
) : Serializable {
//    var impression : String? = null
//    var mark : Int? = null

    constructor(title: String, description: String, rating: String, type: Int, imageLink: String, databaseUserRef: DatabaseReference, listener: DatabaseSyncListener) : this(title, description, rating, type, imageLink, "") {
        addFilmToDatabase(databaseUserRef, listener)
    }

    private fun toHashMap() : HashMap<String, *>{
        return hashMapOf(
            "title" to title,
            "desc" to description,
            "rating" to rating,
            "imageLink" to imageLink,
            "type" to type
        )
    }

    private fun addFilmToDatabase(databaseUserRef : DatabaseReference, listener: DatabaseSyncListener){
        val toAddRef = databaseUserRef.push()
        if(toAddRef.key == null){
            listener.onFailure(Exception("Null database response"))
        } else {
            databaseRef = toAddRef.key!!
            toAddRef.setValue(this.toHashMap()).addOnSuccessListener {
                listener.onSuccess()
            }.addOnFailureListener {
                listener.onFailure(Exception("Database setValue request rejected"))
            }
        }
    }

    fun deleteFilm(databaseUserRef: DatabaseReference, storageUserRef: StorageReference, context: Context, listener: DatabaseSyncListener){
        deleteFilmFromDatabase(databaseUserRef, object : DatabaseSyncListener{
            override fun onSuccess() {
                deleteImageFromStorage(storageUserRef, object  : DatabaseSyncListener{
                    override fun onSuccess() {
                        val editor = context.getSharedPreferences("FilmImages", Context.MODE_PRIVATE).edit()
                        editor.remove(databaseRef)
                        editor.apply()
                        listener.onSuccess()
                    }
                    override fun onFailure(exception: java.lang.Exception) {
                        listener.onFailure(exception)
                    }
                })
            }
            override fun onFailure(exception: Exception) {
                listener.onFailure(exception)
            }
        })
    }

    fun deleteImage(imageView: ImageView, databaseUserRef: DatabaseReference, storageUserRef: StorageReference, context: Context, listener: DatabaseSyncListener){
        deleteImageFromStorage(storageUserRef, object : DatabaseSyncListener{
            override fun onSuccess() {
                databaseUserRef.child("$databaseRef/imageLink").setValue("").addOnSuccessListener {
                    this@FilmItem.imageLink = ""
                    val editor = context.getSharedPreferences("FilmImages", Context.MODE_PRIVATE).edit()
                    editor.remove(databaseRef)
                    editor.apply()
                    imageView.setImageResource(R.drawable.outline_image_24)
                    listener.onSuccess()
                }.addOnFailureListener{
                    listener.onFailure(Exception("Database setValue request rejected"))
                }
            }
            override fun onFailure(exception: Exception) {
                listener.onFailure(exception)
            }
        })
    }

    private fun deleteFilmFromDatabase(databaseUserRef: DatabaseReference, listener: DatabaseSyncListener) {
        databaseUserRef.child(databaseRef).removeValue().addOnSuccessListener {
            listener.onSuccess()
        }.addOnFailureListener {
            listener.onFailure(Exception("Database removeValue request rejected"))
        }
    }

    fun setImageByURI(localUri: Uri, imageView: ImageView, context: Context, storageUserRef : StorageReference, databaseUserRef: DatabaseReference, listener: DatabaseSyncListener){
        Picasso.get().load(localUri).into(object : com.squareup.picasso.Target {
            override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) {
                bitmap?.let {
                    val compressedImage = compressImage(context, bitmap)
                    val imageRef = storageUserRef.child("film_images/${UUID.randomUUID()}.jpg")
                    imageRef.putBytes(compressedImage).addOnSuccessListener {
                        deleteImageFromStorage(storageUserRef, object : DatabaseSyncListener{
                            override fun onSuccess() {
                                databaseUserRef.child("$databaseRef/imageLink")
                                    .setValue(imageRef.name).addOnSuccessListener {
                                        imageLink = imageRef.name
                                        downloadAndLoadImageFromStorage(imageView, storageUserRef, context, listener)
                                    }.addOnFailureListener(){
                                        listener.onFailure(Exception("Database setValue request rejected"))
                                    }
                            }
                            override fun onFailure(exception: Exception) {
                                listener.onFailure(exception)
                            }
                        })
                    }.addOnFailureListener {
                        listener.onFailure(Exception("Storage putBytes request rejected"))
                    }
                }
            }
            override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {
                listener.onFailure(Exception("Failed to prepare image bitmap for uploading"))
            }
            override fun onPrepareLoad(placeHolderDrawable: Drawable?) {}
        })
    }

    private fun compressImage(context: Context, bitmap: Bitmap) : ByteArray{
        val toReturn = ByteArrayOutputStream()
        val k = maxOf(bitmap.width.toFloat(), bitmap.height.toFloat()) / 1024
        if(k>1){
            Toast.makeText(context, "Your image will be compressed for saving into database", Toast.LENGTH_SHORT).show()
            val scaledBitmap = Bitmap.createScaledBitmap(bitmap,(bitmap.width / k).toInt(), (bitmap.height / k).toInt(), true)
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 100, toReturn)
        }
        else{
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, toReturn)
        }
        return toReturn.toByteArray()
    }

    private fun deleteImageFromStorage(storageUserRef: StorageReference, listener: DatabaseSyncListener){
        if(imageLink !=""){
            storageUserRef.child("film_images/$imageLink").delete().addOnSuccessListener {
                listener.onSuccess()
            }.addOnFailureListener {
                listener.onFailure(Exception("Storage delete request rejected"))
            }
        } else {
            listener.onSuccess()
        }
    }

    private fun downloadAndLoadImageFromStorage(imageView: ImageView, storageUserRef: StorageReference, context: Context, listener: DatabaseSyncListener){
        if(imageLink.isNotEmpty()){
            val imageRef = storageUserRef.child("film_images/$imageLink")
            val file = File(context.cacheDir, databaseRef)
            imageRef.getFile(file)
                .addOnSuccessListener {
                    val editor = context.getSharedPreferences("FilmImages", Context.MODE_PRIVATE).edit()
                    editor.putString(databaseRef, file.toURI().toString())
                    editor.apply()
                    if(loadImageLocally(imageView, context)){
                        listener.onSuccess()
                    }
                    else{
                        listener.onFailure(Exception("Device local memory error"))
                    }
                }
                .addOnFailureListener {
                    listener.onFailure(Exception("Storage getFile request rejected"))
                }
        }
        else{
            listener.onSuccess()
        }
    }

    fun changeType(toMove : Int, databaseUserRef: DatabaseReference, listener: DatabaseSyncListener){
            databaseUserRef.child("$databaseRef/type").setValue(toMove).addOnSuccessListener {
                type = toMove
                listener.onSuccess()
            }.addOnFailureListener{
                listener.onFailure(Exception("Database setValue request rejected"))
            }
    }

    fun loadImageIfExists(imageView : ImageView, context: Context, storageUserRef: StorageReference, listener: DatabaseSyncListener){
        if(imageLink.isNotEmpty()){
            if(loadImageLocally(imageView, context)){
                listener.onSuccess()
            }
            else {
                downloadAndLoadImageFromStorage(imageView, storageUserRef, context, listener)
            }
        }
        else{
            listener.onSuccess()
        }
    }

    private fun loadImageLocally(imageView: ImageView, context : Context) : Boolean {
        val sharedPreferences = context.getSharedPreferences("FilmImages", Context.MODE_PRIVATE)
        val savedImageUriString = sharedPreferences.getString(databaseRef, null)
        if (savedImageUriString != null) {
            val savedImageUri = Uri.parse(savedImageUriString)
            Toast.makeText(context, savedImageUri.toString(), Toast.LENGTH_SHORT).show()
//                            imageView.setImageURI(savedImageUri)
            Picasso.get().invalidate(savedImageUri)
            Picasso.get().load(savedImageUri).into(imageView)
            return true
        }
        return false
    }

    fun changeData(enteredTitle : String, enteredDesc : String, enteredRating : String, databaseUserRef: DatabaseReference, listener: DatabaseSyncListener){
        databaseUserRef.child(databaseRef).runTransaction(object : Transaction.Handler{
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                if(enteredTitle!=title){
                    title = enteredTitle
                    currentData.child("title").value = enteredTitle
                }
                if(enteredDesc!=description){
                    description = enteredDesc
                    currentData.child("desc").value = enteredDesc
                }
                if(enteredRating!=rating){
                    rating = enteredRating
                    currentData.child("rating").value = enteredRating
                }
                return Transaction.success(currentData)
            }
            override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
                if (error != null) {
                    listener.onFailure(Exception(error.message))
                } else if (!committed) {
                    listener.onFailure(Exception("Transaction request rejected"))
                } else {
                    listener.onSuccess()
                }
            }
        })
    }

    fun getTitle() : String{
        return title
    }
    fun getDescription() : String{
        return description
    }
    fun getRating() : String{
        return rating
    }
    fun getDatabaseRef() : String{
        return databaseRef
    }
}