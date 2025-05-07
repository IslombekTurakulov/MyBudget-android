package ru.iuturakulov.mybudget.ui.analytics

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import ru.iuturakulov.mybudget.databinding.AnalyticsItemInfoBinding
import ru.iuturakulov.mybudget.databinding.SheetAnalyticsDetailListBinding

class DetailListBottomSheet<U : Any>(
    private val title: String,
    private val items: List<U>,
    private val binder: (View, U) -> Unit,
) : BottomSheetDialogFragment() {

    private var _binding: SheetAnalyticsDetailListBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = SheetAnalyticsDetailListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.tvSheetTitle.text = title

        // Пустое состояние
        if (items.isEmpty()) {
            binding.rvSheetList.isVisible = false
            binding.tvEmpty.isVisible = true
            return
        }

        binding.tvEmpty.isVisible = false
        binding.rvSheetList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            addItemDecoration(
                DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
            )
            adapter = object : ListAdapter<U, DetailViewHolder>(
                object : DiffUtil.ItemCallback<U>() {
                    override fun areItemsTheSame(oldItem: U, newItem: U): Boolean =
                        oldItem === newItem

                    override fun areContentsTheSame(oldItem: U, newItem: U): Boolean =
                        oldItem === newItem
                }
            ) {
                override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DetailViewHolder {
                    val itemBinding = AnalyticsItemInfoBinding
                        .inflate(layoutInflater, parent, false)
                    return DetailViewHolder(itemBinding)
                }

                override fun onBindViewHolder(holder: DetailViewHolder, position: Int) {
                    binder(holder.binding.root, getItem(position))
                }
            }.also { it.submitList(items) }

        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private class DetailViewHolder(val binding: AnalyticsItemInfoBinding) :
        RecyclerView.ViewHolder(binding.root)
}
