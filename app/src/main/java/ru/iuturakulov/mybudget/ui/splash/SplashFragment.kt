package ru.iuturakulov.mybudget.ui.splash

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.airbnb.lottie.LottieCompositionFactory
import com.airbnb.lottie.RenderMode
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.lottieSplash.setCacheComposition(true)
        LottieCompositionFactory
            .fromRawRes(requireContext(), R.raw.lottie_safe_money)
            .addListener { composition ->
                binding.lottieSplash.apply {
                    renderMode = RenderMode.HARDWARE
                    setComposition(composition)
                    playAnimation()
                }
            }
            .addFailureListener {
                binding.lottieSplash.setAnimation(R.raw.lottie_safe_money)
                binding.lottieSplash.playAnimation()
            }

        binding.lottieSplash.addAnimatorListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                lifecycleScope.launch {
                    tokenStorage.getAccessTokenFlow().collect { token ->
                        if (token != null) {
                            findNavController().navigate(R.id.action_splash_to_projects)
                        } else {
                            findNavController().navigate(R.id.action_splash_to_login)
                        }
                    }
                }
            }
        })
    }

    override fun setupViews() {
        // no-op
    }

    override fun setupObservers() {
        // Здесь наблюдателей нет, т.к. вся логика в setupViews()
    }
}

