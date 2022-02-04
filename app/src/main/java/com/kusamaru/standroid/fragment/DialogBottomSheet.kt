package com.kusamaru.standroid.fragment

import android.content.res.ColorStateList
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.kusamaru.standroid.R
import com.kusamaru.standroid.tool.DarkModeSupport
import com.kusamaru.standroid.tool.getThemeColor
import com.kusamaru.standroid.databinding.BottomFragmentDialogBinding

/** 引数なしのコンストラクタを用意しないとまれに落ちるらしい。本来引数じゃなくてargumentsで渡すもんだからな */
class DialogBottomSheet() : BottomSheetDialogFragment() {

    lateinit var description: String
    lateinit var buttonItems: ArrayList<DialogBottomSheetItem>
    lateinit var clickEvent: (Int, BottomSheetDialogFragment) -> Unit

    /** findViewById駆逐 */
    private val viewBinding by lazy { BottomFragmentDialogBinding.inflate(layoutInflater) }

    /**
     * BottomSheet版ダイアログを自作してみた。
     * @param description ダイアログの説明
     * @param buttonItems DialogBottomSheetItemの配列。
     * @param clickEvent クリック押したときのコールバック。引数は押した位置です。
     * */
    constructor(description: String, buttonItems: ArrayList<DialogBottomSheetItem>, clickEvent: (Int, BottomSheetDialogFragment) -> Unit) : this() {
        this.description = description
        this.buttonItems = buttonItems
        this.clickEvent = clickEvent
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (!::clickEvent.isInitialized) {
            return
        }
        // ダークモード
        val darkModeSupport = DarkModeSupport(requireContext())
        viewBinding.bottomFragmentDialogParent.background = ColorDrawable(getThemeColor(darkModeSupport.context))
        // 説明文
        viewBinding.bottomFragmentDialogDescriptionTextView.text = description
        // ボタン作成
        for (position in 0 until buttonItems.size) {
            val item = buttonItems[position]
            // 押したときのRippleつけたいがためにinflateしてる
            val layout = layoutInflater.inflate(R.layout.textview_ripple, null)
            val textView = (layout as TextView).apply {
                text = item.title
                if (item.icon != -1) {
                    setCompoundDrawablesWithIntrinsicBounds(context?.getDrawable(item.icon), null, null, null)
                }
                if (item.textColor != -1) {
                    setTextColor(item.textColor)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        compoundDrawableTintList = ColorStateList.valueOf(item.textColor)
                    }
                }
                setOnClickListener {
                    // 高階関数
                    clickEvent(position, this@DialogBottomSheet)
                    // 閉じる
                    dismiss()
                }
            }
            viewBinding.bottomFragmentDialogLinearlayout.addView(textView)
        }
    }

    // ボタンのテキスト、アイコンなど。IconとtextColorは無指定では-1（設定しない）になります。
    data class DialogBottomSheetItem(val title: String, val icon: Int = -1, val textColor: Int = -1)

}