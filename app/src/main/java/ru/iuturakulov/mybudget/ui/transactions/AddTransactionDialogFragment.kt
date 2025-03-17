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
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
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

    // Новое: список для загруженных изображений
    private val selectedImages = mutableListOf<Bitmap>()

    // Новое: адаптер для RecyclerView с изображениями
    private lateinit var receiptImageAdapter: ReceiptImageAdapter

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
        setupViews()
        setupReceiptImagesRecyclerView() // Новое: инициализация RecyclerView для изображений
    }

    private fun setupViews() {
        args?.let { argument ->

            transaction = argument.transaction
            // Если транзакция уже есть – заполняем поля, иначе остаётся пустой
            transaction?.let { trans ->
                binding.apply {
                    etTransactionName.setText(trans.name)
                    etTransactionAmount.setText(trans.amount.toString())
                    spinnerCategory.setText(trans.category, false)
                    updateCategoryIcon(trans.categoryIcon)
                    // Заполняем список изображений, если они есть
                    selectedImages.addAll(trans.images.mapNotNull(::decodeBase64ToBitmap))
                    setupReceiptImagesRecyclerView()
                    // Если редактируем – показываем кнопку удаления
                    removeTransactionLayout.visibility = View.VISIBLE
                    binding.addTransactionLayout.visibility = View.GONE
                    btnSave.text = "Сохранить"

                    // Устанавливаем значение по умолчанию (например, "Расход")
                    binding.toggleTransactionType.check(
                        if (trans.type == TransactionEntity.TransactionType.INCOME) {
                            R.id.btnIncome
                        } else {
                            R.id.btnExpense
                        }
                    )

                    btnEditTransaction.setOnClickListener {
                        processSaveButton(onTransactionUpdated, argument)
                    }

                    btnDeleteTransaction.setOnClickListener {
                        showDeleteConfirmationDialog()
                    }
                }
            } ?: run {
                // Режим добавления: скрываем кнопку удаления
                binding.removeTransactionLayout.visibility = View.GONE
                binding.addTransactionLayout.visibility = View.VISIBLE
                binding.btnSave.text = "Добавить"

                // Кнопка "Сохранить"
                binding.btnSave.setOnClickListener {
                    processSaveButton(onTransactionAdded, argument)
                }

                // Устанавливаем значение по умолчанию (например, "Расход")
                binding.toggleTransactionType.check(R.id.btnIncome)

                // Кнопка "Отмена"
                binding.btnCancel.setOnClickListener {
                    dismiss()
                }
                setupReceiptImagesRecyclerView()
            }

            setupCategorySpinner()
            setupEmojiPicker()
            setupTransactionTypeToggle() // Новое: настройка выбора типа транзакции

            // Кнопка "Сканировать чек"
            binding.btnScanReceipt.setOnClickListener {
                checkAndRequestPermissions()
            }
        }
    }

    private fun processSaveButton(invoker: ((TemporaryTransaction) -> Unit)?, argument: AddTransactionArgs) {
        if (validateInput()) {
            // Чтение выбранного типа транзакции из MaterialButtonToggleGroup
            val transactionType = when (binding.toggleTransactionType.checkedButtonId) {
                R.id.btnIncome -> TransactionEntity.TransactionType.INCOME
                R.id.btnExpense -> TransactionEntity.TransactionType.EXPENSE
                else -> TransactionEntity.TransactionType.INCOME
            }

            // Если выбрана опция "Другое", берем значение из поля ввода
            val category = if (binding.spinnerCategory.text.toString()
                    .equals("Другое", ignoreCase = true)
            ) {
                binding.etCustomCategory.text.toString()
            } else {
                binding.spinnerCategory.text.toString()
            }
            // Передаем новые поля: тип транзакции и список изображений
            val temporaryTransaction = TemporaryTransaction(
                id = transaction?.id ?: "${argument.projectId}-${System.currentTimeMillis()}",
                name = binding.etTransactionName.text.toString(),
                amount = binding.etTransactionAmount.text.toString().toDouble(),
                category = category,
                categoryIcon = binding.ivTransactionCategoryIcon.tag?.toString() ?: "",
                date = transaction?.date ?: System.currentTimeMillis(),
                projectId = argument.projectId,
                userId = transaction?.userId ?: "",
                type = transactionType,           // Новое поле для типа транзакции
                images = selectedImages.map { bitmapToBase64(it) }    // Новое поле для прикрепленных изображений
            )
            invoker?.invoke(temporaryTransaction)
            dismiss()
        }
    }

    private fun setupTransactionTypeToggle() {
        // Добавляем слушатель для отслеживания изменений выбора
        binding.toggleTransactionType.addOnButtonCheckedListener { group, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.btnIncome -> {
                        binding.btnIncome.isChecked = true
                    }

                    R.id.btnExpense -> {
                        binding.btnExpense.isChecked = true
                    }
                }
            }
        }
    }

    private fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Удалить транзакцию")
            .setMessage("Вы уверены, что хотите удалить эту транзакцию?")
            .setPositiveButton("Удалить") { _, _ ->
                onTransactionDeleted?.invoke()
                dismiss()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun updateCategoryIcon(icon: String) {
        binding.ivTransactionCategoryIcon.text = icon
    }

    private fun decodeBase64ToBitmap(base64String: String): Bitmap? {
        return try {
            val bytes = Base64.decode(base64String, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: Exception) {
            null
        }
    }

    private fun setupCategorySpinner() {
        val categories = resources.getStringArray(R.array.transaction_categories)
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            categories
        )
        binding.spinnerCategory.setAdapter(adapter)

        binding.spinnerCategory.setOnClickListener {
            binding.spinnerCategory.showDropDown()
        }

        binding.spinnerCategory.setOnItemClickListener { _, _, position, _ ->
            val selectedCategory = categories[position]
            if (selectedCategory.equals("Другое", ignoreCase = true)) {
                // Показываем поле для ввода собственной категории
                binding.tilCustomCategory.visibility = View.VISIBLE
                binding.etCustomCategory.requestFocus()
            } else {
                // Если выбрана другая категория — скрываем поле для ввода своей категории
                binding.tilCustomCategory.visibility = View.GONE
            }
        }
    }

    private fun setupEmojiPicker() {
        binding.ivTransactionCategoryIcon.setOnClickListener {
            showEmojiPickerDialog { selectedEmoji ->
                binding.ivTransactionCategoryIcon.text = selectedEmoji // Устанавливаем эмодзи
                binding.ivTransactionCategoryIcon.tag = selectedEmoji
            }
        }
    }

    private fun showEmojiPickerDialog(onEmojiSelected: (String) -> Unit) {
        val emojis = resources.getStringArray(R.array.emoji_list).toList()
        val dialog = BottomSheetDialog(requireContext())
        val dialogView = RecyclerView(requireContext()).apply {
            layoutManager = GridLayoutManager(context, 5) // Сетка 5xN
            adapter = EmojiPickerAdapter(emojis) { emoji ->
                onEmojiSelected(emoji)
                dialog.dismiss()
            }
        }
        dialog.setContentView(dialogView)
        dialog.show()
    }

    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            showImageSourceDialog()
        } else {
            Toast.makeText(
                requireContext(),
                "Необходимо предоставить разрешения",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun requestPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.CAMERA
        )
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        } else {
            permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
        }
        requestPermissionsLauncher.launch(permissions.toTypedArray())
    }

    private fun checkAndRequestPermissions() {
        val cameraPermission = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.CAMERA
        )
        val storagePermission = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        } else {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_MEDIA_IMAGES
            )
        }
        if (cameraPermission == PackageManager.PERMISSION_GRANTED &&
            storagePermission == PackageManager.PERMISSION_GRANTED
        ) {
            showImageSourceDialog()
        } else {
            requestPermissions()
        }
    }

    private fun showImageSourceDialog() {
        val options = arrayOf("Камера", "Галерея")
        AlertDialog.Builder(requireContext())
            .setTitle("Выберите источник изображения")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> openCamera()
                    1 -> openGallery()
                }
            }
            .show()
    }

    // Обновлённый imagePickerLauncher: после получения изображения добавляем его в список и обновляем RecyclerView
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val bitmap = getBitmapFromUri(uri)
            bitmap?.let {
                val processedBitmap = preprocessBitmap(it)
                selectedImages.add(processedBitmap)
                receiptImageAdapter.notifyDataSetChanged()
                recognizeTextFromImage(processedBitmap)
            }
        }
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
            val imageUri = data?.data
            imageUri?.let { uri ->
                val bitmap = getBitmapFromUri(uri)
                bitmap?.let {
                    val processedBitmap = preprocessBitmap(it)
                    selectedImages.add(processedBitmap)
                    receiptImageAdapter.notifyDataSetChanged()
                    recognizeTextFromImage(processedBitmap)
                }
            }
        }
    }

    // Новое: настройка RecyclerView для отображения выбранных изображений
    private fun setupReceiptImagesRecyclerView() {
        // Предполагается, что в layout есть RecyclerView с id rvReceiptImages
        receiptImageAdapter = ReceiptImageAdapter(selectedImages) { position ->
            // Удаление изображения по нажатию кнопки "удалить"
            selectedImages.removeAt(position)
            receiptImageAdapter.notifyItemRemoved(position)
        }
        binding.rvReceiptImages.layoutManager = GridLayoutManager(requireContext(), 3)
        binding.rvReceiptImages.adapter = receiptImageAdapter
    }

    private fun getBitmapFromUri(uri: Uri): Bitmap? {
        return try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Ошибка загрузки изображения", Toast.LENGTH_SHORT)
                .show()
            null
        }
    }

    // Предварительная обработка изображения для улучшения качества распознавания
    private fun preprocessBitmap(bitmap: Bitmap): Bitmap {
        // Преобразование в Ч/Б изображение
        val matrix = ColorMatrix()
        matrix.setSaturation(0f)
        val filter = ColorMatrixColorFilter(matrix)
        val paint = Paint().apply { colorFilter = filter }
        val grayscaleBitmap =
            Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(grayscaleBitmap)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)

        // Сжатие изображения до 1024px по большей стороне
        return resizeBitmap(grayscaleBitmap, 1024, 1024)
    }

    // Изменение размера изображения
    private fun resizeBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val (width, height) = if (bitmap.width > maxWidth || bitmap.height > maxHeight) {
            val aspectRatio = bitmap.width.toFloat() / bitmap.height
            when {
                aspectRatio > 1 -> maxWidth to (maxWidth / aspectRatio).toInt()
                else -> (maxHeight * aspectRatio).toInt() to maxHeight
            }
        } else {
            bitmap.width to bitmap.height
        }

        return Bitmap.createScaledBitmap(bitmap, width, height, true).apply {
            setHasAlpha(false)
        }
    }

    private fun recognizeTextFromImage(bitmap: Bitmap) {
        val startTime = System.currentTimeMillis() // Время начала распознавания

        val inputImage = InputImage.fromBitmap(bitmap, 0)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.Builder().build())

        recognizer.process(inputImage)
            .addOnSuccessListener { visionText ->
                val processedText = preprocessText(visionText.text)
                val totalAmount = extractTotalAmount(processedText)

                binding.tvRecognizedAmount.text =
                    "Распознанная сумма: ${totalAmount.toDoubleOrNull() ?: 0.0}"
                updateTransactionAmount(totalAmount)

                val duration = System.currentTimeMillis() - startTime
                Toast.makeText(
                    requireContext(),
                    "Распознавание завершено за ${duration}ms",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    requireContext(),
                    "Ошибка распознавания текста: ${e.localizedMessage}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun preprocessText(text: String): String {
        return text
            .replace(Regex("[©®™•§]"), "")
            .replace(Regex("\\s+"), " ")
            .replace(Regex("(?<=\\d) (?=\\d)"), "")
            .replace(Regex("o", RegexOption.IGNORE_CASE), "0")
            .replace(Regex("[`´‘’′]"), "'")
    }

    private fun extractTotalAmount(text: String): String {
        val patterns = listOf(
            Regex(
                """(итого|всего|сумма|к\s+оплате|total)\s*[:-]?\s*([\d\s]+[.,]\d{2})\b""",
                RegexOption.IGNORE_CASE
            ),
            Regex("""([€$₽])\s*([\d\s]+[.,]\d{2})\b"""),
            Regex("""\b(\d{1,3}(?:[ ,]\d{3})*[.,]\d{2})(?=\s*(?:руб|р|usd|€|\$))"""),
            Regex("""\b(\d+[.,]\d{2})\b""")
        )
        patterns.forEach { regex ->
            regex.findAll(text).lastOrNull()?.let {
                return it.groupValues.last()
                    .replace(Regex("""[ ,]"""), "")
                    .replace(',', '.')
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

    // Оптимизированная валидация с проверкой корректности суммы
    private fun validateInput(): Boolean {
        val name = binding.etTransactionName.text?.toString()
        val amountStr = binding.etTransactionAmount.text?.toString()
        val customCategory = binding.etCustomCategory?.text?.toString() ?: ""
        if (name.isNullOrBlank()) {
            binding.etTransactionName.error = "Введите название"
            return false
        }
        if (amountStr.isNullOrBlank() || amountStr.toDoubleOrNull() == null || amountStr.toDouble() <= 0) {
            binding.etTransactionAmount.error = "Введите корректную сумму"
            return false
        }
        // Если выбрана опция "Другое", проверяем, что пользователь ввёл свою категорию
        if (binding.spinnerCategory.text.toString() == "Другое" && customCategory.isBlank()) {
            Toast.makeText(requireContext(), "Введите свою категорию", Toast.LENGTH_SHORT).show()
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
        ByteArrayOutputStream().use { baos ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
            val byteArray = baos.toByteArray()
            return Base64.encodeToString(byteArray, Base64.DEFAULT)
        }
    }

    // Новая внутренняя реализация адаптера для RecyclerView с изображениями чеков
    inner class ReceiptImageAdapter(
        private val images: List<Bitmap>,
        private val onDelete: (Int) -> Unit
    ) : RecyclerView.Adapter<ReceiptImageAdapter.ImageViewHolder>() {

        inner class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val imageView = itemView.findViewById<android.widget.ImageView>(R.id.ivReceiptImage)
            val btnDelete = itemView.findViewById<android.widget.ImageButton>(R.id.btnDeleteImage)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_receipt_image, parent, false)
            return ImageViewHolder(view)
        }

        override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
            holder.imageView.setImageBitmap(images[position])
            holder.btnDelete.setOnClickListener {
                onDelete(position)
            }
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

        fun newInstance(
            projectId: String,
            transaction: TemporaryTransaction? = null
        ): AddTransactionDialogFragment {
            val fragment = AddTransactionDialogFragment()
            val args = Bundle()
            args.putParcelable(ARG_TRANSACTION, AddTransactionArgs(projectId, transaction))
            fragment.arguments = args
            return fragment
        }
    }
}
