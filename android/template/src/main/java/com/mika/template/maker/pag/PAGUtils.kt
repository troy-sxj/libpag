package com.mika.template.maker.pag

import android.content.Context
import android.graphics.BitmapFactory
import org.libpag.PAGImage
import java.io.IOException
import java.io.InputStream

/**
 * Author: shangxiaojian
 * Date: 2022/1/20 10:37 上午
 */
object PAGUtils {

    fun createPAGImage(context: Context, imgPath: String): PAGImage? {
        var stream: InputStream? = null
        try {
            stream = context.assets.open(imgPath)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        val bitmap = BitmapFactory.decodeStream(stream) ?: return null
        return PAGImage.FromBitmap(bitmap)
    }

}