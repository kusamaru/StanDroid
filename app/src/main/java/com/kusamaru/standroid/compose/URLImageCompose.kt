package com.kusamaru.standroid.nicovideo.compose

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition

/**
 * GlideをComposeで使おう。
 * @param url URL
 * @return Bitmap
 * */
@Composable
fun getBitmapCompose(url: String): Bitmap? {
    // https://stackoverflow.com/questions/58594262/how-do-i-load-url-into-image-into-drawimage-in-compose-ui-android-jetpack
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    Glide.with(LocalContext.current).asBitmap().load(url).into(object : CustomTarget<Bitmap>() {
        override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
            bitmap = resource
        }

        override fun onLoadCleared(placeholder: Drawable?) {

        }
    })
    return bitmap
}