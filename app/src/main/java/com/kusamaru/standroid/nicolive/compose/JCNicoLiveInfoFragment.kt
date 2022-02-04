package com.kusamaru.standroid.nicolive.compose

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.kusamaru.standroid.MainActivity
import com.kusamaru.standroid.R
import com.kusamaru.standroid.nicoad.NicoAdBottomFragment
import com.kusamaru.standroid.nicolive.bottomfragment.NicoLiveGiftBottomFragment
import com.kusamaru.standroid.nicolive.bottomfragment.NicoLiveKonomiTagEditBottomFragment
import com.kusamaru.standroid.nicolive.bottomfragment.NicoLiveTagBottomFragment
import com.kusamaru.standroid.nicolive.viewmodel.NicoLiveViewModel
import com.kusamaru.standroid.nicovideo.NicoAccountFragment
import com.kusamaru.standroid.nicovideo.compose.DarkColors
import com.kusamaru.standroid.nicovideo.compose.LightColors
import com.kusamaru.standroid.nicovideo.compose.NicoVideoUserCard
import com.kusamaru.standroid.tool.NicoVideoDescriptionText
import com.kusamaru.standroid.tool.isDarkMode

/**
 * 番組詳細Fragment
 * */
class JCNicoLiveInfoFragment : Fragment() {

    /** ViewModel */
    private val viewModel by viewModels<NicoLiveViewModel>({ requireParentFragment() })

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return ComposeView(requireContext()).apply {
            setContent {
                MaterialTheme(
                    colors = if (isDarkMode(LocalContext.current)) DarkColors else LightColors,
                ) {

                    // 番組情報
                    val programData = viewModel.nicoLiveProgramData.observeAsState()
                    // 説明文
                    val description = viewModel.nicoLiveProgramDescriptionLiveData.observeAsState()
                    // ユーザー情報
                    val userData = viewModel.nicoLiveUserDataLiveData.observeAsState()
                    // コミュ、チャンネル情報
                    val communityOrChannelData = viewModel.nicoLiveCommunityOrChannelDataLiveData.observeAsState()
                    // コミュ、チャンネルフォロー中か
                    val isCommunityOrChannelFollow = viewModel.isCommunityOrChannelFollowLiveData.observeAsState(initial = false)
                    // タグ
                    val tagData = viewModel.nicoLiveTagDataListLiveData.observeAsState()
                    // 好みタグ
                    val konomiTagList = viewModel.nicoLiveKonomiTagListLiveData.observeAsState(initial = arrayListOf())
                    // 統計情報LiveData
                    val statisticsLiveData = viewModel.statisticsLiveData.observeAsState()
                    // タイムシフト予約済みかどうか（なお予約済みかどうかはAPIを叩くまでわからん）
                    val isRegisteredTimeShift = viewModel.isTimeShiftRegisteredLiveData.observeAsState(initial = false)
                    // タイムシフト予約が可能かどうか
                    val isAllowTSRegister = viewModel.isAllowTSRegister.observeAsState(initial = true)
                    // コメント一覧表示中か
                    val isVisibleCommentList = viewModel.commentListShowLiveData.observeAsState(initial = false)

                    Surface {
                        Scaffold {
                            /** コメント一覧表示中は情報表示させても意味ないので消す？ */
                            if (!isVisibleCommentList.value) {
                                /** スクロールできるColumn。LazyColumnがサイズ変わったときになんかおかしくなる */
                                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                    // 番組情報
                                    if (programData.value != null && description.value != null) {
                                        NicoLiveInfoCard(
                                            nicoLiveProgramData = programData.value!!,
                                            programDescription = description.value!!,
                                            isRegisteredTimeShift = isRegisteredTimeShift.value,
                                            isAllowTSRegister = isAllowTSRegister.value!!,
                                            onClickTimeShift = { registerTimeShift() },
                                            descriptionClick = { link, type ->
                                                if (type == NicoVideoDescriptionText.DESCRIPTION_TYPE_URL) {
                                                    val intent = Intent(Intent.ACTION_VIEW, link.toUri())
                                                    startActivity(intent)
                                                }
                                            }
                                        )
                                    }
                                    // ユーザー情報。ニコ動用のがそのまま使えた
                                    if (userData.value != null) {
                                        NicoVideoUserCard(userData = userData.value!!, onUserOpenClick = {
                                            setAccountFragment(userData.value!!.userId)
                                        })
                                    }
                                    // コミュ、番組情報
                                    if (communityOrChannelData.value != null) {
                                        NicoLiveCommunityCard(
                                            communityOrChannelData = communityOrChannelData.value!!,
                                            isFollow = isCommunityOrChannelFollow.value,
                                            onFollowClick = {
                                                if (isCommunityOrChannelFollow.value) {
                                                    // 解除
                                                    requestRemoveCommunityFollow(communityOrChannelData.value!!.id)
                                                } else {
                                                    // コミュをフォローする
                                                    requestCommunityFollow(communityOrChannelData.value!!.id)
                                                }
                                            },
                                            onCommunityOpenClick = {
                                                launchBrowser("https://com.nicovideo.jp/community/${communityOrChannelData.value!!.id}")
                                            }
                                        )
                                    }
                                    // タグ
                                    if (tagData.value != null) {
                                        NicoLiveTagCard(
                                            tagItemDataList = tagData.value!!.tagList,
                                            onClickTag = { openBrowser("https://live.nicovideo.jp/search?keyword=${it.tagName}&isTagSearch=true") },
                                            isEditable = !tagData.value!!.isLocked,
                                            onClickEditButton = { showTagEditBottomFragment() },
                                            onClickNicoPediaButton = { openBrowser(it) }
                                        )
                                    }
                                    // 好みタグ
                                    NicoLiveKonomiCard(konomiTagList = konomiTagList.value, onClickEditButton = { showKonomiTagEditBottomFragment() })

                                    // メニュー
                                    NicoLiveMenuScreen(requireParentFragment())

                                    if (statisticsLiveData.value != null) {
                                        // ニコニ広告 投げ銭
                                        NicoLivePointCard(
                                            totalNicoAdPoint = statisticsLiveData.value!!.adPoints,
                                            totalGiftPoint = statisticsLiveData.value!!.giftPoints,
                                            onClickNicoAdOpen = { showNicoAdBottomFragment() },
                                            onClickGiftOpen = { showGiftBottomFragment() }
                                        )
                                    }

                                    // スペース
                                    Spacer(modifier = Modifier.height(100.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /** 好みタグの編集画面を出す。*/
    private fun showKonomiTagEditBottomFragment() {
        NicoLiveKonomiTagEditBottomFragment().apply {
            arguments = Bundle().apply {
                putString("broadcaster_user_id", viewModel.nicoLiveUserDataLiveData.value?.userId)
            }
        }.show(parentFragmentManager, "konomi_tag")
    }

    /**
     * ブラウザを開く
     * @param url リンク
     * */
    private fun openBrowser(url: String) {
        Intent(Intent.ACTION_VIEW, url.toUri()).apply {
            startActivity(this)
        }
    }

    /** 投げ銭のBottomFragment表示 */
    private fun showGiftBottomFragment() {
        NicoLiveGiftBottomFragment().apply {
            arguments = Bundle().apply {
                putString("live_id", viewModel.nicoLiveProgramData.value?.programId)
            }
        }.show(parentFragmentManager, "gift")
    }

    /** ニコニ広告BottomFragmentを表示させる */
    private fun showNicoAdBottomFragment() {
        NicoAdBottomFragment().apply {
            arguments = Bundle().apply {
                putString("content_id", viewModel.nicoLiveProgramData.value?.programId)
            }
        }.show(parentFragmentManager, "nicoad")
    }

    /** タグ編集画面を出す */
    private fun showTagEditBottomFragment() {
        NicoLiveTagBottomFragment().show(parentFragmentManager, "edit_tag")
    }

    /**
     * ブラウザを開く
     * @param url うらる。
     * */
    private fun launchBrowser(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, url.toUri())
        startActivity(intent)
    }

    /**
     * アカウント情報Fragmentを表示
     * @param userId ゆーざーID
     * */
    private fun setAccountFragment(userId: String) {
        val accountFragment = NicoAccountFragment().apply {
            arguments = Bundle().apply {
                putString("userId", userId)
            }
        }
        setFragment(accountFragment, "account")
    }

    /**
     * Fragmentを置く関数
     *
     * @param fragment 置くFragment
     * @param backstack Fragmentを積み上げる場合は適当な値を入れて
     * */
    private fun setFragment(fragment: Fragment, backstack: String) {
        // Fragment設置
        (requireActivity() as MainActivity).setFragment(fragment, backstack, true)
        // ミニプレイヤー化
        viewModel.isMiniPlayerMode.postValue(true)
    }

    /** コミュをフォローする関数 */
    private fun requestCommunityFollow(communityId: String) {
        (requireParentFragment() as? JCNicoLiveFragment)?.showSnackBar(getString(R.string.nicovideo_account_follow_message_message), getString(R.string.follow_count)) {
            viewModel.requestCommunityFollow(communityId)
        }
    }

    /** コミュのフォローを解除する関数 */
    private fun requestRemoveCommunityFollow(communityId: String) {
        (requireParentFragment() as? JCNicoLiveFragment)?.showSnackBar(getString(R.string.nicovideo_account_remove_follow_message), getString(R.string.nicovideo_account_remove_follow)) {
            viewModel.requestRemoveCommunityFollow(communityId)
        }
    }

    /** TS予約、解除を行う関数 */
    fun registerTimeShift() {
        val isRegisteredTS = viewModel.isTimeShiftRegisteredLiveData.value ?: false
        val message = if (isRegisteredTS) getString(R.string.nicolive_time_shift_un_register_message) else getString(R.string.nicolive_time_shift_register_message)
        val action = if (isRegisteredTS) getString(R.string.nicolive_time_shift_un_register_short) else getString(R.string.nicolive_time_shift_register_short)
        (requireParentFragment() as JCNicoLiveFragment).showSnackBar(message, action) {
            if (isRegisteredTS) {
                // 登録解除APIを叩く
                viewModel.unRegisterTimeShift()
            } else {
                // 登録APIを叩く
                viewModel.registerTimeShift()
            }
        }
    }

}