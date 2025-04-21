package ru.iuturakulov.mybudget.ui.transactions

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.drawable.ColorDrawable
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Parcel
import android.os.Parcelable
import android.provider.MediaStore
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.accessibility.AccessibilityEventCompat.setAction
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.chrisbanes.photoview.PhotoView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dagger.hilt.android.AndroidEntryPoint
import ru.iuturakulov.mybudget.R
import ru.iuturakulov.mybudget.data.local.entities.TemporaryTransaction
import ru.iuturakulov.mybudget.data.local.entities.TransactionEntity
import ru.iuturakulov.mybudget.data.remote.dto.ParticipantRole
import ru.iuturakulov.mybudget.databinding.DialogAddTransactionBinding
import ru.iuturakulov.mybudget.ui.projects.details.EmojiPickerAdapter
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@AndroidEntryPoint
class AddTransactionDialogFragment : DialogFragment() {

    private var _binding: DialogAddTransactionBinding? = null
    private val binding get() = _binding!!

    private var args: AddTransactionArgs? = null
    private var transaction: TemporaryTransaction? = null
    private var onTransactionAdded: ((TemporaryTransaction) -> Unit)? = null
    private var onTransactionUpdated: ((TemporaryTransaction) -> Unit)? = null
    private var onTransactionDeleted: (() -> Unit)? = null

    private var currentDateMillis: Long = MaterialDatePicker.todayInUtcMilliseconds()
    private lateinit var dateFormatter: SimpleDateFormat

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
        dateFormatter = SimpleDateFormat("dd MMMM yyyy HH:mm", Locale.getDefault())
        setupReceiptImagesRecyclerView() // Инициализация RecyclerView для изображений
//        setupViews()
        setupViewsBasedOnRole() // Метод для настройки в зависимости от роли
        setupToolbar()
    }

    private fun setupViewsBasedOnRole() {
        args?.let { arguments ->
            when (ParticipantRole.valueOf(arguments.currentRole)) {
                ParticipantRole.OWNER -> setupEditMode(arguments)
                ParticipantRole.EDITOR -> setupEditorMode(arguments)
                ParticipantRole.VIEWER -> setupViewMode(arguments)
            }
        }
    }

    private fun setupEditMode(arguments: AddTransactionArgs) {
        // Полный доступ для владельцев и администраторов
        binding.apply {
            // Разблокируем все поля
            etTransactionName.isEnabled = true
            etTransactionAmount.isEnabled = true
            spinnerCategory.isEnabled = true
            ivTransactionCategoryIcon.isEnabled = true
            toggleTransactionType.isEnabled = true
            btnScanReceipt.isEnabled = true
            btnSave.isEnabled = true
            btnDeleteTransaction.isEnabled = arguments.transaction != null
            btnEditTransaction.isEnabled = true
            etTransactionName.isEnabled = true
            etTransactionDate.isEnabled = true
        }

        setupCommonViews(arguments)
    }

    private fun setupEditorMode(arguments: AddTransactionArgs) {
        // Ограниченный доступ для редакторов
        binding.apply {
            // Разблокируем основные поля, но запрещаем удаление
            etTransactionName.isEnabled = true
            etTransactionAmount.isEnabled = true
            spinnerCategory.isEnabled = true
            ivTransactionCategoryIcon.isEnabled = true
            toggleTransactionType.isEnabled = true
            btnScanReceipt.isEnabled = true
            btnSave.isEnabled = true
            btnDeleteTransaction.isEnabled = true // Редакторы могут удалять
            btnEditTransaction.isEnabled = true
            etTransactionName.isEnabled = true
            etTransactionDate.isEnabled = true
            // Показываем кнопку удаления
            btnDeleteTransaction.visibility = View.VISIBLE
        }

        setupCommonViews(arguments)
    }

    private fun setupViewMode(arguments: AddTransactionArgs) {
        // Только просмотр для зрителей
        binding.apply {
            // Блокируем все поля редактирования
            etTransactionName.isEnabled = false
            etTransactionAmount.isEnabled = false
            spinnerCategory.isEnabled = false
            ivTransactionCategoryIcon.isEnabled = false
            toggleTransactionType.isEnabled = false
            btnScanReceipt.isEnabled = false
            btnSave.isEnabled = false
            btnDeleteTransaction.isEnabled = false
            btnEditTransaction.isEnabled = false

            etTransactionName.isEnabled = false
            etTransactionDate.isEnabled = false

            // Скрываем ненужные элементы
            btnScanReceipt.visibility = View.GONE
            btnSave.visibility = View.GONE
            btnDeleteTransaction.visibility = View.GONE
            btnEditTransaction.visibility = View.GONE
            tilCustomCategory.visibility = View.GONE
            dateInputLayout.endIconDrawable = null
            dateInputLayout.setEndIconOnClickListener(null)
            etTransactionDate.setOnClickListener(null)
        }

        setupCommonViews(arguments)
    }

    private fun setupCommonViews(arguments: AddTransactionArgs) {
        transaction = arguments.transaction
        binding.apply {
            if (transaction != null) {
                toolbar.subtitle = ""
                // Режим редактирования/просмотра существующей транзакции
                etTransactionName.setText(transaction?.name)
                etTransactionAmount.setText(transaction?.amount.toString())
                spinnerCategory.setText(transaction?.category, false)
                updateCategoryIcon(transaction?.categoryIcon.orEmpty())
                selectedImages.addAll(
                    transaction?.images?.mapNotNull(::decodeBase64ToBitmap) ?: emptyList()
                )
                receiptImageAdapter.notifyDataSetChanged()
                removeTransactionLayout.visibility = View.VISIBLE
                addTransactionLayout.visibility = View.GONE
                btnSave.text = getString(R.string.save)
                toggleTransactionType.check(
                    if (transaction?.type == TransactionEntity.TransactionType.INCOME) R.id.btnIncome
                    else R.id.btnExpense
                )
                btnEditTransaction.setOnClickListener {
                    processSaveButton(onTransactionUpdated, arguments)
                }
                btnDeleteTransaction.setOnClickListener { showDeleteConfirmationDialog() }
            } else {
                // Режим добавления новой транзакции
                removeTransactionLayout.visibility = View.GONE
                addTransactionLayout.visibility = View.VISIBLE
                btnSave.text = getString(R.string.add)
                btnSave.setOnClickListener { processSaveButton(onTransactionAdded, arguments) }
                toggleTransactionType.check(R.id.btnIncome)
                btnCancel.setOnClickListener { dismiss() }
                setupTransactionIcon()
            }

            setupDatePicker(transaction?.date)
            setupCategorySpinner(transaction?.category)
            setupEmojiPicker()
            setupTransactionTypeToggle()
            btnScanReceipt.setOnClickListener { checkAndRequestPermissions() }
        }
    }

//    private fun setupViews() {
//        args?.let { argument ->
//            transaction = argument.transaction
//            binding.apply {
//                if (transaction != null) {
//                    // Режим редактирования: заполняем поля
//                    etTransactionName.setText(transaction?.name)
//                    etTransactionAmount.setText(transaction?.amount.toString())
//                    spinnerCategory.setText(transaction?.category, false)
//                    updateCategoryIcon(transaction?.categoryIcon.orEmpty())
//                    selectedImages.addAll(
//                        transaction?.images?.mapNotNull(::decodeBase64ToBitmap) ?: emptyList()
//                    )
//                    receiptImageAdapter.notifyDataSetChanged()
//                    removeTransactionLayout.visibility = View.VISIBLE
//                    addTransactionLayout.visibility = View.GONE
//                    btnSave.text = getString(R.string.save)
//                    toggleTransactionType.check(
//                        if (transaction?.type == TransactionEntity.TransactionType.INCOME) R.id.btnIncome
//                        else R.id.btnExpense
//                    )
//                    btnEditTransaction.setOnClickListener {
//                        processSaveButton(
//                            onTransactionUpdated,
//                            argument
//                        )
//                    }
//                    btnDeleteTransaction.setOnClickListener { showDeleteConfirmationDialog() }
//                } else {
//                    // Режим добавления: скрываем кнопку удаления
//                    removeTransactionLayout.visibility = View.GONE
//                    addTransactionLayout.visibility = View.VISIBLE
//                    btnSave.text = getString(R.string.add)
//                    btnSave.setOnClickListener { processSaveButton(onTransactionAdded, argument) }
//                    toggleTransactionType.check(R.id.btnIncome)
//                    btnCancel.setOnClickListener { dismiss() }
//                    setupTransactionIcon()
//                }
//                setupDatePicker(transaction?.date)
//                setupCategorySpinner(transaction?.category)
//                setupEmojiPicker()
//                setupTransactionTypeToggle()
//                btnScanReceipt.setOnClickListener { checkAndRequestPermissions() }
//            }
//        }
//    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            dismiss()
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
            val category =
                if (binding.spinnerCategory.text?.trim().toString().equals("Другое", ignoreCase = true))
                    binding.etCustomCategory.text?.trim().toString() else binding.spinnerCategory.text.trim().toString()
            val temporaryTransaction = TemporaryTransaction(
                id = transaction?.id ?: "${argument.projectId}-${System.currentTimeMillis()}",
                name = binding.etTransactionName.text.toString().trim(),
                amount = binding.etTransactionAmount.text.toString().trim().toDoubleOrNull() ?: 0.0,
                category = category,
                categoryIcon = binding.ivTransactionCategoryIcon.text?.toString().orEmpty(),
                date = getSelectedDateMillis(),
                projectId = argument.projectId,
                userId = transaction?.userId.orEmpty(),
                userName = transaction?.userName.orEmpty(),
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

    private fun setupCategorySpinner(currentCategory: String?) {
        // TODO: перейти на новую фильтрацию
        val categories = resources.getStringArray(R.array.transaction_categories).filter {
            it != "Все"
        }.apply {
            if (currentCategory != null) {
                this.filter { it != "Другое" }
            }
        }.plus(currentCategory).filterNotNull().distinct()
        val adapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categories)
        binding.spinnerCategory.setAdapter(adapter)
        binding.spinnerCategory.setSelection(0)
        binding.spinnerCategory.setOnClickListener { binding.spinnerCategory.showDropDown() }
        binding.spinnerCategory.setOnItemClickListener { _, _, position, _ ->
            val selectedCategory = categories[position]
            binding.tilCustomCategory.visibility =
                if (selectedCategory.equals("Другое", ignoreCase = true))
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
        val cameraPermission =
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
        val storagePermission = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU)
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        else
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_MEDIA_IMAGES
            )
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
        receiptImageAdapter = ReceiptImageAdapter(
            selectedImages,
            onDelete = { position ->
                // Удаление изображения
                selectedImages.removeAt(position)
                receiptImageAdapter.notifyItemRemoved(position)
            },
            onImageClick = { position ->
                // Просмотр изображения
                showFullscreenImage(receiptImageAdapter.getImageAt(position))
            }
        )
        binding.rvReceiptImages.layoutManager = GridLayoutManager(requireContext(), 3)
        binding.rvReceiptImages.adapter = receiptImageAdapter
    }

    private fun showFullscreenImage(bitmap: Bitmap?) {
        if (bitmap == null) return

        val dialog = MaterialAlertDialogBuilder(requireContext(), R.style.FullscreenImageDialog)
            .setView(R.layout.dialog_fullscreen_image)
            .setBackgroundInsetStart(0)
            .setBackgroundInsetEnd(0)
            .create()


        dialog.apply {
            window?.apply {
                setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                statusBarColor = Color.TRANSPARENT
            }

            setOnShowListener {
                val imageView = findViewById<PhotoView>(R.id.fullscreenImageView)
                val btnClose = findViewById<ExtendedFloatingActionButton>(R.id.btnCloseFullscreen)
                val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)

                imageView?.setImageBitmap(bitmap)

                btnClose?.setOnClickListener { dismiss() }

                toolbar?.setNavigationOnClickListener { dismiss() }
                toolbar?.title = "Просмотр чека"

                toolbar?.setOnMenuItemClickListener { menuItem ->
                    when (menuItem.itemId) {
                        R.id.action_share -> {
                            shareImage(bitmap)
                            true
                        }
                        R.id.action_save -> {
                            saveImageToGallery(bitmap)
                            true
                        }
                        else -> false
                    }
                }
            }
        }

        dialog.show()
    }

    // Объявляем лаунчеры для запроса разрешений
    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Повторяем действие после получения разрешения
            when (pendingAction) {
                PendingAction.SHARE -> shareImageInternal(pendingBitmap!!)
                PendingAction.SAVE -> saveImageToGalleryInternal(pendingBitmap!!)
                null -> {
                    // no-op
                }
            }
        } else {
            showMaterialSnackbar("Для этого действия необходимо разрешение на доступ к хранилищу")
        }
    }

    // Enum для отслеживания ожидающего действия
    private enum class PendingAction { SHARE, SAVE }
    private var pendingAction: PendingAction? = null
    private var pendingBitmap: Bitmap? = null

    private fun shareImage(bitmap: Bitmap) {
        if (checkStoragePermission()) {
            shareImageInternal(bitmap)
        } else {
            pendingAction = PendingAction.SHARE
            pendingBitmap = bitmap
            requestStoragePermission()
        }
    }

    private fun saveImageToGallery(bitmap: Bitmap) {
        if (checkStoragePermission()) {
            saveImageToGalleryInternal(bitmap)
        } else {
            pendingAction = PendingAction.SAVE
            pendingBitmap = bitmap
            requestStoragePermission()
        }
    }

    private fun checkStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Для Android 10+ разрешение не требуется
            true
        } else {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestStoragePermission() {
        if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            showPermissionExplanationDialog()
        } else {
            storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }

    private fun showPermissionExplanationDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Требуется разрешение")
            .setMessage("Для сохранения и отправки изображений необходимо разрешение на доступ к хранилищу")
            .setPositiveButton("Разрешить") { _, _ ->
                storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    // Оригинальные методы переименованы в Internal
    private fun shareImageInternal(bitmap: Bitmap) {
        try {
            val cachePath = File(requireContext().externalCacheDir, "shared_image_${System.currentTimeMillis()}.jpg")
            FileOutputStream(cachePath).use { stream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
            }

            val imageUri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                cachePath
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, imageUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            startActivity(
                Intent.createChooser(
                    shareIntent,
                    getString(R.string.share_image_title)
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )

        } catch (e: Exception) {
            showMaterialSnackbar(
                message = "Ошибка при отправке изображения: ${e.localizedMessage}",
                actionText = "Повторить",
                action = { shareImage(bitmap) }
            )
        }
    }

    private fun saveImageToGalleryInternal(bitmap: Bitmap) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveImageToGalleryApi29Plus(bitmap)
        } else {
            saveImageToGalleryLegacy(bitmap)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun saveImageToGalleryApi29Plus(bitmap: Bitmap) {
        val resolver = requireContext().contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "receipt_${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/MyBudget")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }

        try {
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            uri?.let {
                resolver.openOutputStream(it).use { stream ->
                    if (bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream ?: return)) {
                        contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                        resolver.update(uri, contentValues, null, null)
                        showMaterialSnackbar("Изображение сохранено в галерею")
                    }
                }
            }
        } catch (e: Exception) {
            showMaterialSnackbar(
                message = "Ошибка сохранения: ${e.localizedMessage}",
                actionText = "Повторить",
                action = { saveImageToGallery(bitmap) }
            )
        }
    }

    private fun saveImageToGalleryLegacy(bitmap: Bitmap) {
        val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val appDir = File(imagesDir, "MyBudget")
        if (!appDir.exists()) appDir.mkdirs()

        val fileName = "receipt_${System.currentTimeMillis()}.jpg"
        val file = File(appDir, fileName)

        try {
            FileOutputStream(file).use { stream ->
                if (bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)) {
                    MediaScannerConnection.scanFile(
                        requireContext(),
                        arrayOf(file.absolutePath),
                        arrayOf("image/jpeg"),
                        null
                    )
                    showMaterialSnackbar("Изображение сохранено в галерею")
                }
            }
        } catch (e: Exception) {
            showMaterialSnackbar(
                message = "Ошибка сохранения: ${e.localizedMessage}",
                actionText = "Повторить",
                action = { saveImageToGallery(bitmap) }
            )
        }
    }

    private fun showMaterialSnackbar(
        message: String,
        actionText: String? = null,
        action: (() -> Unit)? = null
    ) {
        val rootView = this.requireActivity().window?.decorView?.findViewById<ViewGroup>(android.R.id.content)
        rootView?.let {
            Snackbar.make(it, message, Snackbar.LENGTH_LONG)
                .setAnimationMode(Snackbar.ANIMATION_MODE_FADE)
                .apply {
                    if (actionText != null && action != null) {
                        setAction(actionText) { action.invoke() }
                    }
                }
                .show()
        }
    }

    private fun getBitmapFromUri(uri: Uri): Bitmap? {
        return try {
            requireContext().contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            }
        } catch (e: Exception) {
            Snackbar.make(
                binding.root,
                getString(R.string.error_loading_image),
                Snackbar.LENGTH_SHORT
            ).show()
            null
        }
    }

    // Применение предобработки: преобразование в Ч/Б и изменение размера
    private fun preprocessBitmap(bitmap: Bitmap): Bitmap {
        val grayscale = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(grayscale)
        val paint = Paint().apply {
            colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
        }
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
                binding.tvRecognizedAmount.text =
                    getString(R.string.recognized_amount, totalAmount.toDoubleOrNull() ?: 0.0)
                updateTransactionAmount(totalAmount)
                val duration = System.currentTimeMillis() - startTime
                Snackbar.make(
                    binding.root,
                    "Распознавание завершено за ${duration}ms",
                    Snackbar.LENGTH_SHORT
                ).show()
            }
            .addOnFailureListener { e ->
                Snackbar.make(
                    binding.root,
                    "Ошибка распознавания: ${e.localizedMessage}",
                    Snackbar.LENGTH_SHORT
                ).show()
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
                return it.groupValues.last().replace(Regex("""[ ,]"""), "").replace(',', '.')
            }
        }
        return "0.00"
    }

    private fun updateTransactionAmount(totalAmount: String) {
        val existingAmount = binding.etTransactionAmount.text?.toString()?.toDoubleOrNull() ?: 0.0
        binding.tvRecognizedAmount.visibility = View.VISIBLE
        val recognizedAmount = totalAmount.toDoubleOrNull() ?: 0.0
        val updatedAmount = existingAmount + recognizedAmount
        binding.etTransactionAmount.setText(updatedAmount.toString())
    }

    // Валидация введенных данных
    private fun validateInput(): Boolean {
        val name = binding.etTransactionName.text?.toString()?.trim()
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
        if (binding.spinnerCategory.text.toString()
                .equals("Другое", ignoreCase = true) && customCategory.isBlank()
        ) {
            Snackbar.make(
                binding.root,
                getString(R.string.error_enter_custom_category),
                Snackbar.LENGTH_SHORT
            ).show()
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

    private fun setupDatePicker(serverDateMillis: Long?) {
        // Установка даты из бэкенда или текущей даты
        serverDateMillis?.let {
            currentDateMillis = it
            updateDateDisplay()
        } ?: setDateToToday()

        binding.etTransactionDate.setOnClickListener { showMaterialDateTimePicker() }
        binding.dateInputLayout.setEndIconOnClickListener { showMaterialDateTimePicker() }
    }

    private fun showMaterialDateTimePicker() {
        val todayMillis = MaterialDatePicker.todayInUtcMilliseconds()
        val currentDateTime = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(currentDateMillis),
            ZoneId.systemDefault()
        )
        val now = LocalDateTime.now()

        // Ограничиваем выбор даты до текущего дня
        val constraintsBuilder = CalendarConstraints.Builder()
            .setEnd(todayMillis) // Максимальная дата - сегодня
            .setValidator(object : CalendarConstraints.DateValidator {
                override fun isValid(date: Long): Boolean {
                    return date <= todayMillis
                }

                override fun describeContents(): Int = 0
                override fun writeToParcel(dest: Parcel, flags: Int) {}
            })

        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Выберите дату")
            .setSelection(minOf(currentDateMillis, todayMillis)) // Не позволяем выбрать дату больше сегодняшней
            .setCalendarConstraints(constraintsBuilder.build())
            .setTheme(R.style.ThemeOverlay_Material3_DatePicker)
            .build()

        datePicker.addOnPositiveButtonClickListener { selectedDate ->
            val isToday = selectedDate == todayMillis

            val timePicker = MaterialTimePicker.Builder()
                .setTitleText("Выберите время")
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(currentDateTime.hour)
                .setMinute(currentDateTime.minute)
                .setTheme(R.style.ThemeOverlay_Material3_TimePicker)
                .build()

            timePicker.addOnPositiveButtonClickListener {
                val selectedHour = timePicker.hour
                val selectedMinute = timePicker.minute

                // Если выбрана сегодняшняя дата, проверяем чтобы время не было будущим
                if (isToday) {
                    val selectedTime = now.withHour(selectedHour).withMinute(selectedMinute)
                    if (selectedTime.isAfter(now)) {
                        // Если выбрано будущее время для сегодняшнего дня - показываем ошибку
                        showTimeErrorSnackbar()
                        return@addOnPositiveButtonClickListener
                    }
                }

                val selectedDateTime = LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(selectedDate),
                    ZoneId.systemDefault()
                ).withHour(selectedHour).withMinute(selectedMinute)

                currentDateMillis = selectedDateTime.toInstant(ZoneOffset.UTC).toEpochMilli()
                updateDateDisplay()
            }

            timePicker.show(parentFragmentManager, "TIME_PICKER_TAG")
        }

        datePicker.show(parentFragmentManager, "DATE_PICKER_TAG")
    }

    private fun showTimeErrorSnackbar() {
        Snackbar.make(
            binding.root,
            "Нельзя выбрать время в будущем",
            Snackbar.LENGTH_LONG
        ).setAction("OK") { /* Закрыть снэкбар */ }
            .show()
    }

    private fun updateDateDisplay() {
        val date = Date(currentDateMillis)
        binding.etTransactionDate.setText(dateFormatter.format(date))
    }

    private fun setDateToToday() {
        currentDateMillis = MaterialDatePicker.todayInUtcMilliseconds()
        updateDateDisplay()
    }

    fun getSelectedDateMillis(): Long {
        return currentDateMillis
    }



    companion object {
        private const val REQUEST_IMAGE_CAPTURE = 1
        private const val ARG_TRANSACTION = "arg_transaction"

        @kotlinx.parcelize.Parcelize
        data class AddTransactionArgs(
            val projectId: String,
            var transaction: TemporaryTransaction? = null,
            var currentRole: String
        ) : Parcelable

        fun newInstance(
            projectId: String,
            currentRole: String,
            transaction: TemporaryTransaction? = null
        ): AddTransactionDialogFragment {
            val fragment = AddTransactionDialogFragment()
            val args = Bundle().apply {
                putParcelable(
                    ARG_TRANSACTION,
                    AddTransactionArgs(
                        projectId = projectId,
                        transaction = transaction,
                        currentRole = currentRole
                    )
                )
            }
            fragment.arguments = args
            return fragment
        }
    }
}
