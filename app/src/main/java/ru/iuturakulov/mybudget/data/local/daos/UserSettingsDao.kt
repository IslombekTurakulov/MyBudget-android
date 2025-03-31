package ru.iuturakulov.mybudget.data.local.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ru.iuturakulov.mybudget.data.local.entities.UserSettingsEntity

@Dao
interface UserSettingsDao {

    @Query("SELECT * FROM user_settings WHERE email = :email")
    suspend fun getUserSettings(email: String): UserSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserSettings(settings: UserSettingsEntity)

    @Query("DELETE FROM user_settings WHERE email = :email")
    suspend fun deleteUserSettings(email: String)
}