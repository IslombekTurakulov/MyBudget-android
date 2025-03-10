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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
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
import ru.iuturakulov.mybudget.data.local.entities.TemporaryTransaction
import ru.iuturakulov.mybudget.databinding.DialogAddTransactionBinding
import ru.iuturakulov.mybudget.ui.projects.details.EmojiPickerAdapter

@AndroidEntryPoint
class AddTransactionDialogFragment : DialogFragment() {

    private var _binding: DialogAddTransactionBinding? = null
    private val binding get() = _binding!!

    private var args: AddTransactionArgs? = null
    private var onTransactionAdded: ((TemporaryTransaction) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAddTransactionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        args = arguments?.getParcelable(ARG_TRANSACTION)
        setupViews()
    }

    private fun setupViews() {
        args?.let { argument ->
            setupCategorySpinner()
            setupEmojiPicker()

            // Кнопка "Сканировать чек"
            binding.btnScanReceipt.setOnClickListener {
                checkAndRequestPermissions()
            }

            // Кнопка "Сохранить"
            binding.btnSave.setOnClickListener {
                if (validateInput()) {
                    val temporaryTransaction = TemporaryTransaction(
                        id = "${argument.projectId}-${System.currentTimeMillis()}".hashCode()
                            .toString(),
                        name = binding.etTransactionName.text.toString(),
                        amount = binding.etTransactionAmount.text.toString().toDouble(),
                        category = binding.spinnerCategory.text.toString(),
                        categoryIcon = binding.ivTransactionCategoryIcon.tag?.toString() ?: "",
                        date = System.currentTimeMillis(),
                        projectId = argument.projectId,
                        userId = ""
                    )
                    onTransactionAdded?.invoke(temporaryTransaction)
                    dismiss()
                }
            }

            // Кнопка "Отмена"
            binding.btnCancel.setOnClickListener {
                dismiss()
            }
        }
    }

    private fun setupCategorySpinner() {
        val categories = listOf("Еда", "Транспорт", "Развлечения", "Прочее") // Категории
        val adapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categories)
        binding.spinnerCategory.setAdapter(adapter)

        // Установка иконки категории при выборе
        binding.spinnerCategory.setOnItemClickListener { _, _, position, _ ->
            val selectedCategory = categories[position]
            val icon = getCategoryIcon(selectedCategory)
            binding.ivTransactionCategoryIcon.text = icon // Устанавливаем эмодзи
            binding.ivTransactionCategoryIcon.tag =
                selectedCategory // Сохраняем выбранную категорию
        }
    }

    private fun getCategoryIcon(category: String): String {
        return when (category) {
            "Еда" -> "🍕"
            "Транспорт" -> "🚗"
            "Развлечения" -> "🎉"
            "Прочее" -> "💼"
            else -> "❓"
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
        val emojis = listOf("😊", "🚗", "🍕", "🎉", "💵", "📈", "🛒", "✈️")
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

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val bitmap = getBitmapFromUri(uri)
            bitmap?.let {
                binding.ivReceiptPreview.setImageBitmap(preprocessBitmap(it)) // Отображаем изображение в превью
                recognizeTextFromImage(preprocessBitmap(it)) // Распознаем текст
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
                    binding.ivReceiptPreview.setImageBitmap(it) // Отображаем изображение в превью
                    recognizeTextFromImage(preprocessBitmap(it)) // Распознаем текст
                }
            }
        }
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
        // Уменьшаем насыщенность (ч/б изображение)
        val matrix = ColorMatrix()
        matrix.setSaturation(0f) // Устанавливаем насыщенность на 0 (черно-белое изображение)
        val filter = ColorMatrixColorFilter(matrix)
        val paint = Paint().apply { colorFilter = filter }
        val grayscaleBitmap =
            Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(grayscaleBitmap)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)

        // Сжимаем изображение до 1024px по большей стороне для ускорения обработки
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

        return Bitmap.createScaledBitmap(
            bitmap,
            width,
            height,
            true
        ).apply {
            setHasAlpha(false) // Убираем альфа-канал для лучшей обработки
        }
    }

    private fun recognizeTextFromImage(bitmap: Bitmap) {
        val startTime = System.currentTimeMillis() // Время начала распознавания

        val inputImage = InputImage.fromBitmap(bitmap, 0)
        val multiLanguageOptions = TextRecognizerOptions.Builder().build()

        val recognizer = TextRecognition.getClient(multiLanguageOptions)

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
            .replace(Regex("[©®™•§]"), "") // Удаление специальных символов
            .replace(Regex("\\s+"), " ")   // Нормализация пробелов
            .replace(Regex("(?<=\\d) (?=\\d)"), "") // Удаление пробелов внутри чисел
            .replace(Regex("o", RegexOption.IGNORE_CASE), "0") // Замена буквы O на 0
            .replace(Regex("[`´‘’′]"), "'") // Нормализация апострофов
    }

    private fun extractTotalAmount(text: String): String {
        val patterns = listOf(
            // Основные паттерны
            Regex(
                """(итого|всего|сумма|к\s+оплате|total)\s*[:-]?\s*([\d\s]+[.,]\d{2})\b""",
                RegexOption.IGNORE_CASE
            ),
            Regex("""([€$₽])\s*([\d\s]+[.,]\d{2})\b"""),
            Regex("""\b(\d{1,3}(?:[ ,]\d{3})*[.,]\d{2})(?=\s*(?:руб|р|usd|€|\$))"""),

            // Резервные паттерны
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

    private fun validateInput(): Boolean {
        val name = binding.etTransactionName.text?.toString()
        val amount = binding.etTransactionAmount.text?.toString()

        if (name.isNullOrBlank()) {
            binding.etTransactionName.error = "Введите название"
            return false
        }

        if (amount.isNullOrBlank() || amount.toDoubleOrNull() == null) {
            binding.etTransactionAmount.error = "Введите корректную сумму"
            return false
        }

        return true
    }

    fun setOnTransactionAdded(listener: (TemporaryTransaction) -> Unit) {
        onTransactionAdded = listener
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val REQUEST_IMAGE_CAPTURE = 1
        private const val ARG_TRANSACTION = "arg_transaction"

        @kotlinx.parcelize.Parcelize
        data class AddTransactionArgs(
            val projectId: String,
        ) : Parcelable

        fun newInstance(projectId: String): AddTransactionDialogFragment {
            val fragment = AddTransactionDialogFragment()
            val args = Bundle()
            args.putParcelable(ARG_TRANSACTION, AddTransactionArgs(projectId))
            fragment.arguments = args
            return fragment
        }
    }
}

