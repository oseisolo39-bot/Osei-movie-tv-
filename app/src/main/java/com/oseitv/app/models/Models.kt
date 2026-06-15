package com.oseitv.app.models

import java.util.Calendar

data class Program(
    val title: String,
    val startTime: Calendar,
    val endTime: Calendar,
    val description: String
)

data class Channel(
    val id: String,
    val name: String,
    val url: String,
    val category: String,
    val logo: String? = null
)
