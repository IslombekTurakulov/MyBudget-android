package ru.iuturakulov.mybudget.data.local.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import ru.iuturakulov.mybudget.data.local.entities.TransactionEntity

@Dao
interface TransactionDao {

    // Получить транзакции для проекта
    @Query("SELECT * FROM transactions WHERE projectId = :projectId")
    fun getTransactionsForProject(projectId: Int): Flow<List<TransactionEntity>>

    // Вставить одну транзакцию
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    // Вставить несколько транзакций
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<TransactionEntity>)

    // Обновить транзакцию
    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    // Удалить одну транзакцию
    @Query("DELETE FROM transactions WHERE id = :transactionId")
    suspend fun deleteTransaction(transactionId: Int)

    // Очистить транзакции для проекта
    @Query("DELETE FROM transactions WHERE projectId = :projectId")
    suspend fun clearTransactionsForProject(projectId: Int)
}
