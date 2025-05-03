package ru.iuturakulov.mybudget.core

import android.view.View
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

fun View.setOnDebounceClick(
    intervalMs: Long = 600L,
    scope: CoroutineScope = MainScope(),
    onClick: (View) -> Unit
) {
    var debounceJob: Job? = null

    setOnClickListener { v ->
        if (debounceJob?.isActive == true) return@setOnClickListener

        debounceJob = scope.launch {
            onClick(v)
            delay(intervalMs)
        }
    }
}