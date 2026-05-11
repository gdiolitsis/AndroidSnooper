package com.prateekj.snooper.dbreader.model

data class Table(

    var name: String = "",

    var columns: List<String> = emptyList(),

    var rows: List<Row> = emptyList()
)
