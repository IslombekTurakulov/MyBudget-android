package ru.iuturakulov.mybudget.ui.splash

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.core.view.isGone
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
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class SplashFragment : BaseFragment<FragmentSplashBinding>(R.layout.fragment_splash) {

    @Inject
    lateinit var tokenStorage: TokenStorage

    override fun getViewBinding(view: View) = FragmentSplashBinding.bind(view)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val vb = binding

        vb.lottieSplash.setCacheComposition(true)
        LottieCompositionFactory
            .fromRawRes(requireContext(), R.raw.lottie_safe_money)
            .addListener { composition ->
                vb.lottieSplash.apply {
                    renderMode = RenderMode.HARDWARE
                    setComposition(composition)
                    playAnimation()
                }
            }
            .addFailureListener {
                vb.lottieSplash.isGone = true
            }

        vb.lottieSplash.addAnimatorListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                // no-op
            }
        })
    }

    override fun setupViews() {
        lifecycleScope.launch {
            try {
                tokenStorage.getAccessTokenFlow().collect { token ->
                    Handler(Looper.getMainLooper()).postDelayed({
                        val navController = findNavController()
                        val currentDestination = navController.currentDestination?.id

                        if (currentDestination == R.id.splashFragment) {
                            if (token != null) {
                                navController.navigate(R.id.action_splash_to_projects)
                            } else {
                                navController.navigate(R.id.action_splash_to_login)
                            }
                        }
                    }, 2000)
                }
            } catch (e: Exception) {
                val currentDestination = findNavController().currentDestination?.label
                Timber.e("Destination: ${currentDestination}, Navigation error $e")
            }
        }
    }
}
