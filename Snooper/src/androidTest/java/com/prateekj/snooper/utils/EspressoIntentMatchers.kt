package com.prateekj.snooper.utils

import android.content.Intent
import android.net.Uri
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
                    item.get(
                        EXTRA_INTENT
                    ) as? Intent
                        ?: return false

                val uri =
                    intent.getParcelableExtra<Uri>(
                        EXTRA_STREAM
                    ) ?: return false

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
