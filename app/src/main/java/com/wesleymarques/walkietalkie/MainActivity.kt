package com.wesleymarques.walkietalkie

import android.content.res.Resources
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.wesleymarques.walkietalkie.fragments.FriendsFragment
import com.wesleymarques.walkietalkie.fragments.TalkFragment
import com.wesleymarques.walkietalkie.managers.NodeManager
import com.wesleymarques.walkietalkie.managers.StreamPlayerManager
import com.wesleymarques.walkietalkie.util.ResourceManager
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), Navigator.Host {

    private var lastFragment:Fragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Navigator.host = this //injecting the host
        ResourceManager.resources = this.resources

        setContentView(R.layout.activity_main)
        btSearchFriends.setOnClickListener { view ->
            if (lastFragment is FriendsFragment) {
                loadFragment(TalkFragment())
            } else {
                loadFragment(FriendsFragment())
            }
        }

        loadFragment(TalkFragment() as Fragment)
    }

    override fun onStart() {
        super.onStart()
        StreamPlayerManager.startServer(this)
        NodeManager.broadcast()
        NodeManager.listen()
    }

    override fun onStop() {
        super.onStop()
        NodeManager.stopAll()
        StreamPlayerManager.stopServer()
    }

    override fun loadFragment(fragment:Fragment) {
        lastFragment = fragment
        val t = supportFragmentManager.beginTransaction()
        t.replace(R.id.fragmentContainer, fragment, "MAIN")
        t.commit()
        if (fragment is FriendsFragment) {
            btSearchFriends.setImageResource(android.R.drawable.ic_menu_revert)
        } else {
            btSearchFriends.setImageResource(android.R.drawable.ic_menu_search)
        }
    }

    override fun onBackPressed() {
        Log.d("Debug", "On Back pressed")
        if (lastFragment is FriendsFragment) {
            loadFragment(TalkFragment())
        } else {
            super.onBackPressed()
        }
    }

    override fun getResources(): Resources {
        return super.getResources()
    }
}
