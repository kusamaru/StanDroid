package com.kusamaru.standroid.tool

import android.os.Build
import android.view.View
import android.view.Window
import android.view.WindowInsets
import android.view.WindowInsetsController

/**
 * ステータスバーを非表示にしたりするための関数たち。
 * */
object SystemBarVisibility {

    /**
     * システムバー（ステータスバー、ナビゲーションバー）を 非表示 にする関数
     *
     * Android 11 Support !!!
     */
    fun hideSystemBar(window: Window) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11 以上と分岐
            window.insetsController?.apply {
                // スワイプで一時的に表示可能
                systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                // StatusBar + NavigationBar 非表示
                hide(WindowInsets.Type.systemBars())
            }
        } else {
            // Android 10 以前。
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION

        }
    }

    /**
     * システムバー（ステータスバー、ナビゲーションバー）を 表示 する関数
     *
     * Android 11 Support !!!
     */
    fun showSystemBar(window: Window) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11 以上と分岐
            window.insetsController?.apply {
                systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_BARS_BY_TOUCH
                show(WindowInsets.Type.systemBars())
            }
        } else {
            // Android 10 以前。
            window.decorView.systemUiVisibility = 0
        }
    }

}