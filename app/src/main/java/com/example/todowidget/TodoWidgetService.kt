package com.example.todowidget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.example.todowidget.model.Todo
import com.example.todowidget.repository.TodoRepository
import kotlinx.coroutines.*
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
            intent,
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
    intent: Intent,
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
                        todos = todoList
                        error = null
                        Log.d(TAG, "Successfully loaded ${todoList.size} todos")
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
        Log.d(TAG, "getViewAt position: $position")
        
        val remoteView = RemoteViews(context.packageName, R.layout.todo_item)
        
        if (position < 0 || position >= todos.size) {
            return remoteView
        }
        
        val todo = todos[position]
        
        // Set the todo text
        remoteView.setTextViewText(R.id.todo_text, todo.title)
        
        // Set the completion status
        remoteView.setImageViewResource(
            R.id.checkbox,
            if (todo.completed) R.drawable.ic_check_circle_24dp
            else R.drawable.ic_radio_button_unchecked_24dp
        )
        
        // Set the click intent for the item
        val fillInIntent = Intent().apply {
            // You can add extras to the intent if needed
            // For example, to open a specific todo when clicked:
            // putExtra("todo_id", todo.id)
        }
        
        remoteView.setOnClickFillInIntent(R.id.todo_item_container, fillInIntent)
        
        return remoteView
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
            todos[position].id?.toLong() ?: position.toLong()
        }
    }
    
    override fun hasStableIds(): Boolean {
        return true
    }
}
