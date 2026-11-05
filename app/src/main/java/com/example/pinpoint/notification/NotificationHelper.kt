package com.example.pinpoint.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import androidx.core.graphics.drawable.IconCompat
import com.example.MainActivity
import com.example.R
import com.example.pinpoint.model.ChatMessage
import com.example.pinpoint.model.MessageType

object NotificationHelper {

    const val CHANNEL_ID = "pinpoint_messages_whatsapp"
    const val CHANNEL_NAME = "PinPoint Messages"
    const val CHANNEL_DESC = "High priority real-time messages and encrypted notifications"

    const val KEY_TEXT_REPLY = "key_text_reply"
    const val EXTRA_CONVERSATION_ID = "extra_conversation_id"
    const val ACTION_REPLY = "com.example.pinpoint.ACTION_REPLY"
    const val ACTION_MARK_READ = "com.example.pinpoint.ACTION_MARK_READ"

    private val conversationHistory = mutableMapOf<String, MutableList<ChatMessage>>()

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESC
                enableLights(true)
                lightColor = 0xFF0091EA.toInt()
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 150, 250)
                setShowBadge(true)
            }

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    fun showWhatsAppStyleNotification(context: Context, message: ChatMessage) {
        createNotificationChannel(context)

        val convId = message.conversationId
        val history = conversationHistory.getOrPut(convId) { mutableListOf() }
        history.add(message)

        val isGroup = convId.startsWith("grp_")
        val notificationId = convId.hashCode()

        // User Person
        val userPerson = Person.Builder()
            .setName("Me")
            .setKey("user_me")
            .build()

        // MessagingStyle (WhatsApp / Telegram style)
        val messagingStyle = NotificationCompat.MessagingStyle(userPerson)
        if (isGroup) {
            messagingStyle.isGroupConversation = true
            messagingStyle.conversationTitle = if (convId == "grp_sales_team") "Sales & Engineering Team" else "SecOps Group"
        }

        // Add history messages
        history.takeLast(10).forEach { msg ->
            val senderPerson = Person.Builder()
                .setName(if (msg.isOutgoing) "Me" else msg.senderName)
                .setKey(msg.senderPin)
                .setIcon(createAvatarIcon(msg.senderName))
                .build()

            val text = when (msg.type) {
                MessageType.IMAGE -> "📷 Photo message (${msg.mediaName ?: "shared image"})"
                MessageType.FILE -> "📄 Document (${msg.mediaName ?: "attachment"})"
                MessageType.VOICE_NOTE -> "🎙️ Voice note (${msg.mediaSize ?: "0:14"})"
                MessageType.PING -> "💥 PING! High Priority Alert"
                else -> msg.content
            }

            messagingStyle.addMessage(
                NotificationCompat.MessagingStyle.Message(
                    text,
                    msg.timestamp,
                    senderPerson
                )
            )
        }

        // Tap Notification PendingIntent (Opens chat screen directly)
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_CONVERSATION_ID, convId)
        }
        val tapPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        // Direct Inline Reply Action (WhatsApp style)
        val remoteInput = RemoteInput.Builder(KEY_TEXT_REPLY)
            .setLabel("Reply to ${message.senderName}...")
            .build()

        val replyIntent = Intent(context, ReplyReceiver::class.java).apply {
            action = ACTION_REPLY
            putExtra(EXTRA_CONVERSATION_ID, convId)
        }
        val replyPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId,
            replyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        val replyAction = NotificationCompat.Action.Builder(
            R.drawable.ppm_logo,
            "Reply",
            replyPendingIntent
        )
            .addRemoteInput(remoteInput)
            .setAllowGeneratedReplies(true)
            .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_REPLY)
            .build()

        // Mark as Read Action
        val readIntent = Intent(context, ReplyReceiver::class.java).apply {
            action = ACTION_MARK_READ
            putExtra(EXTRA_CONVERSATION_ID, convId)
        }
        val readPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 10000,
            readIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        val readAction = NotificationCompat.Action.Builder(
            R.drawable.ppm_logo,
            "Mark as read",
            readPendingIntent
        )
            .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_MARK_AS_READ)
            .build()

        // Build Final High Priority Notification
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ppm_logo)
            .setStyle(messagingStyle)
            .setContentIntent(tapPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .addAction(replyAction)
            .addAction(readAction)

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (_: SecurityException) {
            // Permission missing on Android 13+
        }
    }

    fun clearConversationNotification(context: Context, conversationId: String) {
        conversationHistory.remove(conversationId)
        NotificationManagerCompat.from(context).cancel(conversationId.hashCode())
    }

    private fun createAvatarIcon(name: String): IconCompat {
        val size = 96
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val bgPaint = Paint().apply {
            color = 0xFF007399.toInt()
            isAntiAlias = true
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, bgPaint)

        val textPaint = Paint().apply {
            color = 0xFFFFFFFF.toInt()
            textSize = 42f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }

        val initial = name.take(1).uppercase()
        val yPos = (size / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f)
        canvas.drawText(initial, size / 2f, yPos, textPaint)

        return IconCompat.createWithBitmap(bitmap)
    }
}
