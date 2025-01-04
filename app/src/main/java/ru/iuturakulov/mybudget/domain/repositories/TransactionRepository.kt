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
    fun getTransactionsForProject(projectId: String): Flow<List<TransactionEntity>> {
        return transactionDao.getTransactionsForProject(projectId)
    }

    // Добавление транзакции
    suspend fun addTransaction(projectId: String, transaction: TransactionEntity) {
        transactionDao.insertTransaction(transaction) // Сохраняем локально
        try {
            val dto = TransactionMapper.entityToDto(transaction)
            val response = projectService.addTransaction(projectId, dto)
            val updatedTransaction = TransactionMapper.dtoToEntity(response)
            transactionDao.updateTransaction(updatedTransaction) // Обновляем локальный кэш
        } catch (e: Exception) {
            transaction.id?.let { tr -> transactionDao.deleteTransaction(tr) }
            // Откат изменений
            throw Exception("Ошибка добавления транзакции: ${e.localizedMessage}")
        }
    }

    // Обновление транзакции
    suspend fun updateTransaction(projectId: String, transaction: TransactionEntity) {
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
    suspend fun deleteTransaction(projectId: String, transactionId: String) {
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
    suspend fun syncTransactions(projectId: String) {
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
