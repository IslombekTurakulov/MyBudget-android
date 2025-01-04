package ru.iuturakulov.mybudget.ui.splash

import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import ru.iuturakulov.mybudget.R
import ru.iuturakulov.mybudget.auth.TokenStorage
import ru.iuturakulov.mybudget.databinding.FragmentSplashBinding
import ru.iuturakulov.mybudget.ui.BaseFragment
import javax.inject.Inject

@AndroidEntryPoint
class SplashFragment : BaseFragment<FragmentSplashBinding>(R.layout.fragment_splash) {

    @Inject
    lateinit var tokenStorage: TokenStorage

    override fun getViewBinding(view: View): FragmentSplashBinding {
        return FragmentSplashBinding.bind(view)
    }

    override fun setupViews() {
        Handler(Looper.getMainLooper()).postDelayed({
            if (this::tokenStorage.isInitialized && tokenStorage.getToken() != null) {
                findNavController().navigate(R.id.action_splash_to_projects)
            } else {
                findNavController().navigate(R.id.action_splash_to_login)
            }
        }, 2000) // 2 секунды задержки для визуализации загрузки
    }

    override fun setupObservers() {
        // Здесь наблюдателей нет, т.к. вся логика в setupViews()
    }
}

