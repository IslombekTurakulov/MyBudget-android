package ru.iuturakulov.mybudget.ui.projects.participants

import android.content.ClipData
import android.content.ClipboardManager
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import ru.iuturakulov.mybudget.data.local.entities.ParticipantEntity
import ru.iuturakulov.mybudget.data.remote.dto.ParticipantRole
import ru.iuturakulov.mybudget.databinding.ItemProjectParticipantsBinding

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
                tvParticipantEmail.text = participant.email

                val copyText = "${participant.name} <${participant.email}>"

                fun copyToClipboard(text: String) {
                    val clipboard = ContextCompat.getSystemService(
                        root.context,
                        ClipboardManager::class.java
                    ) as ClipboardManager
                    val clip = ClipData.newPlainText("ParticipantInfo", text)
                    clipboard.setPrimaryClip(clip)
                    Snackbar.make(root, "Скопировано: $text", Snackbar.LENGTH_SHORT).show()
                }

                tvParticipantName.setOnClickListener {
                    copyToClipboard(copyText)
                }

                tvParticipantEmail.setOnClickListener {
                    copyToClipboard(copyText)
                }

                chipParticipantRole.text = participant.role.getDisplayName(binding.root.context)
                chipParticipantRole.chipBackgroundColor = ColorStateList.valueOf(
                    ColorUtils.setAlphaComponent(
                        participant.role.getStatusColor(binding.root.context),
                        (0.3f * 255).toInt()
                    )
                )

                if (currentUserParticipantRole.name == ParticipantRole.OWNER.name) {
                    btnEditParticipant.visibility = View.VISIBLE
                    btnDeleteParticipant.visibility = View.VISIBLE
                } else {
                    btnEditParticipant.visibility = View.GONE
                    btnDeleteParticipant.visibility = View.GONE
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
