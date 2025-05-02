package ru.iuturakulov.mybudget.ui.login

import android.util.Patterns
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import ru.iuturakulov.mybudget.R
import ru.iuturakulov.mybudget.databinding.FragmentLoginBinding
import ru.iuturakulov.mybudget.ui.BaseFragment
import ru.iuturakulov.mybudget.ui.transactions.AddTransactionDialogFragment

@AndroidEntryPoint
class LoginFragment : BaseFragment<FragmentLoginBinding>(R.layout.fragment_login) {

    private val viewModel: LoginViewModel by viewModels()

    override fun getViewBinding(view: View) = FragmentLoginBinding.bind(view)

    override fun setupViews() {
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text?.toString().orEmpty().trim()
            val password = binding.etPassword.text?.toString().orEmpty().trim()
            val emailPattern = Patterns.EMAIL_ADDRESS

            if (email.isBlank() || !emailPattern.matcher(email).matches()) {
                binding.tilEmail.error = getString(R.string.error_invalid_email)
                return@setOnClickListener
            } else {
                binding.tilEmail.error = null
            }

            if (password.isBlank()) {
                binding.tilPassword.error = getString(R.string.error_empty_password)
                return@setOnClickListener
            } else {
                binding.tilPassword.error = null
            }

            viewModel.login(email, password)
        }

        binding.tvForgotPassword.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_resetPassword)
        }

        binding.tvRegister.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_register)
        }
    }

    override fun setupObservers() {
        viewModel.loginState.observe(viewLifecycleOwner) { state ->
            binding.progressBar.isVisible = state is LoginViewModel.LoginState.Loading
            binding.btnLogin.isEnabled = state !is LoginViewModel.LoginState.Loading

            when (state) {
                is LoginViewModel.LoginState.Success -> {
                    findNavController().navigate(R.id.action_login_to_projects)
                }

                is LoginViewModel.LoginState.Error -> {
                    Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                }

                else -> Unit
            }
        }
    }
}
