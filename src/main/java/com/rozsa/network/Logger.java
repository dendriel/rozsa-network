package com.rozsa.network;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

// Debug purposes.
public class Logger {
    public static void info(String format, Object...args) {
        String msg = String.format(format, args);
        printLog(msg, "INFO");
    }

    public static void warn(String format, Object...args) {
        String msg = String.format(format, args);
        printLog(msg, "WARN");
    }

    private static void printLog(String msg, String severity) {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
        Date date = new Date();
        String dateStr = dateFormat.format(date);

        String formatted = String.format("%s [%s] %s", dateStr, severity, msg);
        System.out.println(formatted);
    }
}
