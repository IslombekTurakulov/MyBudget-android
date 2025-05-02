package ru.iuturakulov.mybudget.ui.register

import android.graphics.Color
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.Patterns
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.textfield.TextInputLayout
import dagger.hilt.android.AndroidEntryPoint
import ru.iuturakulov.mybudget.R
import ru.iuturakulov.mybudget.databinding.FragmentRegisterBinding
import ru.iuturakulov.mybudget.ui.BaseFragment

@AndroidEntryPoint
class RegisterFragment : BaseFragment<FragmentRegisterBinding>(R.layout.fragment_register) {

    private val viewModel: RegisterViewModel by viewModels()

    override fun getViewBinding(view: View) = FragmentRegisterBinding.bind(view)

    override fun setupViews() = binding.run {
        tvAlreadyRegistered.setOnClickListener { findNavController().navigateUp() }
        toolbar.setNavigationOnClickListener { findNavController().navigateUp() }

        val part1 = getString(R.string.policy_part1)
        val link = getString(R.string.policy_link)
        val full = getString(R.string.policy_html, part1, link)
        cbPolicy.setClickableLink(full, link) {
            findNavController().navigate(R.id.action_privacy_policy)
        }

        btnRegister.setOnClickListener {
            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString()
            val confirmPassword = etConfirm.text.toString()

            if (validateInputs(name, email, password, confirmPassword)) {
                viewModel.register(name, email, password)
            }
        }
    }

    override fun setupObservers() {
        viewModel.registerState.observe(viewLifecycleOwner) { state ->
            binding.progressBar.isVisible = state is RegisterViewModel.RegisterState.Loading
            binding.btnRegister.isEnabled = state !is RegisterViewModel.RegisterState.Loading

            when (state) {
                is RegisterViewModel.RegisterState.Success -> {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.success),
                        Toast.LENGTH_SHORT
                    ).show()
                    findNavController().navigate(R.id.action_register_to_login)
                }

                is RegisterViewModel.RegisterState.Error -> {
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                }

                else -> Unit
            }
        }
    }

    private fun validateInputs(
        name: String,
        email: String,
        password: String,
        confirmPassword: String
    ): Boolean {
        fun showError(inputLayout: TextInputLayout, error: String?): Boolean {
            inputLayout.error = error
            inputLayout.isErrorEnabled = error != null
            if (error != null) inputLayout.requestFocus()
            return error == null
        }

        if (!showError(
                binding.tilName,
                getString(R.string.error_empty_name).takeIf { name.isBlank() }
            )
        ) return false

        if (!showError(
                binding.tilName,
                getString(R.string.error_name_too_short).takeIf { name.length < 4 }
            )
        ) return false

        val emailPattern = Patterns.EMAIL_ADDRESS
        if (!showError(
                binding.tilEmail,
                when {
                    email.isBlank() -> getString(R.string.error_empty_email)
                    !emailPattern.matcher(email)
                        .matches() -> getString(R.string.error_invalid_email)

                    else -> null
                }
            )
        ) return false

        if (!showError(
                binding.tilPassword,
                when {
                    password.isBlank() -> getString(R.string.error_empty_password)
                    password.length < 6 -> getString(R.string.error_short_password)
                    else -> null
                }
            )
        ) return false

        if (!showError(
                binding.tilConfirm,
                when {
                    confirmPassword.isBlank() -> getString(R.string.error_empty_confirm)
                    confirmPassword != password -> getString(R.string.error_password_mismatch)
                    else -> null
                }
            )
        ) return false

        if (!binding.cbPolicy.isChecked) {
            Toast.makeText(
                requireContext(),
                getString(R.string.error_policy_required),
                Toast.LENGTH_SHORT
            ).show()
            return false
        }

        return true
    }


    fun TextView.setClickableLink(
        fullText: String,
        linkText: String,
        @ColorRes linkColor: Int = android.R.color.holo_blue_dark,
        onLinkClick: (View) -> Unit
    ) {
        val spannable = SpannableString(fullText)
        val start = fullText.indexOf(linkText).takeIf { it >= 0 } ?: return
        val end = start + linkText.length

        spannable.setSpan(object : ClickableSpan() {
            override fun onClick(widget: View) {
                onLinkClick(widget)
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = true
                ds.color = ContextCompat.getColor(context, linkColor)
            }
        }, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        text = spannable
        movementMethod = LinkMovementMethod.getInstance()
        highlightColor = Color.TRANSPARENT

        setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val tv = v as TextView
                val offset = tv.getOffsetForPosition(event.x, event.y)
                val spans = (tv.text as Spanned).getSpans(offset, offset, ClickableSpan::class.java)
                if (spans.isNotEmpty()) {
                    return@setOnTouchListener false
                }
            }
            false
        }
    }
}
