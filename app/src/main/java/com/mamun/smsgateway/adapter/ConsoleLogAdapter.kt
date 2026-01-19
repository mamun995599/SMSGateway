package com.mamun.smsgateway.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mamun.smsgateway.databinding.ItemLogEntryBinding
import com.mamun.smsgateway.model.LogEntry
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ConsoleLogAdapter : ListAdapter<LogEntry, ConsoleLogAdapter.LogViewHolder>(LogDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val binding = ItemLogEntryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return LogViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class LogViewHolder(private val binding: ItemLogEntryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

        fun bind(logEntry: LogEntry) {
            binding.tvTimestamp.text = "[${dateFormat.format(Date(logEntry.timestamp))}]"
            binding.tvMethod.text = logEntry.method
            binding.tvLogMessage.text = "${logEntry.path} from ${logEntry.clientIp} - ${logEntry.statusCode}"

            // Color code based on method
            val methodColor = when (logEntry.method) {
                "GET" -> Color.parseColor("#61AFEF")
                "POST" -> Color.parseColor("#98C379")
                "PUT" -> Color.parseColor("#E5C07B")
                "DELETE" -> Color.parseColor("#E06C75")
                else -> Color.parseColor("#ABB2BF")
            }
            binding.tvMethod.setTextColor(methodColor)

            // Color code based on status
            val statusColor = when (logEntry.type) {
                LogEntry.LogType.SUCCESS -> Color.parseColor("#98C379")
                LogEntry.LogType.WARNING -> Color.parseColor("#E5C07B")
                LogEntry.LogType.ERROR -> Color.parseColor("#E06C75")
                LogEntry.LogType.INFO -> Color.parseColor("#00FF00")
            }
            binding.tvLogMessage.setTextColor(statusColor)
        }
    }

    class LogDiffCallback : DiffUtil.ItemCallback<LogEntry>() {
        override fun areItemsTheSame(oldItem: LogEntry, newItem: LogEntry): Boolean {
            return oldItem.timestamp == newItem.timestamp
        }

        override fun areContentsTheSame(oldItem: LogEntry, newItem: LogEntry): Boolean {
            return oldItem == newItem
        }
    }
}
