package com.example.todowidget.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.todowidget.R
import com.example.todowidget.model.Project
import java.text.SimpleDateFormat
import java.util.*

class ProjectAdapter(
    private val context: Context,
    private var projects: List<Project> = emptyList()
) : RecyclerView.Adapter<ProjectAdapter.ProjectViewHolder>() {

    private val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProjectViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_project, parent, false)
        return ProjectViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProjectViewHolder, position: Int) {
        val project = projects[position]
        holder.bind(project)
    }

    override fun getItemCount(): Int = projects.size

    fun updateProjects(newProjects: List<Project>) {
        projects = newProjects
        notifyDataSetChanged()
    }

    inner class ProjectViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val projectName: TextView = itemView.findViewById(R.id.project_name)
        private val dueDate: TextView = itemView.findViewById(R.id.due_date)
        private val taskCount: TextView = itemView.findViewById(R.id.task_count)

        fun bind(project: Project) {
            projectName.text = project.name
            dueDate.text = context.getString(R.string.due, dateFormat.format(project.dueDate))
            taskCount.text = context.resources.getQuantityString(
                R.plurals.task_count, 
                project.taskCount, 
                project.taskCount
            )
            
            // Set priority indicator
            val priorityColor = when (project.priority) {
                Project.Priority.HIGH -> R.color.priority_high
                Project.Priority.MEDIUM -> R.color.priority_medium
                Project.Priority.LOW -> R.color.priority_low
            }
            projectName.setCompoundDrawablesRelativeWithIntrinsicBounds(
                priorityColor, 0, 0, 0
            )
        }
    }
}
