package com.never0802.acms;

import android.content.Context;
import android.content.SharedPreferences;

public final class TokenStore {
    private static final String PREF = "acms_native";
    private static final String ACCESS = "access_token";
    private static final String REFRESH = "refresh_token";
    private static final String WAITING = "waiting";
    private static final String FIELD = "field";
    private static final String PLAN = "plan";
    private static final String UPDATED = "updated";

    private TokenStore() {}

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    public static void saveSession(Context c, String access, String refresh) {
        SharedPreferences.Editor e = prefs(c).edit();
        if (access == null || access.isEmpty()) e.remove(ACCESS); else e.putString(ACCESS, access);
        if (refresh == null || refresh.isEmpty()) e.remove(REFRESH); else e.putString(REFRESH, refresh);
        e.apply();
    }

    public static void clearSession(Context c) {
        prefs(c).edit().remove(ACCESS).remove(REFRESH).apply();
    }

    public static String access(Context c) { return prefs(c).getString(ACCESS, ""); }
    public static String refresh(Context c) { return prefs(c).getString(REFRESH, ""); }

    public static void saveCounts(Context c, WidgetCounts v) {
        prefs(c).edit()
                .putInt(WAITING, v.waiting)
                .putInt(FIELD, v.field)
                .putInt(PLAN, v.plan)
                .putLong(UPDATED, System.currentTimeMillis())
                .apply();
    }

    public static WidgetCounts cachedCounts(Context c) {
        SharedPreferences p = prefs(c);
        if (!p.contains(UPDATED)) return null;
        return new WidgetCounts(p.getInt(WAITING, 0), p.getInt(FIELD, 0), p.getInt(PLAN, 0));
    }

    public static long updatedAt(Context c) { return prefs(c).getLong(UPDATED, 0L); }
}
