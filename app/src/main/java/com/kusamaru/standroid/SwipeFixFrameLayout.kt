package com.kusamaru.standroid

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.core.view.children
import androidx.core.view.isVisible

/**
 * 詳しくは：https://github.com/takusan23/MotionLayoutSwipeFixFrameLayout
 * */
class SwipeFixFrameLayout(context: Context, attributeSet: AttributeSet? = null) : FrameLayout(context, attributeSet) {
    /** ドラッグする（スワイプに設定した）View。 */
    var swipeTargetView: View? = null

    /** MotionLayoutの状態を知るためにMotionLayoutが必要 */
    var motionLayout: MotionLayout? = null

    /**
     * 強制的にクリックを渡す時に使う。ここで指定しない場合、MotionLayout傘下にRecyclerView等が有ってもスクロール出来ない可能性があります。
     * そのためのこの配列。
     *
     * MotionLayoutのConstraintSetで指定したIDを引数に入れることでそのIDの状態ならタッチを渡すことが出来ます。
     *
     * ここで入れたIDとタッチ中MotionLayoutの状態を取得して、一致している場合はタッチを特別に渡します。
     *
     * もし`<onSwipe>`が動作してしまう場合は`<onSwipe>`に`touchRegionId`を足してみてください。
     * */
    val allowIdList = arrayListOf<Int>()

    /**
     * [swipeTargetView]の上にViewを重ねた場合、重ねたViewと同時に[onSwipeTargetViewClickFunc]が呼ばれてしまう。
     *
     * [onSwipeTargetViewDoubleClickFunc]を呼ばずに、重ねたViewのクリックイベントのみを取る場合はまずこの配列にViewを追加してください。
     *
     * そうすることで、重ねたViewの[View.setOnClickListener]が呼ばれ、[onSwipeTargetViewClickFunc]は呼ばれなくなります。
     *
     * あと、[blockViewList]が押されたときに呼ばれる[onBlockViewClickFunc]なんてものもありますので、[View.setOnClickListener]が動かないときなどで使ってみてください。
     *
     * もし、MotionLayoutを動かしたくない場合は、重ねるViewに[View.isClickable]を[false]にすることで、MotionLayoutも動かなくなります。
     * */
    var blockViewList = arrayListOf<View>()

    /**
     * [swipeTargetView]のクリックイベント
     *
     * 注意：ほんのちょっとだけ遅延させてから高階関数を呼んでいます。理由はこの上（子のView）のクリックイベントがうまく処理できないため。
     * */
    var onSwipeTargetViewClickFunc: ((event: MotionEvent?) -> Unit)? = null

    /** [onSwipeTargetViewClickFunc]を呼ぶまでどれぐらい遅延させるか。 */
    var onSwipeTargetViewClickFuncDelayMs = 100L

    /**
     * [onSwipeTargetViewClickFunc]のダブルクリック版。
     *
     * 動画のスキップにどうぞ
     * */
    var onSwipeTargetViewDoubleClickFunc: ((motionEvent: MotionEvent?) -> Unit)? = null

    /**
     * タブルタップと判定する間隔。一回目と二回目のタップの時間（ミリ秒）を引いてこの変数以下ならタブルタップとして扱います。
     * */
    var DOUBLE_TAP_MAX_DURATION_MS = 300

    /**
     * ダブルタップ検出用。最後にタッチした時間が入っている。
     * */
    private var lastTouchTime = -1L

    /**
     * クリック位置が[blockViewList]に入れたViewと同じ場合はこの高階関数が呼ばれます
     *
     * 引数のViewには[blockViewList]の中から一致したViewが入っています。[View.getId]を利用することで分岐できると思います。
     *
     * */
    var onBlockViewClickFunc: ((view: View?, event: MotionEvent?) -> Unit)? = null

    /**
     * 子のViewへタッチイベントを渡すかどうか
     * */
    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {

        if (swipeTargetView != null && ev != null && motionLayout != null) {

            // タッチがswipeTargetViewの中にあるときのみタッチイベントを渡す
            val isTouchingSwipeTargetView = ev.x > swipeTargetView!!.left && ev.x < swipeTargetView!!.right && ev.y > swipeTargetView!!.top && ev.y < swipeTargetView!!.bottom
            if (isTouchingSwipeTargetView) {

                // swipeTargetViewの上に重ねたViewのクリック判定
                val blockTouchList = blockViewList
                    .filter { view -> view.isEnabled && view.isVisible }
                    .filter { blockView ->
                        val screenPos = getViewPositionOnScreen(blockView)
                        // View#rawXとかは画面から見ての座標。View#yは親のViewから見ての座標
                        return@filter ev.rawX > screenPos.left && ev.rawX < screenPos.right && ev.rawY > screenPos.top && ev.rawY < screenPos.bottom
                    }

                // クリックさせるなど
                if (ev.action == MotionEvent.ACTION_UP) {
                    // MotionLayoutにはクリックを渡さないけど、指定したViewのときにはクリックを渡したい場合。
                    if (blockTouchList.isNotEmpty()) {
                        // 条件にあったBlockViewがあれば
                        postDelayed({
                            // MotionLayoutへタッチを渡さないけど押したViewのクリックは渡す
                            blockTouchList.forEach { blockView ->
                                onBlockViewClickFunc?.invoke(blockView, ev) // 代わりの関数で代替する
                            }
                        }, onSwipeTargetViewClickFuncDelayMs)
                    } else {
                        postDelayed({
                            // タブルタップの処理
                            if (lastTouchTime != -1L && (System.currentTimeMillis() - lastTouchTime) <= DOUBLE_TAP_MAX_DURATION_MS) {
                                // タブルタップとして扱う
                                onSwipeTargetViewDoubleClickFunc?.invoke(ev)
                                lastTouchTime = -1
                            } else {
                                // もういっかい！もういっかい！
                                lastTouchTime = System.currentTimeMillis()
                            }
                            // 普通のクリック
                            onSwipeTargetViewClickFunc?.invoke(ev)
                        }, onSwipeTargetViewClickFuncDelayMs)
                    }
                }

                // 指定したViewを動かしている場合は渡す
                return super.onInterceptTouchEvent(ev)
            } else if (allowIdList.contains(motionLayout!!.currentState)) {
/*
                // タッチイベントを渡すことが許可されているIDなら渡す
                if (!isTouchingSwipeTargetView) {
                    // swipeTargetId以外ではMotionLayout動かさない。のでTransitionを無効にする
                    motionLayout!!.definedTransitions.forEach { transition ->
                        transition.setEnable(false)
                    }
                }
*/
                return super.onInterceptTouchEvent(ev)
            } else if (motionLayout!!.progress != 0f && motionLayout!!.progress != 1f) {
                // もしMotionLayout進行中なら
                return super.onInterceptTouchEvent(ev)
            } else {
                return true
            }

        } else {
            Log.e(this::class.java.simpleName, "swipeTargetView もしくは motionLayout が null です。")
            Log.e(this::class.java.simpleName, "SwipeTargetViewとMotionLayoutがnull以外になっていることを確認してください。")
            return super.onInterceptTouchEvent(ev)
        }
    }

    /**
     * 再帰的に子Viewを取得していく関数。ViewGroupがあれば取得。。。を続ける
     * @param viewGroup 取得したい親ViewGroup
     * @return 親ViewGroupに入っているすべてのView。
     * */
    fun getChildViewRecursive(viewGroup: ViewGroup): ArrayList<View> {
        // 結果が入る
        val resultChildView = arrayListOf<View>()

        // 再帰的に呼ぶ関数
        fun forEachFunc(childView: View) {
            // Viewを追加していく
            resultChildView.add(childView)
            // ViewGroupならまたforEachへ
            if (childView is ViewGroup) {
                childView.children.toList().forEach(::forEachFunc)
            }
        }
        // 最初
        viewGroup.children.toList().forEach(::forEachFunc)
        return resultChildView
    }

    /**
     * [getChildViewRecursive]で取得したViewの中から、[View.isClickable]がtrueの物を取り出し、すべて[blockViewList]に追加する関数
     * @param viewGroup 親ViewGroup
     * */
    fun addAllIsClickableViewFromParentView(viewGroup: ViewGroup) {
        val isClickableViewList = getChildViewRecursive(viewGroup).filter { view -> view.isClickable }
        blockViewList.addAll(isClickableViewList)
    }

    /**
     * 画面から見て、Viewがどの位置にあるかを返す関数。
     *
     * [View.getBottom]は、親要素から見ての位置なので、全体から見ての座標ではない
     *
     * @param view 座標を知りたいView
     * @return Rect。[Rect.bottom]等で座標を取得することが出来ます。
     * */
    private fun getViewPositionOnScreen(view: View): Rect {
        val intArray = IntArray(2)
        view.getLocationOnScreen(intArray)
        val left = intArray[0]
        val top = intArray[1]
        val right = left + view.width
        val bottom = top + view.height
        return Rect(left, top, right, bottom)
    }

}