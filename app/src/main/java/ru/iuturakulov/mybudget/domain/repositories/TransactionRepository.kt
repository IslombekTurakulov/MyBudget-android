package ru.iuturakulov.mybudget.domain.repositories

import ru.iuturakulov.mybudget.data.local.daos.TransactionDao
import ru.iuturakulov.mybudget.data.local.entities.TransactionEntity
import javax.inject.Inject

class TransactionRepository @Inject constructor(
    private val transactionDao: TransactionDao
) {

    suspend fun getTransactionsForProject(projectId: Int): List<TransactionEntity> {
        return transactionDao.getTransactionsForProject(projectId)
    }

    suspend fun addTransaction(transaction: TransactionEntity) {
        transactionDao.insertTransaction(transaction)
    }

    suspend fun deleteTransaction(transactionId: Int) {
        transactionDao.deleteTransaction(transactionId)
    }
}