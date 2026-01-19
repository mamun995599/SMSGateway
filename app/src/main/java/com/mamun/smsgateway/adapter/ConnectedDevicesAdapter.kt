package com.mamun.smsgateway.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mamun.smsgateway.databinding.ItemConnectedDeviceBinding
import com.mamun.smsgateway.model.ConnectedDevice
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ConnectedDevicesAdapter : ListAdapter<ConnectedDevice, ConnectedDevicesAdapter.DeviceViewHolder>(DeviceDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val binding = ItemConnectedDeviceBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return DeviceViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DeviceViewHolder(private val binding: ItemConnectedDeviceBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

        fun bind(device: ConnectedDevice) {
            binding.tvDeviceIp.text = device.ipAddress
            binding.tvConnectedTime.text = "Connected at ${dateFormat.format(Date(device.connectedAt))}"
        }
    }

    class DeviceDiffCallback : DiffUtil.ItemCallback<ConnectedDevice>() {
        override fun areItemsTheSame(oldItem: ConnectedDevice, newItem: ConnectedDevice): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ConnectedDevice, newItem: ConnectedDevice): Boolean {
            return oldItem == newItem
        }
    }
}