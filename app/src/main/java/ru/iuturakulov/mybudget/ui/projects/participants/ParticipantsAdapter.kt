package ru.iuturakulov.mybudget.ui.projects.participants

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.iuturakulov.mybudget.data.local.entities.ParticipantEntity
import ru.iuturakulov.mybudget.databinding.ItemProjectParticipantsBinding

class ParticipantsAdapter(
    private val onEditClick: (ParticipantEntity) -> Unit,
    private val onDeleteClick: (ParticipantEntity) -> Unit
) : ListAdapter<ParticipantEntity, ParticipantsAdapter.ParticipantViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ParticipantViewHolder {
        val binding = ItemProjectParticipantsBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ParticipantViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ParticipantViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ParticipantViewHolder(private val binding: ItemProjectParticipantsBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(participant: ParticipantEntity) {
            binding.apply {
                tvParticipantName.text = participant.name
                tvParticipantRole.text = participant.role
                btnEditParticipant.setOnClickListener {
                    onEditClick(participant)
                }
                btnDeleteParticipant.setOnClickListener {
                    onDeleteClick(participant)
                }
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<ParticipantEntity>() {
        override fun areItemsTheSame(
            oldItem: ParticipantEntity,
            newItem: ParticipantEntity
        ): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(
            oldItem: ParticipantEntity,
            newItem: ParticipantEntity
        ): Boolean {
            return oldItem == newItem
        }
    }
}
