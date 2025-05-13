package ru.iuturakulov.mybudget.ui.privacy

import android.os.Bundle
import android.view.View
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.crashlytics.FirebaseCrashlytics
import ru.iuturakulov.mybudget.R
import ru.iuturakulov.mybudget.databinding.FragmentPrivacyBinding
import ru.iuturakulov.mybudget.ui.BaseFragment
import java.util.Locale

class PrivacyPolicyFragment : BaseFragment<FragmentPrivacyBinding>(R.layout.fragment_privacy) {
    override fun getViewBinding(view: View): FragmentPrivacyBinding {
        return FragmentPrivacyBinding.bind(view)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.webviewPrivacy.settings.apply {
            javaScriptEnabled = false
            domStorageEnabled = false
            builtInZoomControls = true
            displayZoomControls = false
            allowFileAccess = false
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        }

        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        binding.webviewPrivacy.webViewClient = object : WebViewClient() {
            override fun onReceivedError(
                view: WebView, request: WebResourceRequest, error: WebResourceError
            ) {
                super.onReceivedError(view, request, error)
                Snackbar.make(
                    binding.webviewPrivacy,
                    getString(R.string.error_load_privacy_policy),
                    Snackbar.LENGTH_INDEFINITE
                ).setAction(getString(R.string.action_retry)) {
                    binding.webviewPrivacy.reload()
                }.show()

                FirebaseCrashlytics.getInstance()
                    .log("PrivacyPolicy load error: code=${error.errorCode}, desc=${error.description}")
            }
        }

        val lang = Locale.getDefault().language
        val assetFile = when (lang) {
            "ru", "be" -> "privacy_policy_ru.html"
            else -> "privacy_policy_en.html"
        }
        binding.webviewPrivacy.loadUrl("file:///android_asset/$assetFile")
    }
}
