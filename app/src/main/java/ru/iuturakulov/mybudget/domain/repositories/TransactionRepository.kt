package ru.iuturakulov.mybudget.domain.repositories

import kotlinx.coroutines.flow.Flow
import ru.iuturakulov.mybudget.data.local.daos.TransactionDao
import ru.iuturakulov.mybudget.data.local.entities.TransactionEntity
import ru.iuturakulov.mybudget.data.mappers.TransactionMapper
import ru.iuturakulov.mybudget.data.remote.ProjectService
import javax.inject.Inject

class TransactionRepository @Inject constructor(
    private val transactionDao: TransactionDao,
    private val projectService: ProjectService
) {

    // Получение списка транзакций проекта
    fun getTransactionsForProject(projectId: Int): Flow<List<TransactionEntity>> {
        return transactionDao.getTransactionsForProject(projectId)
    }

    // Добавление транзакции
    suspend fun addTransaction(projectId: Int, transaction: TransactionEntity) {
        transactionDao.insertTransaction(transaction) // Сохраняем локально
        var updatedTransaction : TransactionEntity? = null
        try {
            val dto = TransactionMapper.entityToDto(transaction)
            val response = projectService.addTransaction(projectId, dto)
            updatedTransaction = TransactionMapper.dtoToEntity(response)
            transactionDao.updateTransaction(updatedTransaction) // Обновляем локальный кэш
        } catch (e: Exception) {
            updatedTransaction?.id?.let { tr ->
                transactionDao.deleteTransaction(tr)
            } // Откат изменений
            throw Exception("Ошибка добавления транзакции: ${e.localizedMessage}")
        }
    }

    // Обновление транзакции
    suspend fun updateTransaction(projectId: Int, transaction: TransactionEntity) {
        try {
            val dto = TransactionMapper.entityToDto(transaction)
            val response = projectService.updateTransaction(projectId, dto)
            val updatedTransaction = TransactionMapper.dtoToEntity(response)
            transactionDao.updateTransaction(updatedTransaction) // Обновляем локальный кэш
        } catch (e: Exception) {
            throw Exception("Ошибка добавления транзакции: ${e.localizedMessage}")
        }
    }

    // Удаление транзакции
    suspend fun deleteTransaction(projectId: Int, transactionId: Int) {
        // Локальное удаление
        transactionDao.deleteTransaction(transactionId)
        try {
            // Удаление на сервере
            projectService.deleteTransaction(projectId, transactionId)
        } catch (e: Exception) {
            throw Exception("Ошибка удаления транзакции: ${e.localizedMessage}")
        }
    }

    // Синхронизация транзакций с сервером
    suspend fun syncTransactions(projectId: Int) {
        try {
            val remoteTransactions = projectService.fetchTransactions(projectId)
            val entities = remoteTransactions.map { TransactionMapper.dtoToEntity(it) }

            // Очистка локального кэша и обновление из сервера
            transactionDao.clearTransactionsForProject(projectId)
            transactionDao.insertTransactions(entities)
        } catch (e: Exception) {
            throw Exception("Ошибка синхронизации транзакций: ${e.localizedMessage}")
        }
    }
}
