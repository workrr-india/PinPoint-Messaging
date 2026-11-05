package com.example.pinpoint.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.RemoteInput
import com.example.pinpoint.data.PinPointRepository
import com.example.pinpoint.model.ChatMessage
import com.example.pinpoint.model.MessageType

class ReplyReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val conversationId = intent.getStringExtra(NotificationHelper.EXTRA_CONVERSATION_ID) ?: return

        when (action) {
            NotificationHelper.ACTION_REPLY -> {
                val results = RemoteInput.getResultsFromIntent(intent) ?: return
                val replyText = results.getCharSequence(NotificationHelper.KEY_TEXT_REPLY)?.toString() ?: return

                if (replyText.isNotBlank()) {
                    val repository = PinPointRepository.getInstance(context)
                    repository.sendMessage(conversationId, replyText)

                    // Re-query user profile to update notification thread with sent reply
                    val userProfile = repository.userProfile.value
                    val replyMessage = ChatMessage(
                        id = "msg_${System.currentTimeMillis()}",
                        conversationId = conversationId,
                        senderPin = userProfile.pin,
                        senderName = userProfile.displayName,
                        content = replyText,
                        timestamp = System.currentTimeMillis(),
                        type = MessageType.TEXT,
                        isOutgoing = true
                    )

                    NotificationHelper.showWhatsAppStyleNotification(context, replyMessage)
                }
            }

            NotificationHelper.ACTION_MARK_READ -> {
                NotificationHelper.clearConversationNotification(context, conversationId)
            }
        }
    }
}
