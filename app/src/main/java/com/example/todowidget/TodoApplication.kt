package com.example.todowidget

import android.content.Context
import android.util.Log
import androidx.multidex.MultiDexApplication
import androidx.work.Configuration
import androidx.work.WorkManager
import com.example.todowidget.TodoWidgetProvider.Companion.scheduleUpdateWorker

class TodoApplication : MultiDexApplication(), Configuration.Provider {
    
    companion object {
        private const val TAG = "TodoApplication"
    }
    
    override fun getWorkManagerConfiguration(): Configuration {
        return Configuration.Builder()
            .setMinimumLoggingLevel(if (BuildConfig.DEBUG) Log.DEBUG else Log.ERROR)
            .build()
    }
    
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        androidx.multidex.MultiDex.install(this)
    }
    
    override fun onCreate() {
        super.onCreate()
        try {
            // Schedule the initial work
            scheduleUpdateWorker(this)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing application", e)
        }
    }
}
