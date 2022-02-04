package com.kusamaru.standroid.tool

import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.core.view.GestureDetectorCompat

/**
 * [View.setOnClickListener]があるのにダブルタップ版が無いのは不公平ってことで作りました。
 *
 * ダブルタップリスナー拡張関数。
 *
 * でもこれつけると[View.setOnClickListener]がおかしくなるのでisDoubleClickで判断してください。
 *
 * 多分 [View.setClickable] [View.setFocusable] を [true] にしないとうまく行かないかも
 *
 * @param click ダブルタップかタップした時に呼ばれる高階関数。isDoubleClickがtrueならダブルタップです。falseならクリック
 * */
fun View.setOnDoubleClickListener(click: (MotionEvent?, isDoubleClick: Boolean) -> Unit) {
    val gestureDetector = GestureDetectorCompat(this.context, object : GestureDetector.SimpleOnGestureListener() {})
    gestureDetector.setOnDoubleTapListener(object : GestureDetector.OnDoubleTapListener {
        override fun onDoubleTap(p0: MotionEvent?): Boolean {
            // ダブルタップ時
            click(p0, true)
            return true
        }

        override fun onDoubleTapEvent(p0: MotionEvent?): Boolean {
            return true
        }

        override fun onSingleTapConfirmed(p0: MotionEvent?): Boolean {
            click(p0, false)
            return true
        }
    })
    // これ忘れんな
    this.setOnTouchListener { view, motionEvent ->
        gestureDetector.onTouchEvent(motionEvent)
    }
}