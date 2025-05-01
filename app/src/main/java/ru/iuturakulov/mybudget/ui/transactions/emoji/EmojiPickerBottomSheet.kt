package ru.iuturakulov.mybudget.ui.transactions.emoji

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import ru.iuturakulov.mybudget.R
import ru.iuturakulov.mybudget.databinding.BottomSheetEmojiPickerBinding
import ru.iuturakulov.mybudget.ui.BaseBottomSheetDialogFragment
import ru.iuturakulov.mybudget.ui.projects.details.EmojiPickerAdapter

class EmojiPickerBottomSheet : BaseBottomSheetDialogFragment<BottomSheetEmojiPickerBinding>(R.layout.bottom_sheet_emoji_picker) {

    private var onEmojiSelected: ((String) -> Unit)? = null

    fun setOnEmojiSelectedListener(listener: (String) -> Unit) {
        onEmojiSelected = listener
    }

    override fun onStart() {
        super.onStart()
        dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            ?.let { sheet ->
                BottomSheetBehavior.from(sheet).apply {
                    isDraggable = true
                    isHideable = true
                    skipCollapsed = false
                    peekHeight = resources.getDimensionPixelSize(R.dimen.emoji_sheet_peek)
                }
            }
    }

    override fun getViewBinding(view: View): BottomSheetEmojiPickerBinding {
        return BottomSheetEmojiPickerBinding.bind(view)
    }

    override fun setupViews() {
        val emojis = resources.getStringArray(R.array.emoji_list).toList()
        binding.rvEmoji.apply {
            layoutManager = GridLayoutManager(context, resources.getInteger(R.integer.emoji_columns))
            adapter = EmojiPickerAdapter(emojis) { emoji ->
                onEmojiSelected?.invoke(emoji)
                dismiss()
            }
            setHasFixedSize(true)
            addItemDecoration(
                GridSpacingItemDecoration(
                    spanCount = resources.getInteger(R.integer.emoji_columns),
                    spacing = resources.getDimensionPixelSize(R.dimen.emoji_spacing),
                    includeEdge = true
                )
            )
        }
    }

    companion object {
        const val TAG = "EmojiPicker"
    }
}
