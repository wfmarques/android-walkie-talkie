package com.wesleymarques.walkietalkie.adapters

import android.graphics.Color
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.wesleymarques.walkietalkie.R
import com.wesleymarques.walkietalkie.managers.NodeManager
import com.wesleymarques.walkietalkie.model.Friend
import kotlinx.android.synthetic.main.view_friend_item.view.*

/**
 * Created by wesley on 05/12/17.
 */
class FriendsAdapter: RecyclerView.Adapter<FriendViewHolder> {

    private var delegate:(Friend?) -> Unit
    private val friends = mutableListOf<Friend>()
    private val friendsMap = mutableMapOf<String, Friend>()

    var selectedFriend:Friend? = null


    constructor(delegate:(Friend?) -> Unit) : super() {
        this.delegate = delegate
    }

    fun appendFriend(friend:Friend?) {
        if (!friendsMap.containsKey(friend?.host)) {
            friendsMap[friend!!.host] = friend!!
            friends.add(friend)
        } else {
            friendsMap[friend!!.host]?.host = friend!!.host
            friendsMap[friend!!.host]?.name = friend!!.name
        }
        notifyDataSetChanged()
    }

    override fun getItemCount() = friends.size

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): FriendViewHolder {
        val view:ViewGroup = LayoutInflater
                .from(parent!!.context)
                .inflate(R.layout.view_friend_item, parent, false) as ViewGroup
        return FriendViewHolder(view)
    }

    override fun onBindViewHolder(holder: FriendViewHolder?, position: Int) {
          holder?.host?.text = friends[position].host
          holder?.name?.text =  friends[position].name
          holder?.itemView?.setOnClickListener {
              if ( selectedFriend != null && selectedFriend?.host == friends[position].host ) {
                  selectedFriend = null
              } else {
                  selectedFriend = friends[position]
              }

              delegate(selectedFriend)
              notifyDataSetChanged()
          }

          selectedFriend.let {
            if (it?.host == friends[position].host) {
                holder?.itemView?.setBackgroundColor(Color.CYAN)
            } else {
                holder?.itemView?.setBackgroundColor(Color.WHITE)
            }
          }
    }
}

class FriendViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
    var host:TextView = itemView.tvHost
    val name:TextView = itemView.tvName

}