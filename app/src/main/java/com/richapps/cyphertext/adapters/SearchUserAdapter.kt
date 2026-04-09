package com.richapps.cyphertext.adapters

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.richapps.cyphertext.R
import com.richapps.cyphertext.activities.ChatActivity
import com.richapps.cyphertext.models.Users

class SearchUserAdapter(
    private var userList: List<Users> = emptyList(),
    //private val currentUserPhone: String = ""

) : RecyclerView.Adapter<SearchUserAdapter.UserViewHolder>() {

    inner class UserViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val userName = itemView.findViewById<TextView>(R.id.user_name)
        val userNumber = itemView.findViewById<TextView>(R.id.user_number)
        //val userId = itemView.findViewById<TextView>(R.id.user_id)


        fun bind(user: Users) {
            userName.text = user.userName
            userNumber.text = user.phoneNumber
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): UserViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.search_user_row, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: UserViewHolder,
        position: Int,
    ) {
        holder.bind(userList[position])

        holder.itemView.setOnClickListener {
            // TODO("Navigate to chat screen")
            val intent = Intent(holder.itemView.context, ChatActivity::class.java)
            intent.putExtra("userName", userList[position].userName)
            intent.putExtra("userId", userList[position].userId)
            holder.itemView.context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = userList.size

    fun updateData(newList: List<Users>) {
        this.userList = newList
        notifyDataSetChanged()
    }

    //More modern approach using DiffUtil

//    fun updateData(newList: List<Users>) {
//        val diffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
//            override fun getOldListSize() = userList.size
//            override fun getNewListSize() = newList.size
//
//            override fun areItemsTheSame(oldPos: Int, newPos: Int): Boolean {
//                return userList[oldPos].phoneNumber == newList[newPos].phoneNumber
//            }
//
//            override fun areContentsTheSame(oldPos: Int, newPos: Int): Boolean {
//                return userList[oldPos] == newList[newPos]
//            }
//        })
//        userList = newList
//        diffResult.dispatchUpdatesTo(this)
//    }
}