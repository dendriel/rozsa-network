package com.rozsa.network;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

// Debug purposes.
public class Logger {
    public static void info(String format, Object...args) {
        String msg = String.format(format, args);
        printLog(msg, "INFO", false);
    }

    public static void warn(String format, Object...args) {
        String msg = String.format(format, args);
        printLog(msg, "WARN", false);
    }

    public static void error(String format, Object...args) {
        String msg = String.format(format, args);
        printLog(msg, "ERROR", true);
    }

    private static void printLog(String msg, String severity, boolean trace) {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
        Date date = new Date();
        String dateStr = dateFormat.format(date);

        String formatted = String.format("%s [%s] %s", dateStr, severity, msg);

        if (trace) {
            StackTraceElement[] stacktrace = Thread.currentThread().getStackTrace();
            StackTraceElement e = stacktrace[3];
            String[] classNameTokens = e.getClassName().split("\\.");
            String className = classNameTokens[classNameTokens.length-1];

            formatted = String.format("%s [%s:%s:%d]", className, e.getMethodName(), e.getLineNumber());
        }

        System.out.println(formatted);
    }
}
