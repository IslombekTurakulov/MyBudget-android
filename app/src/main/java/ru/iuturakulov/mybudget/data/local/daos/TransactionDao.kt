package ru.iuturakulov.mybudget.data.local.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ru.iuturakulov.mybudget.data.local.entities.TransactionEntity

@Dao
interface TransactionDao {

    @Query("SELECT * FROM transactions WHERE projectId = :projectId")
    fun getTransactionsForProject(projectId: Int): List<TransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :transactionId")
    suspend fun deleteTransaction(transactionId: Int)
}