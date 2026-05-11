package com.prateekj.snooper.networksnooper.activity

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.prateekj.snooper.AndroidSnooper.Companion.ACTION_END_SNOOPER_FLOW
import com.prateekj.snooper.infra.AppPermissionChecker

abstract class SnooperBaseActivity :
    AppCompatActivity() {

    private lateinit var receiver:
            BroadcastReceiver

    protected lateinit var appPermissionChecker:
            AppPermissionChecker

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)

        receiver =
            createFinishActivityReceiver(
                this
            )

        appPermissionChecker =
            AppPermissionChecker(this)
    }

    override fun onResume() {

        super.onResume()

        LocalBroadcastManager
            .getInstance(this)
            .registerReceiver(
                receiver,
                IntentFilter(
                    ACTION_END_SNOOPER_FLOW
                )
            )
    }

    override fun onPause() {

        safelyUnregisterReceiver()

        super.onPause()
    }

    override fun onDestroy() {

        safelyUnregisterReceiver()

        super.onDestroy()
    }

    override fun onOptionsItemSelected(
        item: MenuItem
    ): Boolean {

        return when (item.itemId) {

            android.R.id.home -> {

                onBackPressedDispatcher
                    .onBackPressed()

                true
            }

            else -> {

                super.onOptionsItemSelected(
                    item
                )
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {

        super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        )

        appPermissionChecker
            .handlePermissionResult(
                requestCode,
                permissions,
                grantResults
            )
    }

    private fun safelyUnregisterReceiver() {

        try {

            LocalBroadcastManager
                .getInstance(this)
                .unregisterReceiver(receiver)

        } catch (_: Exception) {
        }
    }

    private fun createFinishActivityReceiver(
        activity: Activity
    ): BroadcastReceiver {

        return object : BroadcastReceiver() {

            override fun onReceive(
                context: Context?,
                intent: Intent?
            ) {

                if (
                    activity.isFinishing ||
                    (
                        Build.VERSION.SDK_INT >=
                        Build.VERSION_CODES.JELLY_BEAN_MR1 &&
                        activity.isDestroyed
                    )
                ) {

                    return
                }

                ActivityCompat.finishAffinity(
                    activity
                )
            }
        }
    }
}
