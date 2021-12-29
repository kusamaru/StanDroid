package io.github.takusan23.tatimidroid.nicolive.bottomfragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.github.takusan23.tatimidroid.nicoapi.nicolive.dataclass.NicoTagItemData
import io.github.takusan23.tatimidroid.nicolive.adapter.TagRecyclerViewAdapter
import io.github.takusan23.tatimidroid.nicolive.viewmodel.NicoLiveViewModel
import io.github.takusan23.tatimidroid.databinding.BottomFragmentNicoliveTagEditBinding

/** タグ編集BottomFragment */
class NicoLiveTagBottomFragment : BottomSheetDialogFragment() {

    /** findViewById駆逐 */
    private val viewBinding by lazy { BottomFragmentNicoliveTagEditBinding.inflate(layoutInflater) }

   /** ViewModelで共有 */
   private val viewModel by viewModels<NicoLiveViewModel>({ requireParentFragment() })

   // RecyclerView
   private var recyclerViewList = arrayListOf<NicoTagItemData>()
   private val tagRecyclerViewAdapter by lazy { TagRecyclerViewAdapter(recyclerViewList, viewModel) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // RecyclerView初期化
        viewBinding.bottomFragmentTagRecyclerView.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            adapter = tagRecyclerViewAdapter
            // 区切り線
            val itemDecoration = DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
            if (itemDecorationCount == 0) {
                addItemDecoration(itemDecoration)
            }
        }

        // データを監視
        viewModel.nicoLiveTagDataListLiveData.observe(viewLifecycleOwner) { list ->
            recyclerViewList.clear()
            recyclerViewList.addAll(list.tagList)
            tagRecyclerViewAdapter.notifyDataSetChanged()
        }

        // タグ追加
        viewBinding.bottomFragmentTagAddButton.setOnClickListener {
            val tagName = viewBinding.bottomFragmentTagEditText.text.toString()
            viewModel.addTag(tagName)
        }

    }
}