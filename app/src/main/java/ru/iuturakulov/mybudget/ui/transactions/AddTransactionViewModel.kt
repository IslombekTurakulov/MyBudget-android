package ru.iuturakulov.mybudget.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import ru.iuturakulov.mybudget.data.local.entities.TemporaryTransaction
import ru.iuturakulov.mybudget.data.local.entities.TransactionEntity.Companion.toTemporaryModel
import ru.iuturakulov.mybudget.domain.repositories.TransactionRepository
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class AddTransactionViewModel @Inject constructor(
    private val repo: TransactionRepository
) : ViewModel() {

    private val _transaction = MutableStateFlow<TemporaryTransaction?>(null)
    val transaction: StateFlow<TemporaryTransaction?> = _transaction

    fun loadTransaction(projectId: String, transactionId: String) {
        viewModelScope.launch {
            try {
                _transaction.value =  repo.getTransactionById(
                    projectId,
                    transactionId
                ).toTemporaryModel()
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }
}