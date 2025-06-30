package com.example.todowidget

import android.content.Context
import android.widget.RemoteViews
import android.widget.RemoteViewsService

class RemoteViewsAdapter(
    private val context: Context,
    private val layoutId: Int,
    private val textViewId: Int,
    private val items: List<String>
) : RemoteViewsService.RemoteViewsFactory {

    override fun onCreate() {}

    override fun onDataSetChanged() {}

    override fun onDestroy() {}

    override fun getCount(): Int = items.size

    override fun getViewAt(position: Int): RemoteViews {
        val remoteView = RemoteViews(context.packageName, layoutId)
        remoteView.setTextViewText(textViewId, items[position])
        return remoteView
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = position.toLong()

    override fun hasStableIds(): Boolean = true
}
