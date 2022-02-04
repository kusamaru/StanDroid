package com.kusamaru.standroid.nicovideo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.text.HtmlCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import com.kusamaru.standroid.nicovideo.fragment.NicoVideoMyListFragment
import com.kusamaru.standroid.nicovideo.fragment.NicoVideoNicoRepoFragment
import com.kusamaru.standroid.nicovideo.fragment.NicoVideoSeriesListFragment
import com.kusamaru.standroid.nicovideo.fragment.NicoVideoUploadVideoFragment
import com.kusamaru.standroid.nicovideo.viewmodel.factory.NicoAccountViewModelFactory
import com.kusamaru.standroid.nicovideo.viewmodel.NicoAccountViewModel
import com.kusamaru.standroid.R
import com.kusamaru.standroid.databinding.FragmentAccountBinding

/**
 * アカウント情報Fragment
 *
 * 投稿動画とか公開マイリストとか。データ取得は[NicoAccountViewModel]に書いてあります
 *
 * 入れてほしいもの
 *
 * userId   | String    | ユーザーID。ない場合(nullのとき)は自分のアカウントの情報を取りに行きます
 * */
class NicoAccountFragment : Fragment() {

    private lateinit var accountViewModel: NicoAccountViewModel

    /** findViewById駆逐 */
    private val viewBinding by lazy { FragmentAccountBinding.inflate(layoutInflater) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val userId = arguments?.getString("userId")
        accountViewModel = ViewModelProvider(this, NicoAccountViewModelFactory(requireActivity().application, userId)).get(NicoAccountViewModel::class.java)

        // ViewModelからでーたをうけとる
        accountViewModel.userDataLiveData.observe(viewLifecycleOwner) { data ->
            viewBinding.fragmentAccountUserNameTextView.text = data.nickName
            viewBinding.fragmentAccountUserIdTextView.text = data.userId
            viewBinding.fragmentAccountVersionNameTextView.text = data.niconicoVersion
            viewBinding.fragmentAccountDescriptionTextView.text = HtmlCompat.fromHtml(data.description, HtmlCompat.FROM_HTML_MODE_COMPACT)
            viewBinding.fragmentAccountFollowCountTextView.text = "${getString(R.string.follow_count)}：${data.followeeCount}"
            viewBinding.fragmentAccountFollowerCountTextView.text = "${getString(R.string.follower_count)}：${data.followerCount}"
            // あば＾～ー画像
            Glide.with(viewBinding.fragmentAccountAvatarImageView).load(data.largeIcon).into(viewBinding.fragmentAccountAvatarImageView)
            viewBinding.fragmentAccountAvatarImageView.imageTintList = null
            // 自分ならフォローボタン潰す
            viewBinding.fragmentAccountFollowButton.isVisible = userId != null

            // フォローボタン押せるように
            setFollowButtonClick()

            // プレ垢
            viewBinding.fragmentAccountPremium.isVisible = data.isPremium

            // Fragmentにわたすデータ
            val bundle = Bundle().apply {
                putString("userId", data.userId)
            }

            // 投稿動画Fragmentへ遷移
            viewBinding.fragmentAccountUploadVideoTextView.setOnClickListener {
                val nicoVideoPOSTFragment = NicoVideoUploadVideoFragment().apply {
                    arguments = bundle
                }
                setFragment(nicoVideoPOSTFragment, "post")
            }

            // ニコレポFragment
            viewBinding.fragmentAccountNicorepoTextView.setOnClickListener {
                val nicoRepoFragment = NicoVideoNicoRepoFragment().apply {
                    arguments = bundle
                }
                setFragment(nicoRepoFragment, "nicorepo")
            }

            // マイリストFragment
            viewBinding.fragmentAccountMylistTextView.setOnClickListener {
                val myListFragment = NicoVideoMyListFragment().apply {
                    arguments = bundle
                }
                setFragment(myListFragment, "mylist")
            }

            // シリーズFragment
            viewBinding.fragmentAccountSeriesTextView.setOnClickListener {
                val seriesFragment = NicoVideoSeriesListFragment().apply {
                    arguments = bundle
                }
                setFragment(seriesFragment, "series")
            }

        }

        // フォロー状態をLiveDataで受け取る
        accountViewModel.followStatusLiveData.observe(viewLifecycleOwner) { isFollowing ->
            // フォロー中ならフォロー中にする
            viewBinding.fragmentAccountFollowButton.text = if (isFollowing) {
                getString(R.string.is_following)
            } else {
                getString(R.string.follow_count)
            }
        }

    }

    private fun setFollowButtonClick() {
        // フォローボタン押した時
        viewBinding.fragmentAccountFollowButton.setOnClickListener {
            // フォロー状態を確認
            val isFollowing = accountViewModel.followStatusLiveData.value == true
            // メッセージ調整
            val snackbarMessage = if (!isFollowing) getString(R.string.nicovideo_account_follow_message_message) else getString(R.string.nicovideo_account_remove_follow_message)
            val snackbarAction = if (!isFollowing) getString(R.string.nicovideo_account_follow) else getString(R.string.nicovideo_account_remove_follow)
            // 確認する
            Snackbar.make(it, snackbarMessage, Snackbar.LENGTH_LONG).setAction(snackbarAction) {
                accountViewModel.postFollowRequest()
            }.show()
        }
    }

    /**
     * Fragmentを置く。第２引数はバックキーで戻るよう
     * */
    private fun setFragment(fragment: Fragment, backstack: String) {
        parentFragmentManager.beginTransaction().replace(id, fragment).addToBackStack(backstack).commit()
    }

}