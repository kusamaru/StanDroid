package io.github.takusan23.tatimidroid.tool

import androidx.constraintlayout.motion.widget.MotionLayout

/**
 * MotionLayout関係で複数の個所で使いそうな関数たち
 * */
object MotionLayoutTool {

    /**
     * MotionLayoutの遷移をすべて 有効/無効 にする関数
     * @param motionLayout MotionLayout
     * @param isEnable 有効にするならtrue
     * */
    fun allTransitionEnable(motionLayout: MotionLayout, isEnable: Boolean) {
        motionLayout.definedTransitions.forEach { transition ->
            transition.setEnable(isEnable)
        }
    }

    /**
     * MotionLayoutで使用するViewを表示/非表示にする関数
     * @param motionLayout MotionLayout
     * @param targetViewId 表示を切り替えたいViewのId
     * @param visibility [android.view.View.VISIBLE]等を参照
     * */
    fun setMotionLayoutViewVisible(motionLayout: MotionLayout, targetViewId: Int, visibility: Int) {
        motionLayout.constraintSetIds.forEach { id ->
            motionLayout.getConstraintSet(id).setVisibility(targetViewId, visibility)
        }
    }

}