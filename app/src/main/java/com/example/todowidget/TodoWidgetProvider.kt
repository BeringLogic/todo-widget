package com.example.todowidget

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import android.widget.RemoteViews
import androidx.work.*
import com.example.todowidget.model.Todo
import com.example.todowidget.repository.TodoRepository
import com.example.todowidget.UpdateWorker

import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.Date

class TodoWidgetProvider : AppWidgetProvider() {

    companion object {
        private const val TAG = "TodoWidget"
        const val ACTION_UPDATE_WIDGET = "com.example.todowidget.action.UPDATE_WIDGET"
        const val ACTION_REFRESH = "com.example.todowidget.ACTION_REFRESH"

        /**
         * Schedules the periodic update worker
         */
        fun scheduleUpdateWorker(context: Context) {
            try {
                Log.d(TAG, "Scheduling widget update worker")
                UpdateWorker.schedule(context)
                Log.d(TAG, "Successfully scheduled periodic work")
            } catch (e: Exception) {
                Log.e(TAG, "Error scheduling update worker", e)
                // Try to schedule a one-time update as fallback
                enqueueImmediateUpdate(context)
            }
        }


        /**
         * Enqueues an immediate one-time update
         */
        fun enqueueImmediateUpdate(context: Context) {
            try {
                UpdateWorker.enqueueImmediateUpdate(context)
                Log.d(TAG, "Enqueued immediate update work")
            } catch (e: Exception) {
                Log.e(TAG, "Error enqueuing immediate update", e)
            }
        }

        /**
         * Cancels all scheduled work
         */
        fun cancelAllWork(context: Context) {
            try {
                WorkManager.getInstance(context).cancelAllWorkByTag(UpdateWorker.WORKER_TAG)
                Log.d(TAG, "Cancelled all scheduled work")
            } catch (e: Exception) {
                Log.e(TAG, "Error cancelling work", e)
            }
        }

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            Log.d(TAG, "Updating widget $appWidgetId")
            
            // Show loading state
            val views = RemoteViews(context.packageName, R.layout.todo_widget)
            views.setTextViewText(R.id.empty_view, context.getString(R.string.loading))
            views.setTextViewText(R.id.status_text, "Loading...")
            
            // Set up click on the entire widget to refresh
            val refreshIntent = Intent(context, TodoWidgetProvider::class.java).apply {
                action = ACTION_UPDATE_WIDGET
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            
            // Set up refresh button click handler
            val refreshPendingIntent = PendingIntent.getBroadcast(
                context,
                1,
                Intent(context, TodoWidgetProvider::class.java).apply {
                    action = ACTION_REFRESH
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.refresh_button, refreshPendingIntent)
            
            // Make the entire widget clickable to refresh
            val widgetClickPendingIntent = PendingIntent.getBroadcast(
                context,
                2,
                Intent(context, TodoWidgetProvider::class.java).apply {
                    action = ACTION_REFRESH
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.empty_view, widgetClickPendingIntent)
            
            appWidgetManager.updateAppWidget(appWidgetId, views)
            
            // Start a coroutine to fetch data
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    val repository = TodoRepository.getInstance(context)
                    val result = repository.getTodos()
                    
                    result.onSuccess { todos ->
                        updateWidgetWithTodos(context, appWidgetManager, appWidgetId, todos)
                    }.onFailure { e ->
                        updateWidgetWithError(context, appWidgetManager, appWidgetId, e)
                    }
                } catch (e: Exception) {
                    updateWidgetWithError(context, appWidgetManager, appWidgetId, e)
                }
            }
        }
        
        private fun updateWidgetWithTodos(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            todos: List<Todo>
        ) {
            Log.d(TAG, "Updating widget $appWidgetId with ${todos.size} todos")
            
            val views = RemoteViews(context.packageName, R.layout.todo_widget)
            
            if (todos.isEmpty()) {
                Log.d(TAG, "No todos found, showing empty state")
                views.setTextViewText(R.id.empty_view, context.getString(R.string.no_tasks_this_week))
            } else {
                Log.d(TAG, "Processing ${todos.size} todos")
                
                // Sort todos by due date (earliest first), with todos without due dates at the end
                val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                val sortedTodos = todos.sortedWith(compareBy<Todo> { todo ->
                    todo.dueDate?.let { dueDateStr ->
                        try {
                            dateFormat.parse(dueDateStr)?.time ?: Long.MAX_VALUE - 1
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing date: $dueDateStr", e)
                            Long.MAX_VALUE - 1
                        }
                    } ?: Long.MAX_VALUE
                })
                
                // Limit the number of todos to display to avoid performance issues
                val maxTodos = 10
                val todosToShow = if (sortedTodos.size > maxTodos) {
                    Log.d(TAG, "Limiting display to first $maxTodos todos out of ${sortedTodos.size}")
                    sortedTodos.take(maxTodos)
                } else {
                    sortedTodos
                }
                
                // Format the todo list with due dates
                val outputFormat = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
                val todoText = todosToShow.joinToString("\n") { todo ->
                    val dueDateStr = todo.dueDate?.let { dateStr ->
                        try {
                            val date = dateFormat.parse(dateStr)
                            date?.let { " (${outputFormat.format(it)})" } ?: ""
                        } catch (e: Exception) {
                            Log.e(TAG, "Error formatting date: $dateStr", e)
                            ""
                        }
                    } ?: ""
                    "• ${todo.title}$dueDateStr"
                }
                
                Log.d(TAG, "Setting todo text with ${todosToShow.size} items")
                views.setTextViewText(R.id.empty_view, todoText)
            }
            
            // Update the status text with last updated time
            val timeString = SimpleDateFormat("h:mm a", Locale.getDefault())
                .format(Date())
            views.setTextViewText(R.id.status_text, 
                context.getString(R.string.last_updated, timeString))
            
            // Update the widget
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
        
        private fun updateWidgetWithError(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            error: Throwable
        ) {
            Log.e(TAG, "Error updating widget $appWidgetId", error)
            
            val views = RemoteViews(context.packageName, R.layout.todo_widget)
            
            // Show error message
            val errorMsg = context.getString(R.string.error_loading_todos, 
                error.localizedMessage ?: "Unknown error")
            views.setTextViewText(R.id.empty_view, errorMsg)
            
            // Update status text
            views.setTextViewText(R.id.status_text, 
                context.getString(R.string.last_updated, "Error"))
            
            // Schedule a retry
            scheduleUpdateWorker(context)
            
            // Update the widget
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
    
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        Log.d(TAG, "onUpdate called for ${appWidgetIds.size} widgets")
        
        // Update all widgets
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
        
        // Schedule the periodic updates
        scheduleUpdateWorker(context)
    }
    
    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        Log.d(TAG, "Widget enabled, scheduling updates")
        scheduleUpdateWorker(context)
    }
    
    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        Log.d(TAG, "Widget disabled, cancelling updates")
        cancelAllWork(context)
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        
        when (intent.action) {
            ACTION_UPDATE_WIDGET, ACTION_REFRESH -> {
                val appWidgetId = intent.getIntExtra(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID
                )
                
                if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    // Force refresh from network if it's a manual refresh
                    if (intent.action == ACTION_REFRESH) {
                        val appWidgetManager = AppWidgetManager.getInstance(context)
                        val views = RemoteViews(context.packageName, R.layout.todo_widget)
                        views.setTextViewText(R.id.status_text, context.getString(R.string.refreshing))
                        appWidgetManager.partiallyUpdateAppWidget(appWidgetId, views)
                        
                        // Invalidate the cache and force a refresh
                        TodoRepository.getInstance(context).invalidateCache()
                    }
                    
                    // Update the widget
                    updateAppWidget(context, AppWidgetManager.getInstance(context), appWidgetId)
                } else {
                    // Fallback: Update all widgets if we couldn't get the ID
                    val appWidgetManager = AppWidgetManager.getInstance(context)
                    val appWidgetIds = appWidgetManager.getAppWidgetIds(
                        ComponentName(context, TodoWidgetProvider::class.java)
                    )
                    if (appWidgetIds.isNotEmpty()) {
                        onUpdate(context, appWidgetManager, appWidgetIds)
                    }
                }
            }
        }
    }
}
