package ru.iuturakulov.mybudget.data.local.entities

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import ru.iuturakulov.mybudget.data.remote.dto.ParticipantRole

@Parcelize
@Entity(tableName = "participants")
data class ParticipantEntity(
    @PrimaryKey val id: String, // ID участника, который приходит с сервера
    val projectId: String,      // Связь с проектом
    val userId: String,      // Уникальный идентификатор пользователя
    val name: String,        // Имя участника
    val email: String,       // Email участника
    val role: ParticipantRole         // Роль участника
) : Parcelable
