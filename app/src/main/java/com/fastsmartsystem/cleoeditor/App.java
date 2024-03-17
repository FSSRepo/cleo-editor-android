package com.fastsmartsystem.cleoeditor;

import android.app.Application;

public class App extends Application {
    private Thread.UncaughtExceptionHandler defaultHandler;
    public static String dataDirectory;

    @Override
    public void onCreate() {
        super.onCreate();
        defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this::handleUncaughtException);
    }

    public void handleUncaughtException(Thread thread, Throwable e) {
        StackTraceElement[] arr = e.getStackTrace();
        final StringBuffer report = new StringBuffer(e.toString());
        final String lineSeperator = "-------------------------------\n\n";
        report.append("--------- Stack trace ---------\n\n");
        for (int i = 0; i < arr.length; i++) {
            report.append("    ");
            report.append(arr[i].toString());
            report.append(lineSeperator);
        }
        defaultHandler.uncaughtException(thread, e);
    }
}
