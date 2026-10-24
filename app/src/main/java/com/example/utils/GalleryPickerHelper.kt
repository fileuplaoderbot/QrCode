package com.example.utils

import android.content.Context
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts

object GalleryPickerHelper {

    fun launchImagePicker(
        context: Context,
        pickMediaLauncher: ActivityResultLauncher<PickVisualMediaRequest>,
        getContentLauncher: ActivityResultLauncher<String>
    ) {
        try {
            pickMediaLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        } catch (e1: Exception) {
            try {
                getContentLauncher.launch("image/*")
            } catch (e2: Exception) {
                Toast.makeText(
                    context,
                    "Could not open photo gallery: ${e2.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
