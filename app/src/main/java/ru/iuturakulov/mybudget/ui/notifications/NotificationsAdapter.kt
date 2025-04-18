package ru.iuturakulov.mybudget.ui.notifications

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

//class NotificationsAdapter(
//    private val onClick: (NotificationsViewModel.NotificationUi) -> Unit
//) : ListAdapter<NotificationsViewModel.NotificationUi, NotificationsAdapter.VH>(Diff()) {
//
//    inner class VH(private val vb: ItemNotificationBinding) :
//        RecyclerView.ViewHolder(vb.root) {
//
//        fun bind(item: NotificationsViewModel.NotificationUi) = with(vb) {
//            tvTitle.text = item.title
//            tvBody .text = item.body
//            tvDate .text = item.date
//            root.alpha = if (item.read) 0.4f else 1f
//            root.setOnClickListener { onClick(item) }
//        }
//    }
//
//    override fun onCreateViewHolder(p: ViewGroup, v: Int) =
//        VH(ItemNotificationBinding.inflate(LayoutInflater.from(p.context), p, false))
//    override fun onBindViewHolder(h: VH, pos: Int) = h.bind(getItem(pos))
//
//    private class Diff : DiffUtil.ItemCallback<NotificationsViewModel.NotificationUi>() {
//        override fun areItemsTheSame(o: NotificationsViewModel.NotificationUi, n: NotificationsViewModel.NotificationUi) = o.id == n.id
//        override fun areContentsTheSame(o: NotificationsViewModel.NotificationUi, n: NotificationsViewModel.NotificationUi) = o == n
//    }
//}
