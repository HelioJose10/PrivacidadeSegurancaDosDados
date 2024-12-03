package main.java;

import java.time.LocalDateTime;

public class Logger {
    public static void log(String message) {
        System.out.println(message);
    }

    public static String formatMessage(String message) {
        return "[" + LocalDateTime.now() + "] " + message;
    }
}
