package com.kusamaru.standroid.googlecast

import android.content.Context
import com.google.android.gms.cast.CastMediaControlIntent
import com.google.android.gms.cast.framework.CastOptions
import com.google.android.gms.cast.framework.OptionsProvider
import com.google.android.gms.cast.framework.SessionProvider

class CastOptionsProvider : OptionsProvider {
    override fun getCastOptions(p0: Context?): CastOptions {
        // 5ドル払わないので受信側をデフォルトにする。
        // Default Media Receiver って名前になる？
        val id = CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID
        val castOptions = CastOptions.Builder()
            .setReceiverApplicationId(id)
            .build()
        return castOptions
    }
    override fun getAdditionalSessionProviders(p0: Context?): MutableList<SessionProvider>? {
        return null
    }
}