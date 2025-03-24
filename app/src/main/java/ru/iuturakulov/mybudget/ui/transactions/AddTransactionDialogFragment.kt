package ru.iuturakulov.mybudget.ui.transactions

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.provider.MediaStore
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dagger.hilt.android.AndroidEntryPoint
import ru.iuturakulov.mybudget.R
import ru.iuturakulov.mybudget.data.local.entities.TemporaryTransaction
import ru.iuturakulov.mybudget.data.local.entities.TransactionEntity
import ru.iuturakulov.mybudget.databinding.DialogAddTransactionBinding
import ru.iuturakulov.mybudget.ui.projects.details.EmojiPickerAdapter
import java.io.ByteArrayOutputStream

@AndroidEntryPoint
class AddTransactionDialogFragment : DialogFragment() {

    private var _binding: DialogAddTransactionBinding? = null
    private val binding get() = _binding!!

    private var args: AddTransactionArgs? = null
    private var transaction: TemporaryTransaction? = null
    private var onTransactionAdded: ((TemporaryTransaction) -> Unit)? = null
    private var onTransactionUpdated: ((TemporaryTransaction) -> Unit)? = null
    private var onTransactionDeleted: (() -> Unit)? = null

    // Список для загруженных изображений
    private val selectedImages = mutableListOf<Bitmap>()
    // Адаптер для RecyclerView с изображениями чеков
    private lateinit var receiptImageAdapter: ReceiptImageAdapter

    // Лаунчер для выбора изображения из галереи
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            getBitmapFromUri(it)?.let { bitmap ->
                val processedBitmap = preprocessBitmap(bitmap)
                selectedImages.add(processedBitmap)
                receiptImageAdapter.notifyDataSetChanged()
                recognizeTextFromImage(processedBitmap)
            }
        }
    }

    // Лаунчер для запроса разрешений
    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { it }) {
            showImageSourceDialog()
        } else {
            Snackbar.make(binding.root, "Необходимо предоставить разрешения", Snackbar.LENGTH_SHORT)
                .show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAddTransactionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.90).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        args = arguments?.getParcelable(ARG_TRANSACTION)
        setupReceiptImagesRecyclerView() // Инициализация RecyclerView для изображений
        setupViews()
    }

    private fun setupViews() {
        args?.let { argument ->
            transaction = argument.transaction
            binding.apply {
                if (transaction != null) {
                    // Режим редактирования: заполняем поля
                    etTransactionName.setText(transaction?.name)
                    etTransactionAmount.setText(transaction?.amount.toString())
                    spinnerCategory.setText(transaction?.category, false)
                    updateCategoryIcon(transaction?.categoryIcon.orEmpty())
                    selectedImages.addAll(transaction?.images?.mapNotNull(::decodeBase64ToBitmap) ?: emptyList())
                    receiptImageAdapter.notifyDataSetChanged()
                    removeTransactionLayout.visibility = View.VISIBLE
                    addTransactionLayout.visibility = View.GONE
                    btnSave.text = getString(R.string.save)
                    toggleTransactionType.check(
                        if (transaction?.type == TransactionEntity.TransactionType.INCOME) R.id.btnIncome
                        else R.id.btnExpense
                    )
                    btnEditTransaction.setOnClickListener { processSaveButton(onTransactionUpdated, argument) }
                    btnDeleteTransaction.setOnClickListener { showDeleteConfirmationDialog() }
                } else {
                    // Режим добавления: скрываем кнопку удаления
                    removeTransactionLayout.visibility = View.GONE
                    addTransactionLayout.visibility = View.VISIBLE
                    btnSave.text = getString(R.string.add)
                    btnSave.setOnClickListener { processSaveButton(onTransactionAdded, argument) }
                    toggleTransactionType.check(R.id.btnIncome)
                    btnCancel.setOnClickListener { dismiss() }
                    setupTransactionIcon()
                }
                setupCategorySpinner()
                setupEmojiPicker()
                setupTransactionTypeToggle()
                btnScanReceipt.setOnClickListener { checkAndRequestPermissions() }
            }
        }
    }

    private fun processSaveButton(
        invoker: ((TemporaryTransaction) -> Unit)?,
        argument: AddTransactionArgs
    ) {
        if (validateInput()) {
            val transactionType = when (binding.toggleTransactionType.checkedButtonId) {
                R.id.btnIncome -> TransactionEntity.TransactionType.INCOME
                R.id.btnExpense -> TransactionEntity.TransactionType.EXPENSE
                else -> TransactionEntity.TransactionType.INCOME
            }
            val category = if (binding.spinnerCategory.text.toString().equals("Другое", ignoreCase = true))
                binding.etCustomCategory.text.toString() else binding.spinnerCategory.text.toString()
            val temporaryTransaction = TemporaryTransaction(
                id = transaction?.id ?: "${argument.projectId}-${System.currentTimeMillis()}",
                name = binding.etTransactionName.text.toString(),
                amount = binding.etTransactionAmount.text.toString().toDouble(),
                category = category,
                categoryIcon = binding.ivTransactionCategoryIcon.tag?.toString().orEmpty(),
                date = transaction?.date ?: System.currentTimeMillis(),
                projectId = argument.projectId,
                userId = transaction?.userId.orEmpty(),
                type = transactionType,
                images = selectedImages.map { bitmapToBase64(it) }
            )
            invoker?.invoke(temporaryTransaction)
            dismiss()
        }
    }

    private fun setupTransactionTypeToggle() {
        binding.toggleTransactionType.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.btnIncome -> binding.btnIncome.isChecked = true
                    R.id.btnExpense -> binding.btnExpense.isChecked = true
                }
            }
        }
    }

    private fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.delete_transaction))
            .setMessage(getString(R.string.delete_transaction_confirmation))
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                onTransactionDeleted?.invoke()
                dismiss()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun updateCategoryIcon(icon: String) {
        binding.ivTransactionCategoryIcon.apply {
            text = icon
            tag = icon
        }
    }

    private fun decodeBase64ToBitmap(base64String: String): Bitmap? {
        return try {
            val bytes = Base64.decode(base64String, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: Exception) {
            null
        }
    }

    private fun setupTransactionIcon() {
        val emojis = resources.getStringArray(R.array.emoji_list).toList()
        binding.ivTransactionCategoryIcon.text = emojis.shuffled().first()
    }

    private fun setupCategorySpinner() {
        val categories = resources.getStringArray(R.array.transaction_categories)
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categories)
        binding.spinnerCategory.setAdapter(adapter)
        binding.spinnerCategory.setOnClickListener { binding.spinnerCategory.showDropDown() }
        binding.spinnerCategory.setOnItemClickListener { _, _, position, _ ->
            val selectedCategory = categories[position]
            binding.tilCustomCategory.visibility = if (selectedCategory.equals("Другое", ignoreCase = true))
                View.VISIBLE else View.GONE
        }
    }

    private fun setupEmojiPicker() {
        binding.ivTransactionCategoryIcon.setOnClickListener {
            showEmojiPickerDialog { selectedEmoji ->
                binding.ivTransactionCategoryIcon.text = selectedEmoji
                binding.ivTransactionCategoryIcon.tag = selectedEmoji
            }
        }
    }

    private fun showEmojiPickerDialog(onEmojiSelected: (String) -> Unit) {
        val emojis = resources.getStringArray(R.array.emoji_list).toList()
        val dialog = BottomSheetDialog(requireContext())
        val recyclerView = RecyclerView(requireContext()).apply {
            layoutManager = GridLayoutManager(context, 5)
            adapter = EmojiPickerAdapter(emojis) { emoji ->
                onEmojiSelected(emoji)
                dialog.dismiss()
            }
        }
        dialog.setContentView(recyclerView)
        dialog.show()
    }

    private fun checkAndRequestPermissions() {
        val cameraPermission = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
        val storagePermission = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU)
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
        else
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_IMAGES)
        if (cameraPermission == PackageManager.PERMISSION_GRANTED && storagePermission == PackageManager.PERMISSION_GRANTED) {
            showImageSourceDialog()
        } else {
            requestPermissionsLauncher.launch(
                arrayOf(
                    Manifest.permission.CAMERA,
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU)
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    else Manifest.permission.READ_MEDIA_IMAGES
                )
            )
        }
    }

    private fun showImageSourceDialog() {
        val options = arrayOf(getString(R.string.camera), getString(R.string.gallery))
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.select_image_source))
            .setItems(options) { _, which ->
                when (which) {
                    0 -> openCamera()
                    1 -> openGallery()
                }
            }
            .show()
    }

    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(intent, REQUEST_IMAGE_CAPTURE)
    }

    private fun openGallery() {
        imagePickerLauncher.launch("image/*")
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            // Получаем изображение из data; для некоторых устройств может понадобиться сохранять Uri
            data?.data?.let { uri ->
                getBitmapFromUri(uri)?.let { bitmap ->
                    val processedBitmap = preprocessBitmap(bitmap)
                    selectedImages.add(processedBitmap)
                    receiptImageAdapter.notifyDataSetChanged()
                    recognizeTextFromImage(processedBitmap)
                }
            }
        }
    }

    private fun setupReceiptImagesRecyclerView() {
        receiptImageAdapter = ReceiptImageAdapter(selectedImages) { position ->
            selectedImages.removeAt(position)
            receiptImageAdapter.notifyItemRemoved(position)
        }
        binding.rvReceiptImages.layoutManager = GridLayoutManager(requireContext(), 3)
        binding.rvReceiptImages.adapter = receiptImageAdapter
    }

    private fun getBitmapFromUri(uri: Uri): Bitmap? {
        return try {
            requireContext().contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            }
        } catch (e: Exception) {
            Snackbar.make(binding.root, getString(R.string.error_loading_image), Snackbar.LENGTH_SHORT).show()
            null
        }
    }

    // Применение предобработки: преобразование в Ч/Б и изменение размера
    private fun preprocessBitmap(bitmap: Bitmap): Bitmap {
        val grayscale = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(grayscale)
        val paint = Paint().apply { colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) }) }
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return resizeBitmap(grayscale, 1024, 1024)
    }

    private fun resizeBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val (width, height) = if (bitmap.width > maxWidth || bitmap.height > maxHeight) {
            val aspectRatio = bitmap.width.toFloat() / bitmap.height
            if (aspectRatio > 1) maxWidth to (maxWidth / aspectRatio).toInt()
            else (maxHeight * aspectRatio).toInt() to maxHeight
        } else bitmap.width to bitmap.height
        return Bitmap.createScaledBitmap(bitmap, width, height, true).apply { setHasAlpha(false) }
    }

    private fun recognizeTextFromImage(bitmap: Bitmap) {
        val startTime = System.currentTimeMillis()
        val inputImage = InputImage.fromBitmap(bitmap, 0)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.Builder().build())
        recognizer.process(inputImage)
            .addOnSuccessListener { visionText ->
                val processedText = preprocessText(visionText.text)
                val totalAmount = extractTotalAmount(processedText)
                binding.tvRecognizedAmount.text = getString(R.string.recognized_amount, totalAmount.toDoubleOrNull() ?: 0.0)
                updateTransactionAmount(totalAmount)
                val duration = System.currentTimeMillis() - startTime
                Snackbar.make(binding.root, "Распознавание завершено за ${duration}ms", Snackbar.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Snackbar.make(binding.root, "Ошибка распознавания: ${e.localizedMessage}", Snackbar.LENGTH_SHORT).show()
            }
    }

    private fun preprocessText(text: String): String {
        return text.replace(Regex("[©®™•§]"), "")
            .replace(Regex("\\s+"), " ")
            .replace(Regex("(?<=\\d) (?=\\d)"), "")
            .replace(Regex("o", RegexOption.IGNORE_CASE), "0")
            .replace(Regex("[`´‘’′]"), "'")
    }

    private fun extractTotalAmount(text: String): String {
        val patterns = listOf(
            Regex("""(итого|всего|сумма|к\s+оплате|total)\s*[:-]?\s*([\d\s]+[.,]\d{2})\b""", RegexOption.IGNORE_CASE),
            Regex("""([€$₽])\s*([\d\s]+[.,]\d{2})\b"""),
            Regex("""\b(\d{1,3}(?:[ ,]\d{3})*[.,]\d{2})(?=\s*(?:руб|р|usd|€|\$))"""),
            Regex("""\b(\d+[.,]\d{2})\b""")
        )
        patterns.forEach { regex ->
            regex.findAll(text).lastOrNull()?.let {
                return it.groupValues.last().replace(Regex("""[ ,]"""), "").replace(',', '.')
            }
        }
        return "0.00"
    }

    private fun updateTransactionAmount(totalAmount: String) {
        val existingAmount = binding.etTransactionAmount.text?.toString()?.toDoubleOrNull() ?: 0.0
        val recognizedAmount = totalAmount.toDoubleOrNull() ?: 0.0
        val updatedAmount = existingAmount + recognizedAmount
        binding.etTransactionAmount.setText(updatedAmount.toString())
    }

    // Валидация введенных данных
    private fun validateInput(): Boolean {
        val name = binding.etTransactionName.text?.toString()
        val amountStr = binding.etTransactionAmount.text?.toString()
        val customCategory = binding.etCustomCategory?.text?.toString() ?: ""
        if (name.isNullOrBlank()) {
            binding.etTransactionName.error = getString(R.string.error_enter_name)
            return false
        }
        if (amountStr.isNullOrBlank() || amountStr.toDoubleOrNull() == null || amountStr.toDouble() <= 0) {
            binding.etTransactionAmount.error = getString(R.string.error_enter_valid_amount)
            return false
        }
        if (binding.spinnerCategory.text.toString().equals("Другое", ignoreCase = true) && customCategory.isBlank()) {
            Snackbar.make(binding.root, getString(R.string.error_enter_custom_category), Snackbar.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    fun setOnTransactionAdded(listener: (TemporaryTransaction) -> Unit) {
        onTransactionAdded = listener
    }

    fun setOnTransactionUpdated(listener: (TemporaryTransaction) -> Unit) {
        onTransactionUpdated = listener
    }

    fun setOnTransactionDeleted(listener: () -> Unit) {
        onTransactionDeleted = listener
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        return ByteArrayOutputStream().use { baos ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
            Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT)
        }
    }

    // Внутренний адаптер для RecyclerView с изображениями чеков
    inner class ReceiptImageAdapter(
        private val images: List<Bitmap>,
        private val onDelete: (Int) -> Unit
    ) : RecyclerView.Adapter<ReceiptImageAdapter.ImageViewHolder>() {

        inner class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val imageView = itemView.findViewById<android.widget.ImageView>(R.id.ivReceiptImage)
            val btnDelete = itemView.findViewById<android.widget.ImageButton>(R.id.btnDeleteImage)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_receipt_image, parent, false)
            return ImageViewHolder(view)
        }

        override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
            holder.imageView.setImageBitmap(images[position])
            holder.btnDelete.setOnClickListener { onDelete(position) }
        }

        override fun getItemCount(): Int = images.size
    }

    companion object {
        private const val REQUEST_IMAGE_CAPTURE = 1
        private const val ARG_TRANSACTION = "arg_transaction"

        @kotlinx.parcelize.Parcelize
        data class AddTransactionArgs(
            val projectId: String,
            var transaction: TemporaryTransaction? = null
        ) : Parcelable

        fun newInstance(projectId: String, transaction: TemporaryTransaction? = null): AddTransactionDialogFragment {
            val fragment = AddTransactionDialogFragment()
            val args = Bundle().apply {
                putParcelable(ARG_TRANSACTION, AddTransactionArgs(projectId, transaction))
            }
            fragment.arguments = args
            return fragment
        }
    }
}
