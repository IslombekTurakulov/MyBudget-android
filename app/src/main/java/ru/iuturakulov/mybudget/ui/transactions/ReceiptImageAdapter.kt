package ru.iuturakulov.mybudget.ui.transactions

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import ru.iuturakulov.mybudget.R

// Улучшенный адаптер для RecyclerView с изображениями чеков
class ReceiptImageAdapter(
    private var images: List<Bitmap>,
    private val onDelete: (Int) -> Unit,
    private val onImageClick: (Int) -> Unit
) : RecyclerView.Adapter<ReceiptImageAdapter.ImageViewHolder>() {

    // Константы для анимации
    companion object {
        private const val ANIMATION_DURATION = 200L
    }

    inner class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.ivReceiptImage)
        val btnDelete: ImageButton = itemView.findViewById(R.id.btnDeleteImage)
        val rootLayout: ViewGroup = itemView.findViewById(R.id.rootLayout)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_receipt_image, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        // Устанавливаем изображение с учетом памяти
        holder.imageView.setImageBitmap(images.getOrNull(position))
        
        // Обработчик клика по изображению (просмотр)
        holder.imageView.setOnClickListener {
            onImageClick(position)
        }
        
        // Обработчик удаления с анимацией
        holder.btnDelete.apply {
            // Всегда показываем кнопку удаления
            visibility = View.VISIBLE
            
            setOnClickListener {
                animate().apply {
                    duration = ANIMATION_DURATION
                    scaleX(0.8f)
                    scaleY(0.8f)
                    withEndAction {
                        onDelete(position)
                        // Анимация удаления элемента
                        holder.rootLayout.animate()
                            .alpha(0f)
                            .setDuration(ANIMATION_DURATION)
                            .withEndAction {
                                notifyItemRemoved(position)
                            }
                            .start()
                    }
                }.start()
            }
        }
        
        // Анимация при появлении элемента
        holder.rootLayout.alpha = 0f
        holder.rootLayout.animate()
            .alpha(1f)
            .setDuration(ANIMATION_DURATION)
            .start()
    }

    override fun getItemCount(): Int = images.size

    // Метод для обновления данных
    fun updateImages(newImages: List<Bitmap>) {
        val diffCallback = object : DiffUtil.Callback() {
            override fun getOldListSize() = images.size
            override fun getNewListSize() = newImages.size
            override fun areItemsTheSame(oldPos: Int, newPos: Int) = 
                images[oldPos].sameAs(newImages[newPos])
            override fun areContentsTheSame(oldPos: Int, newPos: Int) = true
        }
        
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        images = newImages
        diffResult.dispatchUpdatesTo(this)
    }

    // Метод для получения изображения по позиции
    fun getImageAt(position: Int): Bitmap? = images.getOrNull(position)
}