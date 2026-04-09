package com.richapps.cyphertext.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.richapps.cyphertext.R
import com.richapps.cyphertext.Utils
import com.richapps.cyphertext.models.MessageModel

class ChatAdapter (
    internal var messageList: List<MessageModel> = emptyList()
    ) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {


    inner class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val leftHolder = itemView.findViewById<CardView>(R.id.leftHolder)
        val rightHolder = itemView.findViewById<CardView>(R.id.rightHolder)
        val leftMessage = itemView.findViewById<TextView>(R.id.leftChat)
        val rightMessage = itemView.findViewById<TextView>(R.id.rightChat)


    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.message_row, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val model = messageList[position]
        if(model.senderId == Utils.getCurrentUserDbId()){ // sender is current user
            holder.leftHolder.visibility = View.GONE
            holder.rightHolder.visibility = View.VISIBLE
            holder.rightMessage.text = model.message
        }
        else { //sender is other user
            holder.leftHolder.visibility = View.VISIBLE
            holder.rightHolder.visibility = View.GONE
            holder.leftMessage.text = model.message

        }
    }

    override fun getItemCount(): Int = messageList.size

    fun updateMessages(newMessages: List<MessageModel>) {
        messageList = newMessages
        notifyDataSetChanged()
    }

}