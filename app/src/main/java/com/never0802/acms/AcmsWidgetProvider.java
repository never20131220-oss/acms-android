package com.never0802.acms;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;

public class AcmsWidgetProvider extends AppWidgetProvider {
    public static final String ACTION_REFRESH = "com.never0802.acms.WIDGET_REFRESH";

    @Override public void onEnabled(Context context) {
        WidgetScheduler.schedule(context);
        WidgetUpdater.refreshAsync(context);
    }

    @Override public void onUpdate(Context context, AppWidgetManager manager, int[] appWidgetIds) {
        WidgetCounts cached = TokenStore.cachedCounts(context);
        WidgetUpdater.renderAll(context, cached, cached == null ? "로그인 후 새로고침" : "저장된 현황");
        WidgetUpdater.refreshAsync(context);
    }

    @Override public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (ACTION_REFRESH.equals(intent.getAction())) WidgetUpdater.refreshAsync(context);
    }
}
