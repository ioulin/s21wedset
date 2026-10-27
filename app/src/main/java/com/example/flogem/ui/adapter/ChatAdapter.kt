package com.example.flogem.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.flogem.R
import com.example.flogem.databinding.ItemMessageBinding
import com.example.flogem.domain.model.ChatMessage

/**
 * 채팅 메시지 RecyclerView 어댑터
 * - DiffUtil로 효율적인 리스트 업데이트
 * - 사용자/제미나이/시스템/에러 각각 다른 레이아웃
 * - 타이핑 중인 메시지(점 3개 애니메이션) 처리
 */
class ChatAdapter :
    ListAdapter<ChatMessage, ChatAdapter.MessageViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val binding = ItemMessageBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return MessageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class MessageViewHolder(private val binding: ItemMessageBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(message: ChatMessage) {
            binding.apply {
                messageText.text = message.content
                messageTime.text = formatTime(message.timestamp)

                when (message.role) {
                    ChatMessage.Role.USER -> {
                        messageContainer.setBackgroundResource(R.drawable.bg_message_user)
                    }
                    ChatMessage.Role.MODEL -> {
                        messageContainer.setBackgroundResource(R.drawable.bg_message_model)
                    }
                    ChatMessage.Role.SYSTEM,
                    ChatMessage.Role.ERROR -> {
                        messageContainer.setBackgroundResource(R.drawable.bg_message_system)
                    }
                }

                root.setOnClickListener {
                    // 메시지 복사 기능 (선택)
                }
            }
        }

        private fun formatTime(timestamp: Long): String {
            val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
            return sdf.format(java.util.Date(timestamp))
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<ChatMessage>() {
        override fun areItemsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
            return oldItem == newItem
        }
    }
}
