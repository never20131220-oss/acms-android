package com.never0802.acms;

import android.app.job.JobParameters;
import android.app.job.JobService;

public class WidgetUpdateJobService extends JobService {
    @Override public boolean onStartJob(JobParameters params) {
        new Thread(() -> {
            try {
                WidgetCounts counts = SupabaseApi.fetchCounts(getApplicationContext());
                TokenStore.saveCounts(getApplicationContext(), counts);
                WidgetUpdater.renderAll(getApplicationContext(), counts, "자동 업데이트");
            } catch (Exception ignored) {
                WidgetUpdater.renderAll(getApplicationContext(), TokenStore.cachedCounts(getApplicationContext()), "자동 업데이트 대기");
            } finally {
                jobFinished(params, false);
            }
        }, "acms-widget-job").start();
        return true;
    }

    @Override public boolean onStopJob(JobParameters params) { return true; }
}
