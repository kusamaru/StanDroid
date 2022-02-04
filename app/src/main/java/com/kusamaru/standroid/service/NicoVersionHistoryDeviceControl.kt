package com.kusamaru.standroid.service

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.controls.Control
import android.service.controls.ControlsProviderService
import android.service.controls.DeviceTypes
import android.service.controls.actions.ControlAction
import android.service.controls.actions.FloatAction
import android.service.controls.templates.RangeTemplate
import androidx.annotation.RequiresApi
import java.util.concurrent.Flow
import java.util.function.Consumer

/**
 * Android 11のDevice Controlを使ったおまけ
 * 今回はスライダー兼押せるコントロールでニコ動のバージョンを遡れるように
 * 参考元：https://dic.nicovideo.jp/a/ニコニコ動画の変遷
 * */
@RequiresApi(Build.VERSION_CODES.R)
class NicoVersionHistoryDeviceControl : ControlsProviderService() {

    // 追加するデバイスのID
    val NICO_VERSION_HISTORY_CONTROL_ID = "nico_version_history_control_id"

    // ニコニコ動画の変遷
    val NICO_HISTORY_NAME = arrayListOf(
        "（仮）",
        "（β）",
        "（γ）",
        "（RC）",
        "（RC2）",
        "（SP1）",
        "（夏）",
        "（秋）",
        "（冬）",
        "（ββ)",
        "（9）",
        "（原宿）",
        "Zero",
        "Q",
        "GINZA",
        "(く)",
        "(Re)",
    )

    /**
     * ニコニコ動画の変遷
     * こっちは期間
     * */
    val NICO_HISTORY_DURATION = arrayListOf(
        "06/12/12 - 07/01/15",
        "07/01/15 - 07/02/24",
        "07/03/06 - 07/06/18",
        "07/06/18 - 07/10/10",
        "07/10/10 - 08/03/05",
        "08/03/05 - 08/07/05",
        "08/07/05 - 08/10/01",
        "08/10/01 - 08/12/05",
        "08/12/05 - 08/12/12",
        "08/12/12 - 09/10/29",
        "09/10/29 - 10/10/29",
        "10/10/29 - 12/04/30",
        "12/05/01 - 12/10/17",
        "12/10/18 - 13/10/07",
        "13/10/08 - 18/06/27",
        "18/06/28 - 20/08/09",
        "20/08/09 - 現行",
    )


    /**
     * Flowわからん
     * */
    lateinit var flow: Flow.Subscriber<in Control>

    /**
     * ユーザーがデバイスコントロールを選択する際に一覧に出すコントロールをここで用意する
     * */
    override fun createPublisherForAllAvailable(): Flow.Publisher<Control> {
        return Flow.Publisher { subscriber ->
            subscriber.onNext(getSliderControl())
            subscriber.onComplete()
        }
    }


    /**
     * デバイスコントロール一覧で表示するコントロールを用意
     * */
    override fun createPublisherFor(p0: MutableList<String>): Flow.Publisher<Control> {
        // 何してるかよくわからんな
        return Flow.Publisher<Control> {
            flow = it
            it.onSubscribe(object : Flow.Subscription {
                override fun request(p0: Long) {

                }

                override fun cancel() {

                }
            })
            it.onNext(getSliderControl())
        }
    }

    /**
     * コントローラーを押した時
     * */
    override fun performControlAction(p0: String, p1: ControlAction, p2: Consumer<Int>) {
        // システムに処理中とおしえる
        p2.accept(ControlAction.RESPONSE_OK)
        // スライダーいじった時
        if (p1 is FloatAction) {
            flow.onNext(getSliderControl(p1.newValue))
        }
    }

    /**
     * スライダーコントロールを作成する
     * @param currentValue スライダーどこまで進めるか
     * */
    private fun getSliderControl(currentValue: Float = NICO_HISTORY_NAME.size.toFloat()): Control {
        // コントローラーを長押しした時に表示するActivity。今の所なし
        val intent = Intent()
        val pendingIntent = PendingIntent.getActivity(this, 10, intent, if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else PendingIntent.FLAG_UPDATE_CURRENT)
        // コントロール
        val sliderControl = Control.StatefulBuilder(NICO_VERSION_HISTORY_CONTROL_ID, pendingIntent)
            .setTitle("ニコニコ動画：${NICO_HISTORY_NAME[currentValue.toInt() - 1]}") // たいとる
            .setSubtitle(NICO_HISTORY_DURATION[currentValue.toInt() - 1]) // サブタイトル
            .setDeviceType(DeviceTypes.TYPE_LIGHT) // 多分アイコンに使われてる？
            .setStatus(Control.STATUS_OK) // 現在の状態
        sliderControl.setControlTemplate(RangeTemplate("range", 1f, NICO_HISTORY_NAME.size.toFloat(), currentValue, 1f, "第 %.0f 弾"))
        return sliderControl.build()
    }

}