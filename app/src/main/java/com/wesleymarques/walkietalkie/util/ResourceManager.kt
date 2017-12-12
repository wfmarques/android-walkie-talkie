package com.wesleymarques.walkietalkie.util

import android.content.res.Resources

/**
 * Created by wesley on 12/12/17.
 */
object ResourceManager {
    var resources:Resources? = null

    fun getString(id: Int) : String? {
        return resources?.getString(id)
    }

    fun getString(id: Int, varargs: Any) : String? {
        return resources?.getString(id, varargs)
    }
}