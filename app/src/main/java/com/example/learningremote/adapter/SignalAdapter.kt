package com.example.learningremote.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.learningremote.databinding.ItemSignalBinding
import com.example.learningremote.model.RemoteSignal

class SignalAdapter(
    private var signals: MutableList<RemoteSignal>,
    private val onTransmit: (RemoteSignal) -> Unit,
    private val onDelete: (RemoteSignal) -> Unit
) : RecyclerView.Adapter<SignalAdapter.SignalViewHolder>() {

    class SignalViewHolder(val binding: ItemSignalBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SignalViewHolder {
        val binding = ItemSignalBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SignalViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SignalViewHolder, position: Int) {
        val signal = signals[position]

        holder.binding.textViewRemoteId.text = signal.remoteId
        holder.binding.textViewButtonName.text = signal.buttonName

        val typeIcon = if (signal.type == "ir") "📺" else "📡"
        holder.binding.textViewSignalInfo.text = "$typeIcon ${signal.type.uppercase()} • ${signal.dataLen} bytes"

        holder.binding.buttonTransmit.setOnClickListener {
            onTransmit(signal)
        }

        holder.binding.buttonDelete.setOnClickListener {
            onDelete(signal)
        }
    }

    override fun getItemCount(): Int = signals.size

    fun updateData(newSignals: MutableList<RemoteSignal>) {
        signals = newSignals
        notifyDataSetChanged()
    }
}