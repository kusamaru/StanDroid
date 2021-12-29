package io.github.takusan23.tatimidroid.tool

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent

/**
 * 加速度センサーを使って画面回転。実際は加速度センサーだけでは傾き特定できない。センサーの終了は [Lifecycle] が自動で解決してくれる。
 * 使ってみたかったLifecycleライブラリ
 * @param activity 画面回転をする際にActivityが必要なので
 * @param lifecycle 面倒なライフサイクルを解決するのに使う。@OnLifecycleEventをつけた関数はライフサイクルに合わせて自動で呼ばれる？
 * */
class RotationSensor(activity: Activity, lifecycle: Lifecycle) : LifecycleObserver {

    private var sensorManager: SensorManager = activity.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private var sensorEventListener: SensorEventListener

    //加速度の値。配列になっている
    var accelerometerList = floatArrayOf()

    init {
        // Lifecycle考えてくれるやつ
        lifecycle.addObserver(this)

        //加速度
        val accelerometer = sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER)
        //受け取る
        sensorEventListener = object : SensorEventListener {
            @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                //つかわん
            }

            @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
            override fun onSensorChanged(event: SensorEvent?) {
                //値はここで受けとる
                when (event?.sensor?.type) {
                    Sensor.TYPE_ACCELEROMETER -> {
                        // 加速度
                        accelerometerList = event.values
                        // 画面回転。xの値を使う
                        activity.requestedOrientation = when {
                            accelerometerList[0] >= 5 -> {
                                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                            }
                            accelerometerList[0] <= -5 -> {
                                ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
                            }
                            else -> {
                                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                            }
                        }
                    }
                }
            }
        }
        // 加速度センサー登録
        sensorManager.registerListener(
            sensorEventListener,
            accelerometer[0],  //配列のいっこめ。
            SensorManager.SENSOR_DELAY_NORMAL  //更新頻度
        )
    }

    /*
    * ライフサイクルがonDestroyなら勝手に呼ばれる
    * */
    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    private fun destroy() {
        sensorManager.unregisterListener(sensorEventListener)
    }

}