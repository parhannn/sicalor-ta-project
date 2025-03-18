package com.example.sicalor.utils

import android.content.Context
import java.io.File

fun createCustomTempFile(context: Context): File {
    val dir: File? = context.getExternalFilesDir(null)
    return File.createTempFile(
        "JPEG_${System.currentTimeMillis()}_",
        ".jpg",
        dir
    )
}