package ru.iuturakulov.mybudget.ui.projects.participants

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.iuturakulov.mybudget.data.local.entities.ParticipantEntity
import ru.iuturakulov.mybudget.data.remote.dto.ParticipantRole
import ru.iuturakulov.mybudget.databinding.ItemProjectParticipantsBinding
import timber.log.Timber

class ParticipantsAdapter(
    private val onEditClick: (ParticipantEntity) -> Unit,
    private val onDeleteClick: (ParticipantEntity) -> Unit,
    private val currentUserParticipantRole: ParticipantRole,
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
                tvParticipantRole.text = participant.role.name
                if (currentUserParticipantRole.name == ParticipantRole.OWNER.name) {
                    llButtons.visibility = View.VISIBLE
                } else {
                    llButtons.visibility = View.GONE
                }

                if (participant.role == ParticipantRole.OWNER) {
                    btnEditParticipant.visibility = View.GONE
                    btnDeleteParticipant.visibility = View.GONE
                } else {
                    btnEditParticipant.setOnClickListener {
                        onEditClick(participant)
                    }
                    btnDeleteParticipant.setOnClickListener {
                        onDeleteClick(participant)
                    }
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
