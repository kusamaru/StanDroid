package com.kusamaru.standroid.tool

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.annotation.RequiresApi
import com.kusamaru.standroid.R

/**
 * インターネットに接続できるか。接続してればtrue
 * */
fun isConnectionInternet(context: Context?): Boolean {
    val connectivityManager =
        context?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        // Android 10時代のネットワーク接続チェック
        val network = connectivityManager?.activeNetwork
        val networkCapabilities = connectivityManager?.getNetworkCapabilities(network)
        return when {
            networkCapabilities == null -> false
            // Wi-Fi / MobileData / EtherNet / Bluetooth のどれかでつながっているか
            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> true
            else -> false
        }
    } else {
        // 今までのネットワーク接続チェック
        return connectivityManager?.activeNetworkInfo != null && connectivityManager.activeNetworkInfo!!.isConnected
    }
}

/**
 * モバイルデータ接続かどうかを返す関数。モバイルデータ接続の場合はtrue
 * */
fun isConnectionMobileDataInternet(context: Context?): Boolean {
    //今の接続状態を取得
    val connectivityManager = context?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    //ろりぽっぷとましゅまろ以上で分岐
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val networkCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
    } else {
        connectivityManager.activeNetworkInfo!!.type == ConnectivityManager.TYPE_MOBILE
    }
}

/**
 * Wi-Fiでネットワークに接続している場合はtrueを返す
 * @param context コンテキスト
 * @return Wi-Fi接続時ならtrue
 * */
fun isConnectionWiFiInternet(context: Context?): Boolean {
    //今の接続状態を取得
    val connectivityManager = context?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    //ろりぽっぷとましゅまろ以上で分岐
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val networkCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
    } else {
        connectivityManager.activeNetworkInfo!!.type == ConnectivityManager.TYPE_WIFI
    }
}


/**
 * 公式ドキュメント眺めてたら面白そうなもの発見した。Android 6以上で利用できる。
 *
 * インターネット接続が定額制（一度払えば使い放題。固定回線みたいな）か従量制（使った分だけ払う。パケ死ってやつですか）かどうかを判断する
 *
 * 私の環境ではWi-Fiのときはtrue(定額制設定)。モバイルデータ時はfalseだった。5Gの使い放題とかはどっちが返ってくるのか気になるわ
 *
 * @param context コンテキスト
 * @return 定額制ネットワークならtrue。そうじゃないならfalse
 * */
@RequiresApi(Build.VERSION_CODES.M)
fun isConnectionNetworkTypeUnlimited(context: Context?): Boolean {
    val connectivityManager = context?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val networkCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork) ?: return false
    return networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) || networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
}

object InternetConnectionCheck {

    /**
     * [isConnectionWiFiInternet]ならWi-Fiに接続中ですみたいなメッセージを生成する
     * @param context null絶対許さんから。[androidx.fragment.app.Fragment]のときは[androidx.fragment.app.Fragment.requireContext]で行ける
     * */
    fun createNetworkMessage(context: Context): String {
        val isUnlimitedNetwork = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            when {
                isConnectionNetworkTypeUnlimited(context) -> "\n現在のネットワーク：定額制（食べ放題）"
                else -> "\n現在のネットワーク：従量制"
            }
        } else {
            ""
        }
        return when {
            isConnectionMobileDataInternet(context) -> "モバイルデータを利用して再生しています。$isUnlimitedNetwork"
            isConnectionWiFiInternet(context) -> "Wi-Fiを利用して再生しています。$isUnlimitedNetwork"
            else -> "ネットワーク接続方法がわかりませんでした。"
        }
    }

    /**
     * Wi-Fi接続時ならWi-Fiアイコンな[android.graphics.drawable.Drawable]を返します。
     *
     * モバイルデータ時はアンテナピクトな[android.graphics.drawable.Drawable]を返します。
     *
     * @param context [androidx.fragment.app.Fragment.requireContext]だとcontextがnull絶対無い状態でくれる
     * */
    fun getConnectionTypeDrawable(context: Context) = when {
        isConnectionMobileDataInternet(context) -> context.getDrawable(R.drawable.ic_signal_cellular_alt_black_24dp)
        else -> context.getDrawable(R.drawable.ic_wifi_black_24dp)
    }

}