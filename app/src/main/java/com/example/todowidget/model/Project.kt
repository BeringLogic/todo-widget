package com.example.todowidget.model

import java.util.Date

data class Project(
    val id: Long = 0,
    val name: String,
    val dueDate: Date,
    val taskCount: Int,
    val priority: Priority = Priority.MEDIUM
)

enum class Priority {
    HIGH,
    MEDIUM,
    LOW
}

// Sample data for preview/testing
object SampleData {
    fun getThisWeeksProjects(): List<Project> {
        return listOf(
            Project(
                id = 1,
                name = "Complete Project Proposal",
                dueDate = Date(System.currentTimeMillis() + 2 * 24 * 60 * 60 * 1000), // 2 days from now
                taskCount = 5,
                priority = Priority.HIGH
            ),
            Project(
                id = 2,
                name = "Team Meeting Prep",
                dueDate = Date(System.currentTimeMillis() + 5 * 24 * 60 * 60 * 1000), // 5 days from now
                taskCount = 3,
                priority = Priority.MEDIUM
            )
        )
    }
}
