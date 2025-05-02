package ru.iuturakulov.mybudget.domain.repositories

import ru.iuturakulov.mybudget.data.local.daos.TransactionDao
import ru.iuturakulov.mybudget.data.local.entities.TransactionEntity
import ru.iuturakulov.mybudget.data.mappers.TransactionMapper
import ru.iuturakulov.mybudget.data.remote.ProjectService
import ru.iuturakulov.mybudget.data.remote.dto.TransactionDto
import javax.inject.Inject

class TransactionRepository @Inject constructor(
    private val transactionDao: TransactionDao,
    private val projectService: ProjectService
) {

    // Добавление транзакции
    suspend fun addTransaction(projectId: String, transaction: TransactionEntity) {
        try {
            val dto = TransactionMapper.entityToDto(transaction)
            val response = projectService.addTransaction(projectId, dto)

            if (!response.isSuccessful) {
                throw Exception(response.errorBody()?.string())
            }

            val responseBody = response.body()

            requireNotNull(responseBody)

            val updatedTransaction = TransactionMapper.dtoToEntity(responseBody)
            transactionDao.insertTransaction(updatedTransaction)
        } catch (e: Exception) {
            throw Exception(e.localizedMessage)
        }
    }

    // Обновление транзакции
    suspend fun updateTransaction(projectId: String, transaction: TransactionEntity) {
        try {
            val dto = TransactionMapper.entityToDto(transaction)
            val response = projectService.updateTransaction(projectId, transaction.id, dto)

            if (!response.isSuccessful) {
                throw Exception(response.errorBody()?.string())
            }

            val body = response.body()

            requireNotNull(body)

            val updatedTransaction = TransactionMapper.dtoToEntity(body)
            transactionDao.updateTransaction(updatedTransaction) // Обновляем локальный кэш
        } catch (e: Exception) {
            throw Exception(e.localizedMessage)
        }
    }

    // Удаление транзакции
    suspend fun deleteTransaction(projectId: String, transactionId: String) {
        try {
            // Удаление на сервере
            projectService.deleteTransaction(projectId, transactionId)
            // Локальное удаление
            transactionDao.deleteTransaction(transactionId)
        } catch (e: Exception) {
            throw Exception(e.localizedMessage)
        }
    }

    // Синхронизация транзакций с сервером
    suspend fun syncTransactions(projectId: String) {
        try {
            val remoteTransactions = projectService.fetchTransactions(projectId).body()

            requireNotNull(remoteTransactions)

            val entities = remoteTransactions.map { TransactionMapper.dtoToEntity(it) }

            // Очистка локального кэша и обновление из сервера
            transactionDao.clearTransactionsForProject(projectId)
            transactionDao.insertTransactions(entities)
        } catch (e: Exception) {
            throw Exception("Ошибка синхронизации транзакций: ${e.localizedMessage}")
        }
    }

    suspend fun getTransactionById(projectId: String, transactionId: String): TransactionEntity {
        try {
            val remoteTransaction = projectService.getTransactionById(projectId, transactionId).body()

            requireNotNull(remoteTransaction)

            return TransactionMapper.dtoToEntity(remoteTransaction)
        }  catch (e: Exception) {
            throw Exception(e.localizedMessage)
        }
    }
}
