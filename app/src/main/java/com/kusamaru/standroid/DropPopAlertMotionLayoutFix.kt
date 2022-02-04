package com.kusamaru.standroid

import android.view.View
import android.view.animation.AnimationUtils
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.core.view.isVisible
import io.github.takusan23.droppopalert.DropPopAlertInterface
import java.util.*
import kotlin.concurrent.schedule

class DropPopAlertMotionLayoutFix(private val view: View) : DropPopAlertInterface {

    init {
        view.isVisible = false
    }

    /** 表示時間 */
    var showTimeMs = 2000L

    /** MotionLayoutは非表示にするのに一手間かかる */
    var motionLayout: MotionLayout? = null

    private var timer = Timer()

    companion object {
        /** 上から降りてくる */
        const val ALERT_DROP = 1

        /** 下から出てくる */
        const val ALERT_UP = 2
    }

    /**
     * アニメーションさせながら表示する関数
     * @param position [ALERT_DROP]等参照
     * */
    override fun alert(position: Int) {
        // タイマー停止
        timer.cancel()
        timer = Timer()
        // 表示
        showAlert(position)
        timer.schedule(showTimeMs) {
            // UIスレッドではないため
            view.post {
                hideAlert(position)
            }
        }
    }

    /**
     * 表示のみ。非表示にはしない。
     * @param position [ALERT_DROP]等参照
     * */
    override fun showAlert(position: Int) {
        val animation = when (position) {
            ALERT_UP -> {
                AnimationUtils.loadAnimation(view.context, R.anim.drop_pop_alert_up_start_anim)
            }
            else -> {
                AnimationUtils.loadAnimation(view.context, R.anim.drop_pop_alert_drop_start_anim)
            }
        }
        view.startAnimation(animation)
        if (motionLayout != null) {
            motionLayoutViewSetVisible(view, View.VISIBLE)
        } else {
            view.isVisible = true
        }
    }

    /**
     * 非表示のみ。
     * @param position [ALERT_DROP]等参照
     * */
    override fun hideAlert(position: Int) {
        val animation = when (position) {
            ALERT_UP -> {
                AnimationUtils.loadAnimation(view.context, R.anim.drop_pop_alert_up_end_anim)
            }
            else -> {
                AnimationUtils.loadAnimation(view.context, R.anim.drop_pop_alert_drop_end_anim)
            }
        }
        view.startAnimation(animation)
        if (motionLayout != null) {
            motionLayoutViewSetVisible(view, View.GONE)
        } else {
            view.isVisible = false
        }
    }

    /** MotionLayout版setVisibility */
    private fun motionLayoutViewSetVisible(view: View, visibility: Int) {
        motionLayout?.constraintSetIds?.forEach { id ->
            motionLayout?.getConstraintSet(id)?.setVisibility(view.id, visibility)
        }
    }

}

/**
 * Kotlinユーザーのための拡張関数版
 *
 * なお強制的にVisibilityがGONEになります。
 *
 * 拡張関数使っていいかは各自違うと思う
 * */
fun View.toDropPopAlertMotionLayoutFix(): DropPopAlertMotionLayoutFix {
    return DropPopAlertMotionLayoutFix(this)
}