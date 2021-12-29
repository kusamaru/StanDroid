package io.github.takusan23.tatimidroid.googlecast

import android.content.Context
import androidx.core.net.toUri
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaLoadRequestData
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.framework.CastButtonFactory
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManagerListener
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.images.WebImage

class GoogleCast(var context: Context) {

    lateinit var castContext: CastContext
    //Castしたときに接続したや切断したなどが受け取れる。
    lateinit var sessionManagerListener: SessionManagerListener<CastSession>


    //番組のHLSアドレス、番組タイトル、サブタイトル、サムネイルなど
    var hlsAddress = ""
    var programTitle = ""
    var programSubTitle = ""
    var programThumbnail = ""

    fun init() {
        castContext = CastContext.getSharedInstance(context)
        sessionManagerListener = object : SessionManagerListener<CastSession> {
            override fun onSessionStarted(p0: CastSession?, p1: String?) {
                val mediaMetadata = MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE).apply {
                    putString(MediaMetadata.KEY_TITLE, programTitle)
                    putString(MediaMetadata.KEY_SUBTITLE, programSubTitle)
                    //その他にも addImage でアルバムカバー？画像？の設定が可能
                    addImage(WebImage(programThumbnail.toUri()))
                }
                val mediaInfo = MediaInfo.Builder(hlsAddress).apply {
                    setStreamType(MediaInfo.STREAM_TYPE_LIVE)
                    setContentType("videos/mp4")
                    setMetadata(mediaMetadata)
                }
                val mediaLoadRequestData = MediaLoadRequestData.Builder().apply {
                    setMediaInfo(mediaInfo.build())
                }
                val remoteMediaClient = p0?.remoteMediaClient
                remoteMediaClient?.load(mediaLoadRequestData.build())
            }

            override fun onSessionResumeFailed(p0: CastSession?, p1: Int) {

            }

            override fun onSessionSuspended(p0: CastSession?, p1: Int) {

            }

            override fun onSessionEnded(p0: CastSession?, p1: Int) {

            }

            override fun onSessionResumed(p0: CastSession?, p1: Boolean) {

            }

            override fun onSessionStarting(p0: CastSession?) {

            }

            override fun onSessionResuming(p0: CastSession?, p1: String?) {

            }

            override fun onSessionEnding(p0: CastSession?) {

            }

            override fun onSessionStartFailed(p0: CastSession?, p1: Int) {

            }

        }
    }

    // GooglePlay開発者サービスが有るか
    fun isGooglePlayServicesAvailable(): Boolean {
        return GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS
    }


    //ライフサイクルなど
    fun resume() {
        if (isGooglePlayServicesAvailable()) {
            castContext.sessionManager.addSessionManagerListener(
                sessionManagerListener,
                CastSession::class.java
            )
        }
    }

    fun pause() {
        if (isGooglePlayServicesAvailable()) {
            castContext.sessionManager.removeSessionManagerListener(
                sessionManagerListener,
                CastSession::class.java
            )
        }
    }

    //キャストボタン登録
    fun setUpCastButton(mediaRouteButton: androidx.mediarouter.app.MediaRouteButton) {
        if (isGooglePlayServicesAvailable()) {
            CastButtonFactory.setUpMediaRouteButton(
                context,
                mediaRouteButton
            )
        }
    }

}