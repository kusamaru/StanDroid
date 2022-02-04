package com.kusamaru.standroid.adapter.parcelable

import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable

/**
 * ViewPagerに動的に追加した際に、画面回転してもFragmentを再生成するための値を置いておくデータクラス。
 * 多分画面回転時の受け渡し以外で使うことはない。
 * @param bundle Fragment#getArgments()の値。Parcelableだから多分行ける
 * @param text TabLayoutに表示するテキスト。
 * @param type DevNicoVideoRecyclerPagerAdapter#getType(Fragment)の返り値。post / mylist / search のどれかだと思う。
 * */
data class TabLayoutData(
    val type: String?,
    val text: String?,
    val bundle: Bundle?
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString(),
        parcel.readString(),
        parcel.readBundle(Bundle::class.java.classLoader)
    ) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(type)
        parcel.writeString(text)
        parcel.writeBundle(bundle)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<TabLayoutData> {
        override fun createFromParcel(parcel: Parcel): TabLayoutData {
            return TabLayoutData(parcel)
        }

        override fun newArray(size: Int): Array<TabLayoutData?> {
            return arrayOfNulls(size)
        }
    }
}