package ru.iuturakulov.mybudget.ui.projects.participants

import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import dagger.hilt.android.AndroidEntryPoint
import ru.iuturakulov.mybudget.R
import ru.iuturakulov.mybudget.core.setOnDebounceClick
import ru.iuturakulov.mybudget.data.remote.dto.InvitationRequest
import ru.iuturakulov.mybudget.data.remote.dto.InvitationRequest.InvitationType
import ru.iuturakulov.mybudget.data.remote.dto.ParticipantRole
import ru.iuturakulov.mybudget.databinding.DialogAddParticipantBinding
import java.io.File
import java.io.FileOutputStream

@AndroidEntryPoint
class AddParticipantDialogFragment : DialogFragment() {

    private var _binding: DialogAddParticipantBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProjectParticipantsViewModel by viewModels({ requireParentFragment() })

    private var projectId: String? = null
    private var onParticipantAdded: (() -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAddParticipantBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.95).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupArguments()
        setupUI()
        setupObservers()
    }

    private fun setupArguments() {
        projectId = arguments?.getString(ARG_PROJECT_ID)?.takeIf { it.isNotBlank() }
            ?: run {
                dismiss()
                return
            }
    }

    private fun setupUI() {
        setupRoleDropdown()
        setupInviteTypeToggle()
        setupListeners()
    }

    private fun setupRoleDropdown() {
        val roles = ParticipantRole.values()
            .filter { it.displayNameRes != R.string.role_admin }
            .map { it.getDisplayName(requireContext()) }

        val adapter = object : ArrayAdapter<String>(
            requireContext(),
            com.google.android.material.R.layout.support_simple_spinner_dropdown_item,
            roles
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent) as TextView
                view.setTextColor(ContextCompat.getColor(context, R.color.selector_dropdown_text))
                return view
            }
        }

        binding.spinnerRole.setAdapter(adapter)
    }

    private fun setupInviteTypeToggle() {
        // По умолчанию — приглашение по Email
        binding.toggleInviteType.check(R.id.btnInviteByEmail)
        showEmailInput()

        binding.toggleInviteType.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            when (checkedId) {
                R.id.btnInviteByEmail -> {
                    showEmailInput()
                    binding.btnSendInvite.text = getString(R.string.send_invitation)
                }
                R.id.btnInviteByQr -> {
                    showQrInput()
                    binding.btnSendInvite.text = getString(R.string.generate_qr)
                }
            }
        }
    }

    private fun showEmailInput() {
        binding.flEmailContainer.visibility = View.VISIBLE
        binding.flQrContainer.visibility = View.GONE
        binding.tilParticipantEmail.error = null
    }

    private fun showQrInput() {
        binding.flEmailContainer.visibility = View.GONE
        binding.flQrContainer.visibility = View.VISIBLE
        // Очистить предыдущий QR-код и текст
        binding.ivQrCode.setImageDrawable(null)
        binding.ivQrCode.setOnClickListener(null)

        binding.tvInviteCode.text = getString(R.string.invite_code_label, "")
        binding.tvInviteCode.setOnClickListener(null)
    }

    private fun setupListeners() {
        binding.btnSendInvite.setOnClickListener {
            if (validateInput()) {
                sendInvitation()
            }
        }
        binding.tilParticipantEmail.setEndIconOnClickListener {
            binding.etParticipantEmail.text?.clear()
        }
    }

    private fun validateInput(): Boolean {
        val isEmailMode = binding.toggleInviteType.checkedButtonId == R.id.btnInviteByEmail
        if (isEmailMode) {
            if (!isEmailValid()) {
                binding.tilParticipantEmail.error = getString(R.string.error_invalid_email)
                return false
            }
        }
        if (!isRoleSelected()) {
            showRoleError()
            return false
        }
        return true
    }

    private fun isEmailValid(): Boolean {
        val email = binding.etParticipantEmail.text?.toString()?.trim()
        return !email.isNullOrBlank() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun isRoleSelected(): Boolean =
        binding.spinnerRole.text?.isNotBlank() == true

    private fun showRoleError() {
        Toast.makeText(
            requireContext(),
            getString(R.string.error_select_role),
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun sendInvitation() {
        binding.btnSendInvite.isEnabled = false
        binding.progressBar.visibility = View.VISIBLE

        val isEmailMode = binding.toggleInviteType.checkedButtonId == R.id.btnInviteByEmail
        val email = if (isEmailMode) binding.etParticipantEmail.text.toString().trim() else null
        val roleName = binding.spinnerRole.text.toString()
        val role = ParticipantRole.fromDisplayName(requireContext(), roleName)
            ?: return showRoleError()

        val request = InvitationRequest(
            projectId = projectId!!,
            email = email,
            type = if (isEmailMode) InvitationType.MANUAL else InvitationType.QR,
            role = role.name
        )

        viewModel.sendInvitation(request = request)
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.invitationState.collect { state ->
                when (state) {
                    is ProjectParticipantsViewModel.InvitationState.Loading -> {
                        // уже показали прогресс
                    }
                    is ProjectParticipantsViewModel.InvitationState.Success -> {
                        binding.progressBar.visibility = View.GONE
                        binding.btnSendInvite.isEnabled = true

                        if (state.data?.qrCodeBase64 != null) {
                            showQrInput()
                            // Показываем QR-код и код приглашения
                            state.data.qrCodeBase64.let { base64 ->
                                val bytes = Base64.decode(base64, Base64.DEFAULT)
                                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                binding.ivQrCode.setImageBitmap(bmp)
                                binding.ivQrCode.setOnClickListener {
                                    showQrFullScreen(bmp)
                                }
                            }
                            binding.tvInviteCode.text = getString(
                                R.string.invite_code_label,
                                state.data.inviteCode
                            )
                            binding.tvInviteCode.setOnClickListener {
                                copyToClipboard(state.data.inviteCode.toString())
                            }
                        } else {
                            Toast.makeText(
                                requireContext(),
                                getString(R.string.invitation_sent_success),
                                Toast.LENGTH_SHORT
                            ).show()
                            onParticipantAdded?.invoke()
                            dismiss()
                        }
                    }
                    is ProjectParticipantsViewModel.InvitationState.Error -> {
                        binding.progressBar.visibility = View.GONE
                        binding.btnSendInvite.isEnabled = true
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.invitation_sent_error, state.message),
                            Toast.LENGTH_LONG
                        ).show()
                    }

                    ProjectParticipantsViewModel.InvitationState.Idle -> {
                        // no-op
                    }
                }
            }
        }
    }

    private fun copyToClipboard(text: String) {
        val clipboard = ContextCompat.getSystemService(requireContext(), ClipboardManager::class.java)
        val clip = ClipData.newPlainText("InviteLink", text)
        clipboard?.setPrimaryClip(clip)
        Toast.makeText(requireContext(), getString(R.string.copied_to_clipboard), Toast.LENGTH_SHORT).show()
    }

    private fun showQrFullScreen(bitmap: Bitmap) {
        val dialog = Dialog(requireContext(), android.R.style.Theme_DeviceDefault_Light_NoActionBar_Fullscreen)
        dialog.setContentView(R.layout.dialog_qr_fullscreen)

        val iv = dialog.findViewById<ImageView>(R.id.ivFullscreenQr)
        val btnClose = dialog.findViewById<MaterialButton>(R.id.btnClose)
        val btnShare = dialog.findViewById<MaterialButton>(R.id.btnShareQr)

        btnClose.setOnDebounceClick(300) {
            dialog.dismiss()
        }

        iv.setImageBitmap(bitmap)
        btnShare.setOnClickListener {
            shareQrBitmap(bitmap)
        }

        dialog.setTitle(getString(R.string.send_invitation))
        dialog.show()
    }

    private fun shareQrBitmap(bitmap: Bitmap) {
        val cachePath = File(requireContext().cacheDir, "images").apply { mkdirs() }
        val file = File(cachePath, "qr.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        val uri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            file
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_qr)))
    }

    fun setOnParticipantAdded(listener: () -> Unit) {
        onParticipantAdded = listener
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_PROJECT_ID = "arg_project_id"
        fun newInstance(projectId: String): AddParticipantDialogFragment =
            AddParticipantDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PROJECT_ID, projectId)
                }
            }
    }
}
