package com.prateekj.snooper.dbreader.model

data class Database(

    var name: String = "",

    var path: String = "",

    var version: Int = 0,

    var tables: List<String> = emptyList()
)
