package com.wesleymarques.walkietalkie.fragments

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.wesleymarques.walkietalkie.Navigator
import com.wesleymarques.walkietalkie.R
import com.wesleymarques.walkietalkie.adapters.FriendsAdapter
import com.wesleymarques.walkietalkie.managers.NodeManager
import com.wesleymarques.walkietalkie.util.ResourceManager
import com.wesleymarques.walkietalkie.util.hideProgress
import com.wesleymarques.walkietalkie.util.showProgress
import kotlinx.android.synthetic.main.fragment_friends.*

/**
 * Created by wesley on 05/12/17.
 */

class FriendsFragment : Fragment() {

    private val adapter = FriendsAdapter({ friend ->
        println("Friend ${friend?.name}")
        if (friend == null) {
            NodeManager.status = NodeManager.Status.UNLINK
         } else {
            NodeManager.status = NodeManager.Status.LINKED
            Navigator.loadFragment(TalkFragment() as Fragment)
        }
        NodeManager.selectedFriend = friend
    })

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return return inflater?.inflate(R.layout.fragment_friends, container, false)
    }

    override fun onStart() {
        super.onStart()
        rvFriends.adapter = adapter
        rvFriends.layoutManager = LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
        showProgress(activity,
                ResourceManager.getString(R.string.looking_friends),
                true, {
            Navigator.loadFragment(TalkFragment())
        })

        NodeManager.onFriendFound = { friend ->
            activity?.let {
                it.runOnUiThread({
                    adapter.selectedFriend = NodeManager.selectedFriend
                    adapter.appendFriend(friend)
                    hideProgress()
                })
            }

        }
    }

    override fun onStop() {
        super.onStop()
        hideProgress()
    }


}