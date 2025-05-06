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
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import ru.iuturakulov.mybudget.R
import ru.iuturakulov.mybudget.auth.TokenStorage
import ru.iuturakulov.mybudget.databinding.FragmentSplashBinding
import ru.iuturakulov.mybudget.ui.BaseFragment
import javax.inject.Inject

@AndroidEntryPoint
class SplashFragment : BaseFragment<FragmentSplashBinding>(R.layout.fragment_splash) {

    @Inject lateinit var tokenStorage: TokenStorage

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
                vb.lottieSplash.setAnimation(R.raw.lottie_safe_money)
                vb.lottieSplash.playAnimation()
            }

        vb.lottieSplash.addAnimatorListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                viewLifecycleOwner.lifecycleScope.launch {
                    val token = tokenStorage.getAccessTokenFlow().firstOrNull()
                    if (token != null) {
                        findNavController().navigate(R.id.action_splash_to_projects)
                    } else {
                        findNavController().navigate(R.id.action_splash_to_login)
                    }
                }
            }
        })
    }
}
