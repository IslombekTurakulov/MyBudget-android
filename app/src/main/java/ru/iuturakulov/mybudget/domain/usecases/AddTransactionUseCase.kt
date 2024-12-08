package ru.iuturakulov.mybudget.domain.usecases

import ru.iuturakulov.mybudget.data.local.entities.TransactionEntity
import ru.iuturakulov.mybudget.domain.repositories.TransactionRepository
import javax.inject.Inject

class AddTransactionUseCase @Inject constructor(
    private val repository: TransactionRepository
) {

    suspend operator fun invoke(transaction: TransactionEntity) {
        // repository.addTransaction(transaction)
    }
}