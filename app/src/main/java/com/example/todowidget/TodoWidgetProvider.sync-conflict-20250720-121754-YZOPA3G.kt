package com.example.todowidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import com.example.todowidget.adapter.ProjectAdapter
import com.example.todowidget.model.Project
import com.example.todowidget.model.SampleData
import java.text.SimpleDateFormat
import java.util.*

/**
 * Widget that displays this week's projects from the Todo app.
 */
class TodoWidgetProvider : AppWidgetProvider() {
    
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // Update all widgets
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context?,
        appWidgetManager: AppWidgetManager?,
        appWidgetId: Int,
        newOptions: Bundle?
    ) {
        context?.let { ctx ->
            appWidgetManager?.let { manager ->
                updateAppWidget(ctx, manager, appWidgetId)
            }
        }
    }

    override fun onEnabled(context: Context) {
        // Initialize any widget-wide settings when the first widget is created
    }

    override fun onDisabled(context: Context) {
        // Clean up widget-wide settings when the last widget is disabled
    }
}

internal fun updateAppWidget(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int
) {
    // Get this week's projects (in a real app, this would come from a data source)
    val projects = SampleData.getThisWeeksProjects()
    
    // Create RemoteViews
    val views = RemoteViews(context.packageName, R.layout.todo_widget)
    
    // Set up the title with current week range
    val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
    val calendar = Calendar.getInstance()
    val startOfWeek = calendar.apply { 
        set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek) 
    }.time
    
    calendar.add(Calendar.DAY_OF_WEEK, 6)
    val endOfWeek = calendar.time
    
    val weekRange = "${dateFormat.format(startOfWeek)} - ${dateFormat.format(endOfWeek)}"
    views.setTextViewText(R.id.widget_title, "${context.getString(R.string.this_week_projects)} • $weekRange")
    
    // Set up the list view
    if (projects.isNotEmpty()) {
        // In a real app, you would use a RemoteViewsService to populate the list
        // For this example, we'll show a simple text view with the project count
        views.setViewVisibility(R.id.projects_list, View.GONE)
        views.setViewVisibility(R.id.empty_view, View.VISIBLE)
        views.setTextViewText(R.id.empty_view, "${projects.size} projects this week")
    } else {
        views.setViewVisibility(R.id.projects_list, View.GONE)
        views.setViewVisibility(R.id.empty_view, View.VISIBLE)
    }
    
    // Set up pending intents for clicks
    val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }
    
    val pendingIntent = PendingIntent.getActivity(
        context, 
        0, 
        intent, 
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    
    views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)
    
    // Update the widget
    appWidgetManager.updateAppWidget(appWidgetId, views)
    
    // In a real app, you would update the widget's data here
    // and then call notifyAppWidgetViewDataChanged to refresh the list
}
