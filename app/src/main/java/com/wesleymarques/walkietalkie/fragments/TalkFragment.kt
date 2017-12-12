package com.wesleymarques.walkietalkie.fragments

import android.Manifest
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.app.Fragment
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import com.wesleymarques.walkietalkie.R
import com.wesleymarques.walkietalkie.managers.NodeManager
import com.wesleymarques.walkietalkie.managers.RecorderManager
import com.wesleymarques.walkietalkie.util.ResourceManager
import com.wesleymarques.walkietalkie.util.showAlert
import kotlinx.android.synthetic.main.fragment_talk.*

/**
 * Created by wesley on 05/12/17.
 */
class TalkFragment : Fragment() {


    private val PERMISSIONS = arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE)
    private val FRAGMENT_DIALOG = "dialog"
    private val REQUEST_PERMISSIONS = 1


    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return return inflater?.inflate(R.layout.fragment_talk, container, false)
    }

    override fun onStart() {
        super.onStart()
        requestPermissions()

        etName.addTextChangedListener(object: TextWatcher {
            override fun afterTextChanged(newText: Editable?) {
                if (!newText.isNullOrEmpty()) {
                    NodeManager.myName = newText.toString()
                }
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }
        })

        NodeManager.onFriendLinked = { friend ->
            setupUI()
        }

        NodeManager.onFriendUnLinked = {
            setupUI()
        }

        NodeManager.onFriendSpeaking = {
            activity?.let {
                it.runOnUiThread {
                    tvMessage.text = " Speaking"
                    btSpeak.isEnabled = false
                }
            }
        }

        setupUI()

        if (NodeManager.myName != null) {
            etName.setText(NodeManager.myName)
        }

    }

    private fun setupUI() {
        activity?.let {
            it.runOnUiThread {
                try {

                    if (NodeManager.selectedFriend == null || NodeManager.status == NodeManager.Status.IDLE) {
                        tvMessage.text = ResourceManager.getString(R.string.find_a_friend)
                        btSpeak.isEnabled = false
                        btSpeak.text = ResourceManager.getString(R.string.disabled)
                    } else {
                        tvMessage.text = ResourceManager.getString(R.string.connected_to,  NodeManager.selectedFriend?.name as Any)
                        btSpeak.isEnabled = true
                        btSpeak.text = ResourceManager.getString(R.string.hold_and_speak)
                    }


                    btSpeak.setOnTouchListener(View.OnTouchListener { view, motionEvent ->
                        when (motionEvent.action) {
                            MotionEvent.ACTION_DOWN -> {

                                btSpeak.isSelected = true
                                NodeManager.status = NodeManager.Status.SPEAKING
                                RecorderManager.startRecordAndStream(
                                        friendIP = NodeManager.selectedFriend?.host,
                                        success = {

                                        },
                                        error = { message ->
                                            showAlert(activity, "Error", message, "OK")
                                        })
                            }

                            MotionEvent.ACTION_UP -> {
                                NodeManager.status = NodeManager.Status.LINKED
                                btSpeak.isSelected = false
                                RecorderManager.stopRecording()
                            }
                        }
                        return@OnTouchListener false
                    })
                } catch (e: Exception) {
                    e.printStackTrace()
                }

            }
        }

    }

    private fun shouldShowRequestPermissionRationale(permissions: Array<String>): Boolean {
        for (permission in permissions) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
                return true
            }
        }
        return false
    }

    fun requestPermissions() {

        if (shouldShowRequestPermissionRationale(PERMISSIONS)) {
            showAlert(activity,
                    title = "Permission",
                    message = "You allow record Audio?",
                    okMessage = "OK",
                    okCallback = {
                        ActivityCompat.requestPermissions(activity, PERMISSIONS,
                                REQUEST_PERMISSIONS)
                    },
                    cancelMessage = "Cancel",
                    cancelCallback = {

                    })
        } else {
            ActivityCompat.requestPermissions(activity, PERMISSIONS, REQUEST_PERMISSIONS)
        }
    }
}