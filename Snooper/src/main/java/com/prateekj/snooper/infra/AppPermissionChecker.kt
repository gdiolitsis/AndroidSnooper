package com.prateekj.snooper.infra

import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class AppPermissionChecker(
    private val activity: Activity
) {

    private var permissionRequestCode =
        0

    private var callBack:
            PermissionRequestCallBack? = null

    fun handlePermission(
        permission: String,
        permissionRequestCode: Int,
        callBack: PermissionRequestCallBack
    ) {

        this.permissionRequestCode =
            permissionRequestCode

        this.callBack =
            callBack

        if (
            Build.VERSION.SDK_INT <
            Build.VERSION_CODES.M
        ) {

            callBack.permissionGranted()

            return
        }

        val permissionGranted =
            ContextCompat.checkSelfPermission(
                activity,
                permission
            ) == PackageManager.PERMISSION_GRANTED

        if (permissionGranted) {

            callBack.permissionGranted()

            return
        }

        ActivityCompat.requestPermissions(
            activity,
            arrayOf(permission),
            permissionRequestCode
        )
    }

    fun handlePermissionResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {

        if (
            requestCode != permissionRequestCode
        ) {

            return
        }

        val granted =
            grantResults.isNotEmpty() &&
                    grantResults[0] ==
                    PackageManager.PERMISSION_GRANTED

        if (granted) {

            callBack?.permissionGranted()

        } else {

            callBack?.permissionDenied()
        }
    }

    interface PermissionRequestCallBack {

        fun permissionGranted()

        fun permissionDenied()
    }
}
