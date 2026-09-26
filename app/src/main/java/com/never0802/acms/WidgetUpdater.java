package com.never0802.acms;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class WidgetUpdater {
    private WidgetUpdater() {}

    public static void refreshAsync(Context context) {
        Context app = context.getApplicationContext();
        new Thread(() -> {
            try {
                WidgetCounts counts = SupabaseApi.fetchCounts(app);
                TokenStore.saveCounts(app, counts);
                renderAll(app, counts, "방금 업데이트");
            } catch (Exception e) {
                WidgetCounts cached = TokenStore.cachedCounts(app);
                String msg = TokenStore.access(app).isEmpty() ? "ACMS 앱에 로그인하세요" : "업데이트 실패 · ↻ 눌러 재시도";
                renderAll(app, cached, msg);
            }
        }, "acms-widget-refresh").start();
    }

    public static void saveAndRender(Context context, WidgetCounts counts) {
        TokenStore.saveCounts(context, counts);
        renderAll(context, counts, "앱에서 실시간 반영");
    }

    public static void renderAll(Context context, WidgetCounts counts, String status) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        int[] ids = manager.getAppWidgetIds(new ComponentName(context, AcmsWidgetProvider.class));
        for (int id : ids) manager.updateAppWidget(id, buildViews(context, counts, status));
    }

    private static RemoteViews buildViews(Context context, WidgetCounts counts, String status) {
        RemoteViews v = new RemoteViews(context.getPackageName(), R.layout.acms_widget);
        if (counts == null) {
            v.setTextViewText(R.id.widgetWaitingCount, "-");
            v.setTextViewText(R.id.widgetFieldCount, "-");
            v.setTextViewText(R.id.widgetPlanCount, "-");
        } else {
            v.setTextViewText(R.id.widgetWaitingCount, String.valueOf(counts.waiting));
            v.setTextViewText(R.id.widgetFieldCount, String.valueOf(counts.field));
            v.setTextViewText(R.id.widgetPlanCount, String.valueOf(counts.plan));
        }
        long updated = TokenStore.updatedAt(context);
        String time = updated > 0 ? new SimpleDateFormat("HH:mm", Locale.KOREA).format(new Date(updated)) : "--:--";
        v.setTextViewText(R.id.widgetUpdated, time + " · " + status);
        v.setOnClickPendingIntent(R.id.widgetWaiting, openIntent(context, "waiting", 101));
        v.setOnClickPendingIntent(R.id.widgetField, openIntent(context, "field", 102));
        v.setOnClickPendingIntent(R.id.widgetPlan, openIntent(context, "plan", 103));
        v.setOnClickPendingIntent(R.id.widgetRoot, openIntent(context, "", 100));
        Intent refresh = new Intent(context, AcmsWidgetProvider.class).setAction(AcmsWidgetProvider.ACTION_REFRESH);
        PendingIntent refreshPi = PendingIntent.getBroadcast(context, 104, refresh, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        v.setOnClickPendingIntent(R.id.widgetRefresh, refreshPi);
        return v;
    }

    private static PendingIntent openIntent(Context context, String action, int requestCode) {
        Intent i = new Intent(context, MainActivity.class);
        if (!action.isEmpty()) i.putExtra(MainActivity.EXTRA_OPEN, action);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return PendingIntent.getActivity(context, requestCode, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}
