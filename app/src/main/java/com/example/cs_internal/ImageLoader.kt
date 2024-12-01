package com.example.cs_internal

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.core.graphics.scale
import androidx.core.net.toUri
import com.google.firebase.storage.StorageException
import com.google.firebase.storage.StorageReference
import com.squareup.picasso.Picasso
import com.squareup.picasso.Target
import java.io.ByteArrayOutputStream
import java.io.File
import java.lang.Exception

class ImageLoader(){
    fun compressImage(bitmap: Bitmap, size : Int) : ByteArray {
        val toReturn = ByteArrayOutputStream()
        val k = maxOf(bitmap.width.toFloat(), bitmap.height.toFloat()) / size
        // coefficient by which the bitmap needs to be compressed to fit in the size bounds
        if(k>1){
            val scaledBitmap = Bitmap.createScaledBitmap(bitmap,(bitmap.width / k).toInt(), (bitmap.height / k).toInt(), true)
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 100, toReturn)
        }
        else{
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, toReturn)
        }
        return toReturn.toByteArray()
    }

    fun loadProfileImage(userId: String, storageUserRef: StorageReference, view : View, context: Context) {
        val sharedPreferences = context.getSharedPreferences("profile_image", Context.MODE_PRIVATE)
        val imagePath = sharedPreferences.getString(userId, null)
        if(imagePath != null){
            val imageUri = Uri.parse(imagePath)
            loadUriIntoView(imageUri, view, context)
        }
        else{
            val file = File(context.cacheDir, userId)
            storageUserRef.child("profile_image").getFile(file).addOnSuccessListener {
                loadUriIntoView(file.toUri(), view, context)
                sharedPreferences.edit().putString(userId, file.toURI().toString()).apply()
            }.addOnFailureListener { ex->
                if(ex !is StorageException || ex.errorCode != StorageException.ERROR_OBJECT_NOT_FOUND){
                    Toast.makeText(context, "Failed to download profile image", Toast.LENGTH_SHORT).show()
                    Toast.makeText(context, ex.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun loadUriIntoView(imageUri : Uri, view : View, context: Context){
        if(view is ImageView) {
            Picasso.get().invalidate(imageUri)
            Picasso.get().load(imageUri).transform(CircleTransformation()).into(view)
        }
        else if (view is Toolbar){
            Picasso.get().invalidate(imageUri)
            Picasso.get().load(imageUri).transform(CircleTransformation()).into(object : Target{
                override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) {
                    if(bitmap != null){
                        view.navigationIcon = BitmapDrawable(context.resources, bitmap.scale(100, 100, false))
                    }
                }
                override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {
                    Toast.makeText(context, "Failed to prepare bitmap", Toast.LENGTH_SHORT).show()
                    Toast.makeText(context, e?.message, Toast.LENGTH_LONG).show()
                }
                override fun onPrepareLoad(placeHolderDrawable: Drawable?) {}
            })
        }
    }
}