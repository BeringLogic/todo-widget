package com.example.todowidget.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = false)
data class Todo(
    val id: String,
    val title: String,
    val completed: Boolean,
    val dueDate: String? = null
)

class TodoJsonAdapter {
    companion object {
        fun create(): com.squareup.moshi.JsonAdapter<Todo> {
            return com.squareup.moshi.Moshi.Builder().build().adapter(Todo::class.java)
        }
    }
}
