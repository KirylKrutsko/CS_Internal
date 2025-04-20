package com.example.cs_internal

import android.graphics.*
import com.squareup.picasso.Transformation

//used to crop image in circle for profile image
class CircleTransformation() : Transformation {
    override fun transform(source: Bitmap): Bitmap? {
        if(source.config == null) return null
        val size = Math.min(source.height, source.width)
        val toReturnBitmap = Bitmap.createBitmap(size, size, source.config!!)
        val canvas = Canvas(toReturnBitmap)
        val paint = Paint()
        val shader = BitmapShader(source, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        paint.shader = shader
        paint.isAntiAlias = true
        canvas.drawCircle(size/2f, size/2f, size/2f, paint)
        source.recycle()
        return toReturnBitmap
    }

    override fun key(): String {
        return "circle"
    }

}