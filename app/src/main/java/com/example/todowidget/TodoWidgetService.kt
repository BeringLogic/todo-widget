package com.example.todowidget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.example.todowidget.model.Todo
import com.example.todowidget.repository.TodoRepository
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Service that provides data for the widget's list view.
 */
class TodoWidgetService : RemoteViewsService() {
    
    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())
    
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        val appWidgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )
        
        return TodoRemoteViewsFactory(
            applicationContext,
            TodoRepository.getInstance(applicationContext),
            appWidgetId
        )
    }
    
    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
    }
}

/**
 * Factory that provides data to the RemoteViews for the list items.
 */
class TodoRemoteViewsFactory(
    private val context: Context,
    private val repository: TodoRepository,
    private val appWidgetId: Int
) : RemoteViewsService.RemoteViewsFactory {
    
    private val TAG = "TodoRemoteViewsFactory"
    private var todos: List<Todo> = emptyList()
    private var isLoading = true
    private var error: Throwable? = null
    
    override fun onCreate() {
        // Initialize data sources here
    }
    
    override fun onDataSetChanged() {
        Log.d(TAG, "onDataSetChanged called")
        
        // This runs on a background thread by default
        try {
            isLoading = true
            
            // Use runBlocking to perform the suspend function call
            runBlocking {
                try {
                    val result = withContext(Dispatchers.IO) {
                        repository.getTodos()
                    }
                    
                    result.onSuccess { todoList ->
                        // Sort todos by due date (nulls last)
                        todos = todoList.sortedWith(compareBy<Todo> { 
                            it.dueDate?.let { dateStr ->
                                try {
                                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateStr)?.time
                                } catch (e: Exception) {
                                    null
                                }
                            } ?: Long.MAX_VALUE 
                        }.thenBy { it.title }) // Secondary sort by title for same dates
                        
                        error = null
                        Log.d(TAG, "Successfully loaded and sorted ${todoList.size} todos")
                    }.onFailure { e ->
                        todos = emptyList()
                        error = e
                        Log.e(TAG, "Error loading todos", e)
                    }
                } catch (e: Exception) {
                    todos = emptyList()
                    error = e
                    Log.e(TAG, "Unexpected error in onDataSetChanged", e)
                } finally {
                    isLoading = false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in onDataSetChanged", e)
            todos = emptyList()
            error = e
            isLoading = false
        }
    }
    
    override fun onDestroy() {
        // Cleanup resources if needed
    }
    
    override fun getCount(): Int {
        return if (error != null || isLoading) 0 else todos.size
    }
    
    override fun getViewAt(position: Int): RemoteViews {
        Log.d(TAG, "getViewAt: $position")
        
        if (position < 0 || position >= todos.size) {
            Log.e(TAG, "Invalid position: $position, size: ${todos.size}")
            // Return a default view with an error message
            val rv = RemoteViews(context.packageName, android.R.layout.simple_list_item_1)
            rv.setTextViewText(android.R.id.text1, "Error loading item")
            return rv
        }
        
        val todo = todos[position]
        Log.d(TAG, "Creating view for todo: ${todo.id} - ${todo.title}")
        
        // Format the todo text with bullet point
        val todoText = "• ${todo.title}"
        
        // Format the due date if available
        val outputFormat = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
        val dueDateStr = todo.dueDate?.let { dateStr ->
            try {
                val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateStr)
                date?.let { outputFormat.format(it) } ?: ""
            } catch (e: Exception) {
                Log.e(TAG, "Error formatting date: $dateStr", e)
                ""
            }
        } ?: ""
        
        val rv = RemoteViews(context.packageName, R.layout.todo_list_item)
        rv.setTextViewText(R.id.todo_text, todoText)
        
        // Set the due date text if available
        if (dueDateStr.isNotEmpty()) {
            rv.setTextViewText(R.id.due_date_text, dueDateStr)
            rv.setViewVisibility(R.id.due_date_text, View.VISIBLE)
        } else {
            rv.setViewVisibility(R.id.due_date_text, View.GONE)
        }
        
        // Set click intent
        val fillInIntent = Intent().apply {
            putExtra("todo_id", todo.id)
        }
        
        // Set the fill-in intent
        rv.setOnClickFillInIntent(R.id.todo_text, fillInIntent)
        
        return rv
    }
    
    override fun getLoadingView(): RemoteViews? {
        // Return a custom loading view, or null to use the default
        return null
    }
    
    override fun getViewTypeCount(): Int {
        return 1 // We only have one type of view
    }
    
    override fun getItemId(position: Int): Long {
        return if (position < 0 || position >= todos.size) {
            position.toLong()
        } else {
            todos[position].id.hashCode().toLong()
        }
    }
    
    override fun hasStableIds(): Boolean = true
}
