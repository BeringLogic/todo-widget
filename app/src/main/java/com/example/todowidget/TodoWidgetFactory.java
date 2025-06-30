package com.example.todowidget;

import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

public class TodoWidgetFactory implements RemoteViewsService.RemoteViewsFactory {
    private Context context;
    private int appWidgetId;

    public TodoWidgetFactory(Context context, int appWidgetId) {
        this.context = context;
        this.appWidgetId = appWidgetId;
    }

    @Override
    public void onCreate() {
        // Initialize data source
    }

    @Override
    public void onDataSetChanged() {
        // Update data source
    }

    @Override
    public void onDestroy() {
        // Cleanup
    }

    @Override
    public int getCount() {
        // Return number of items
        return 0;
    }

    @Override
    public RemoteViews getViewAt(int position) {
        RemoteViews views = new RemoteViews(context.getPackageName(), android.R.layout.simple_list_item_1);
        // Set item text
        return views;
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }
}
