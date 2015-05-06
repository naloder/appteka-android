package com.tomclaw.appsend;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Created with IntelliJ IDEA.
 * User: Solkin
 * Date: 31.10.13
 * Time: 10:56
 */
public class TaskExecutor {

    private final ExecutorService threadExecutor = Executors.newSingleThreadExecutor();

    private Future<?> future = null;

    private static class Holder {

        static TaskExecutor instance = new TaskExecutor();
    }

    public static TaskExecutor getInstance() {
        return Holder.instance;
    }

    public void execute(final Task task) {
        if (task.isPreExecuteRequired()) {
            MainExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    task.onPreExecuteMain();
                    future = threadExecutor.submit(task);
                }
            });
        } else {
            future = threadExecutor.submit(task);
        }
    }

    public void cancelTask() {
        if(future != null) {
            future.cancel(true);
        }
    }
}
