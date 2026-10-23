package com.example.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher

object GalleryPickerHelper {

    fun launchImagePicker(context: Context, launcher: ActivityResultLauncher<Intent>) {
        val pickIntent = Intent(Intent.ACTION_PICK).apply {
            setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*")
        }
        val getContentIntent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        val chooserIntent = Intent.createChooser(pickIntent, "Select QR Code Image").apply {
            putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(getContentIntent))
        }

        try {
            launcher.launch(chooserIntent)
        } catch (e1: Exception) {
            try {
                launcher.launch(getContentIntent)
            } catch (e2: Exception) {
                try {
                    launcher.launch(pickIntent)
                } catch (e3: Exception) {
                    Toast.makeText(context, "Could not open photo gallery: ${e3.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
