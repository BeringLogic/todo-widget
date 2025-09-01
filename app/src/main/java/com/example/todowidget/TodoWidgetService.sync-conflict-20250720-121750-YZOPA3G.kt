package com.example.todowidget

import android.content.Intent
import android.widget.RemoteViewsService

/**
 * Service that provides data for the widget's list view.
 * This is a placeholder for future implementation.
 */
class TodoWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        // In a real app, you would return a custom RemoteViewsFactory here
        // that provides the data for the widget's list view
        throw UnsupportedOperationException("Not yet implemented")
    }
}
