package com.tomclaw.appsend.util;

import com.tomclaw.appsend.core.Task;
import com.tomclaw.appsend.core.TaskExecutor;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;

import static com.tomclaw.appsend.AppSend.app;
import static com.tomclaw.appsend.util.StreamHelper.safeClose;

public class Analytics {

    private static final String API_URL = "https://zibuhoker.ru/api/track.php";

    private static class Holder {
        static Analytics instance = new Analytics().createAnalytics();
    }

    public static Analytics getInstance() {
        return Holder.instance;
    }

    private String uniqueID;

    private Analytics() {
    }

    public static void trackEvent(final String event, final boolean isManual) {
        getInstance().trackEventInternal(event, isManual);
    }

    private void trackEventInternal(final String event, final boolean isManual) {
        TaskExecutor.getInstance().execute(new Task() {
            @Override
            public void executeBackground() {
                HttpParamsBuilder params = new HttpParamsBuilder()
                        .appendParam("app_id", app().getPackageName())
                        .appendParam("dev_id", uniqueID)
                        .appendParam("event", event)
                        .appendParam("manual", isManual ? 1 : 0);
                try {
                    String result = HttpUtil.executePost(API_URL, params);
                    Logger.log(result);
                } catch (IOException ex) {
                    Logger.log("error sending analytics track", ex);
                }
            }
        });
    }

    private File analyticsFile() {
        return new File(app().getFilesDir(), "analytics.uuid");
    }

    private Analytics createAnalytics() {
        String uuid = null;
        DataInputStream is = null;
        try {
            File file = analyticsFile();
            if (file.exists()) {
                is = new DataInputStream(new FileInputStream(file));
                uuid = is.readUTF();
            }
        } catch (IOException ignored) {
        } finally {
            safeClose(is);
        }
        if (uuid == null) {
            uuid = UUID.randomUUID().toString();
        }
        if (!uuid.equals(uniqueID)) {
            DataOutputStream os = null;
            try {
                File file = analyticsFile();
                os = new DataOutputStream(new FileOutputStream(file));
                os.writeUTF(uuid);
            } catch (IOException ignored) {
            } finally {
                safeClose(os);
            }
        }
        uniqueID = uuid;
        return this;
    }
}