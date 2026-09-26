package com.never0802.acms;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;

public final class WidgetScheduler {
    private static final int JOB_ID = 802;
    private static final long FIFTEEN_MINUTES = 15L * 60L * 1000L;
    private WidgetScheduler() {}

    public static void schedule(Context context) {
        JobScheduler js = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if (js == null) return;
        JobInfo info = new JobInfo.Builder(JOB_ID, new ComponentName(context, WidgetUpdateJobService.class))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPeriodic(FIFTEEN_MINUTES)
                .setPersisted(true)
                .build();
        js.schedule(info);
    }
}
