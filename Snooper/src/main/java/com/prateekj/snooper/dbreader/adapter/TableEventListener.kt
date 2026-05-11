package com.prateekj.snooper.dbreader.adapter

fun interface TableEventListener {

    fun onTableClick(
        table: String
    )
}
