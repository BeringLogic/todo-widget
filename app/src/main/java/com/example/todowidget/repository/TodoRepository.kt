package com.example.todowidget.repository

import android.content.Context
import android.util.Log
import com.example.todowidget.R
import com.example.todowidget.model.Todo
import com.example.todowidget.network.TodoApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.UnknownHostException
import java.text.SimpleDateFormat
import java.util.*
import java.util.Calendar

class TodoRepository private constructor(
    private val context: Context
) {
    private val apiService = TodoApiService.create()
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
    
    suspend fun getTodos(): Result<List<Todo>> = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d(TAG, "Fetching todos from API...")
            val todos = apiService.getTodos()
            Log.d(TAG, "Successfully fetched ${todos.size} todos")
            
            // Log all todos and their due dates
            todos.forEachIndexed { index, todo ->
                Log.d(TAG, "Todo[$index]: '${todo.title}' due: ${todo.dueDate ?: "No due date"}")
            }
            
            // Filter todos to only include those with due dates in the past or this week
            val filteredTodos = todos.filter { todo ->
                todo.dueDate?.let { dueDateStr ->
                    try {
                        val dueDate = dateFormat.parse(dueDateStr) ?: return@filter false
                        val calendar = Calendar.getInstance()
                        
                        // Log current date and time for debugging
                        val now = Date()
                        Log.d(TAG, "Current time: $now")
                        
                        // Set to end of week (next Sunday at 23:59:59.999)
                        calendar.time = now
                        calendar.set(Calendar.HOUR_OF_DAY, 23)
                        calendar.set(Calendar.MINUTE, 59)
                        calendar.set(Calendar.SECOND, 59)
                        calendar.set(Calendar.MILLISECOND, 999)
                        calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
                        calendar.add(Calendar.WEEK_OF_YEAR, 1)
                        val endOfWeek = calendar.time
                        
                        Log.d(TAG, "End of week: $endOfWeek")
                        Log.d(TAG, "Due date: $dueDate (${dueDate.time}), End of week: ${endOfWeek.time}")
                        
                        // Check if due date is in the past or within this week
                        val isInRange = dueDate.time <= endOfWeek.time
                        Log.d(TAG, "Todo '${todo.title}' due $dueDate is ${if (isInRange) "IN" else "OUT OF"} range")
                        isInRange
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing date: '$dueDateStr' for todo '${todo.title}'", e)
                        false
                    }
                } ?: run {
                    Log.d(TAG, "Excluding todo '${todo.title}' - no due date")
                    false // Exclude todos without due dates
                }
            }
            
            Log.d(TAG, "Filtered to ${filteredTodos.size} todos with due dates in the past or this week")
            if (filteredTodos.isNotEmpty()) {
                Log.d(TAG, "First filtered todo: ${filteredTodos[0]}")
            }
            Result.success(filteredTodos)
        } catch (e: UnknownHostException) {
            val errorMsg = context.getString(R.string.error_network_unavailable)
            Log.e(TAG, errorMsg, e)
            Result.failure(Exception(errorMsg, e))
        } catch (e: Exception) {
            val errorMsg = context.getString(R.string.error_loading_todos, e.message ?: "Unknown error".toString())
            Log.e(TAG, errorMsg, e)
            Result.failure(Exception(errorMsg, e))
        }
    }
    
    companion object {
        private const val TAG = "TodoRepository"
        
        @Volatile private var instance: TodoRepository? = null
        
        @Synchronized
        fun getInstance(context: Context): TodoRepository {
            return instance ?: synchronized(this) {
                instance ?: TodoRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}
