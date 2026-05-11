package com.prateekj.snooper.dbreader.model

data class Database(

    var name: String? = null,

    var path: String? = null,

    var version: Int = 0,

    var tables: List<String>? = null
)
