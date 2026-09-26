package com.never0802.acms;

import android.content.Context;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public final class SupabaseApi {
    private static final String BASE = "https://jcbijiaqshkrqwqfthqc.supabase.co";
    private static final String API_KEY = "sb_publishable_qln64vd0zgK3uIEIhWVivQ_HUntYLCR";

    private SupabaseApi() {}

    public static WidgetCounts fetchCounts(Context context) throws Exception {
        String access = TokenStore.access(context);
        if (access.isEmpty()) throw new IllegalStateException("login required");
        HttpResult r = post(BASE + "/rest/v1/rpc/acms_widget_counts", access, "{}");
        if (r.code == 401) {
            if (!refreshSession(context)) throw new IllegalStateException("session expired");
            access = TokenStore.access(context);
            r = post(BASE + "/rest/v1/rpc/acms_widget_counts", access, "{}");
        }
        if (r.code < 200 || r.code >= 300) throw new IllegalStateException("widget rpc " + r.code);
        JSONObject o = new JSONObject(r.body);
        return new WidgetCounts(o.optInt("waiting", 0), o.optInt("field", 0), o.optInt("plan", 0));
    }

    private static boolean refreshSession(Context context) {
        try {
            String refresh = TokenStore.refresh(context);
            if (refresh.isEmpty()) return false;
            JSONObject body = new JSONObject();
            body.put("refresh_token", refresh);
            HttpResult r = postNoBearer(BASE + "/auth/v1/token?grant_type=refresh_token", body.toString());
            if (r.code < 200 || r.code >= 300) return false;
            JSONObject o = new JSONObject(r.body);
            String access = o.optString("access_token", "");
            String nextRefresh = o.optString("refresh_token", refresh);
            if (access.isEmpty()) return false;
            TokenStore.saveSession(context, access, nextRefresh);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static HttpResult post(String endpoint, String access, String body) throws Exception {
        HttpURLConnection c = open(endpoint);
        c.setRequestProperty("Authorization", "Bearer " + access);
        write(c, body);
        return read(c);
    }

    private static HttpResult postNoBearer(String endpoint, String body) throws Exception {
        HttpURLConnection c = open(endpoint);
        write(c, body);
        return read(c);
    }

    private static HttpURLConnection open(String endpoint) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(endpoint).openConnection();
        c.setConnectTimeout(12000);
        c.setReadTimeout(12000);
        c.setRequestMethod("POST");
        c.setDoOutput(true);
        c.setRequestProperty("apikey", API_KEY);
        c.setRequestProperty("Content-Type", "application/json");
        c.setRequestProperty("Accept", "application/json");
        return c;
    }

    private static void write(HttpURLConnection c, String body) throws Exception {
        byte[] data = body.getBytes(StandardCharsets.UTF_8);
        try (OutputStream out = c.getOutputStream()) { out.write(data); }
    }

    private static HttpResult read(HttpURLConnection c) throws Exception {
        int code = c.getResponseCode();
        InputStream in = code >= 200 && code < 400 ? c.getInputStream() : c.getErrorStream();
        StringBuilder sb = new StringBuilder();
        if (in != null) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
            }
        }
        c.disconnect();
        return new HttpResult(code, sb.toString());
    }

    private static final class HttpResult {
        final int code;
        final String body;
        HttpResult(int code, String body) { this.code = code; this.body = body; }
    }
}
