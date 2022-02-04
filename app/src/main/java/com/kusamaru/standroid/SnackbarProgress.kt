package com.kusamaru.standroid

import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import com.google.android.material.snackbar.Snackbar


class SnackbarProgress(val context: Context, val view: View, val message: String) {
    val snackbar = Snackbar.make(view, message, Snackbar.LENGTH_INDEFINITE)
    fun show() {
        val snackBer_viewGrop = snackbar.view.findViewById<View>(R.id.snackbar_text).parent as ViewGroup
        //SnackBerを複数行対応させる
        val snackBer_textView = snackBer_viewGrop.findViewById<View>(R.id.snackbar_text) as TextView
        snackBer_textView.maxLines = 2
        //複数行対応させたおかげでずれたので修正
        val progressBar = ProgressBar(context)
        val progressBer_layoutParams =
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        progressBer_layoutParams.gravity = Gravity.CENTER
        progressBar.setLayoutParams(progressBer_layoutParams)
        snackBer_viewGrop.addView(progressBar, 0)
        snackbar.show()
    }

    fun dismiss() {
        snackbar.dismiss()
    }

}
