package com.prateekj.snooper.utils

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import org.hamcrest.CustomTypeSafeMatcher
import org.hamcrest.Matcher
import android.content.Intent.EXTRA_INTENT
import android.content.Intent.EXTRA_STREAM
import android.content.Intent.EXTRA_SUBJECT

object EspressoIntentMatchers {

    fun forMailChooserIntent(
        action: String,
        mimeType: String,
        extraData: String,
        fileName: String
    ): Matcher<Bundle> {

        return object : CustomTypeSafeMatcher<Bundle>(
            "Custom matcher for matching mail chooser intent"
        ) {

            override fun matchesSafely(
                item: Bundle
            ): Boolean {

                val intent =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

                        item.getParcelable(
                            EXTRA_INTENT,
                            Intent::class.java
                        )

                    } else {

                        @Suppress("DEPRECATION")
                        item.getParcelable(
                            EXTRA_INTENT
                        )
                    } ?: return false

                val uri =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

                        intent.getParcelableExtra(
                            EXTRA_STREAM,
                            Uri::class.java
                        )

                    } else {

                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(
                            EXTRA_STREAM
                        )
                    } ?: return false

                val uriPath =
                    uri.path ?: return false

                val subject =
                    intent.getStringExtra(
                        EXTRA_SUBJECT
                    ) ?: return false

                return action == intent.action &&
                        mimeType == intent.type &&
                        subject.equals(
                            extraData,
                            ignoreCase = true
                        ) &&
                        uriPath.endsWith(fileName)
            }
        }
    }
}
