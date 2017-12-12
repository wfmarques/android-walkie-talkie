package com.wesleymarques.walkietalkie.util

import android.app.Activity
import android.app.ProgressDialog
import android.content.DialogInterface
import android.util.Log
import android.view.Window

/**
 * Created by wesley on 06/12/17.
 */
private var mAlertDialog: android.support.v7.app.AlertDialog? = null
private var mProgressDialog: ProgressDialog? = null


fun showAlert(activity: Activity, title: String?, message: String?, okMessage: String?,  okCallback: () -> Unit = {}, cancelMessage: String? = null,  cancelCallback: () -> Unit = {}) {

    activity.runOnUiThread(object : Runnable {
        override fun run() {
            if (mAlertDialog != null) {
                mAlertDialog!!.hide()
            }

            mAlertDialog = android.support.v7.app.AlertDialog.Builder(activity).create()
            //
            if (title == null) {
                mAlertDialog!!.requestWindowFeature(Window.FEATURE_NO_TITLE)
            } else {
                mAlertDialog!!.setTitle(title)
            }

            mAlertDialog!!.setMessage(message)

            if (cancelMessage != null) {
                mAlertDialog!!.setButton(android.support.v7.app.AlertDialog.BUTTON_NEGATIVE, cancelMessage
                ) { dialog, which ->
                    cancelCallback()
                    dialog.dismiss()
                    mAlertDialog = null
                }
            }

            mAlertDialog!!.setButton(android.support.v7.app.AlertDialog.BUTTON_POSITIVE, okMessage,
                    object : DialogInterface.OnClickListener {
                        override fun onClick(dialog: DialogInterface, which: Int) {
                            if (okCallback != null) {
                                okCallback()
                            }
                            dialog.dismiss()
                            mAlertDialog = null
                        }
                    })

            mAlertDialog!!.show()
        }
    })

}

fun showProgress(activity: Activity, text: String?, cancelable:Boolean = false, cancelableHandler: () -> Unit = {}) {
    activity.runOnUiThread(object : Runnable {
        override fun run() {
            if (mProgressDialog != null) {
                hideProgress()
                mProgressDialog = null
            }

            if (mProgressDialog == null) {
                mProgressDialog = ProgressDialog(activity)
                mProgressDialog!!.setCancelable(cancelable)
                mProgressDialog!!.setCanceledOnTouchOutside(cancelable)
                mProgressDialog!!.setOnCancelListener {
                    cancelableHandler()
                }
            }

            mProgressDialog!!.setMessage(text)
            mProgressDialog!!.show()
        }
    })

}

fun hideProgress() {
    if (mProgressDialog != null) {
        mProgressDialog!!.dismiss()
    }
}