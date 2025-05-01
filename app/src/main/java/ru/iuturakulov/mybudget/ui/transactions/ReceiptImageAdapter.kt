package ru.iuturakulov.mybudget.ui.transactions

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView

import ru.iuturakulov.mybudget.databinding.ItemReceiptImageBinding

class ReceiptImageAdapter(
    initialImages: List<Bitmap>,
    private val onDelete: (Int) -> Unit,
    private val onImageClick: (Int) -> Unit
) : RecyclerView.Adapter<ReceiptImageAdapter.ImageViewHolder>() {

    companion object {
        private const val ANIMATION_DURATION = 200L
    }

    private var images: List<Bitmap> = initialImages

    inner class ImageViewHolder(
        private val binding: ItemReceiptImageBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.ivReceiptImage.setOnClickListener {
                val pos = this.position
                if (pos != RecyclerView.NO_POSITION) {
                    onImageClick(pos)
                }
            }
            binding.btnDeleteImage.setOnClickListener {
                val pos = position
                if (pos == RecyclerView.NO_POSITION) return@setOnClickListener

                binding.rootLayout.animate()
                    .scaleX(0.8f).scaleY(0.8f)
                    .setDuration(ANIMATION_DURATION)
                    .withEndAction {
                        onDelete(pos)
                    }
                    .start()
            }
        }

        fun bind(bitmap: Bitmap) {
            binding.ivReceiptImage.setImageBitmap(bitmap)

            binding.rootLayout.alpha = 0f
            binding.rootLayout.animate()
                .alpha(1f)
                .setDuration(ANIMATION_DURATION)
                .start()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val binding = ItemReceiptImageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ImageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        holder.bind(images[position])
    }

    override fun getItemCount(): Int = images.size

    fun getImageAt(position: Int): Bitmap? =
        images.getOrNull(position)

    fun updateImages(newImages: List<Bitmap>) {
        val diff = object : DiffUtil.Callback() {
            override fun getOldListSize() = images.size
            override fun getNewListSize() = newImages.size

            override fun areItemsTheSame(oldPos: Int, newPos: Int) =
                images[oldPos] === newImages[newPos]

            override fun areContentsTheSame(oldPos: Int, newPos: Int) =
                images[oldPos].sameAs(newImages[newPos])
        }

        val result = DiffUtil.calculateDiff(diff)
        images = newImages
        result.dispatchUpdatesTo(this)
    }
}
