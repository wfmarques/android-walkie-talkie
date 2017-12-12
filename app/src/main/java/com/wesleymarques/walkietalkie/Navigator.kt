package com.wesleymarques.walkietalkie

import android.support.v4.app.Fragment
import android.util.Log

/**
 * Created by wesley on 09/12/17.
 * Abstract object for the navigation responsible, avoid directly references for Activities or any
 * kind of Class implementing the Host interface
 */
object Navigator {
    var host:Host? = null

    fun loadFragment(fragment:Fragment) {
        if (host != null) {
            host?.loadFragment(fragment)
        } else {
            Log.d("Navigator", "HOST NOT INJECTED")
        }
    }

    interface Host {
        fun loadFragment(fragment: Fragment)
    }
}