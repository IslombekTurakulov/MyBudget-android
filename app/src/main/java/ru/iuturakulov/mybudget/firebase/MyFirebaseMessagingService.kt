package ru.iuturakulov.mybudget.firebase

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import ru.iuturakulov.mybudget.MainActivity
import ru.iuturakulov.mybudget.R
import java.util.Locale
import javax.inject.Inject

/**
 * Сервис для приёма FCM-уведомлений и регистрации токена.
 * Hilt сам создаёт граф и подставляет DeviceTokenRegistrar.
 */
@AndroidEntryPoint
class MyFirebaseMessagingService : FirebaseMessagingService() {

    /** Класс, который отправляет FCM-токен на ваш Ktor-сервер */
    @Inject
    lateinit var registrar: DeviceTokenRegistratiom

    /**
     * Вызывается, когда FCM выдаёт новый регистрационный токен.
     * Нужно пересылать его на сервер, чтобы пуши шли именно на это устройство.
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        registrar.enqueue(
            token = token,
            languageCode = Locale.getDefault().language
        )
    }

    /**
     * Основной входной метод — сюда приходят все сообщения (notification + data).
     * Разбираем заголовок, тело и доп. данные, и формируем локальную нотификацию.
     */
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        // Если есть Notification payload — используем его
        val notif = message.notification
        if (notif != null) {
            showLocalNotification(
                title = notif.title ?: getString(R.string.app_name),
                body = notif.body ?: "",
                data = message.data
            )
            return
        }

        // 2) Иначе — data-only сообщения. Извлекаем title/body из data
        val data = message.data
        val title = data["title"] ?: getString(R.string.app_name)
        val body  = data["body"]  ?: ""
        showLocalNotification(title, body, data)
    }

    /**
     * Формирует и показывает локальное уведомление.
     *
     * @param title Заголовок
     * @param body  Текст
     * @param data  Словарь доп. полей (например, id транзакции, тип уведомления и т.д.)
     */
    private fun showLocalNotification(
        title: String,
        body: String,
        data: Map<String, String>
    ) {
        // Собираем Intent, на который нужно перейти при клике
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP

            // Если в data пришёл id транзакции, передаём его дальше
            data["txId"]?.let { putExtra("txId", it) }
            // Можно передать и тип уведомления:
            data["type"]?.let { putExtra("notificationType", it) }
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            /* requestCode */ System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Строим notification с помощью Support Library
        val notification = NotificationCompat.Builder(this, MyFirebaseMessagingService.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notifications_24)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        // Показываем уведомление
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }

        NotificationManagerCompat.from(this)
            .notify(/* id */ System.currentTimeMillis().toInt(), notification)
    }

    companion object {
        /** ID канала, который вы создаёте в Application.onCreate() */
        const val CHANNEL_ID = "mybudget_notifications"
    }
}
