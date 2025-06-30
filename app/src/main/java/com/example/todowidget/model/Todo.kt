package com.example.todowidget.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Todo(
    val id: String,
    val title: String,
    val completed: Boolean,
    @Json(name = "due_date") val dueDate: String? = null
)
