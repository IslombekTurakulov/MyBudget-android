package ru.iuturakulov.mybudget.ui

import android.os.Bundle
import android.view.View
import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding

abstract class BaseFragment<VB : ViewBinding>(
    @LayoutRes layoutRes: Int
) : Fragment(layoutRes) {

    private var _binding: VB? = null
    protected val binding: VB get() = _binding!!

    protected abstract fun getViewBinding(view: View): VB

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = getViewBinding(view)
        setupViews()
        setupObservers()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    protected open fun setupViews() {}
    protected open fun setupObservers() {}
}