package com.hackathon.fileexplorer


import android.media.ImageReader
import java.io.File
import java.net.URLConnection

fun File.getChildCount()=
    this.listFiles().size


fun File.isImage() : Boolean{
    val mimetype = URLConnection.guessContentTypeFromName(this.path)
    return mimetype!=null && mimetype.startsWith(prefix="image",ignoreCase = true)
}

fun File.getType() = URLConnection.guessContentTypeFromName(this.path)

fun File.checkDuplicateIn(files : List<File>) : Boolean{
   return false
}