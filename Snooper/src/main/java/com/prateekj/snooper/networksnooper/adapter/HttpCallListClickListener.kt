package com.prateekj.snooper.networksnooper.adapter

import com.prateekj.snooper.networksnooper.model.HttpCallRecord

fun interface HttpCallListClickListener {

    fun onClick(
        httpCall: HttpCallRecord
    )
}
