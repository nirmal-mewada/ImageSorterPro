package com.imagesorter.util;

import java.util.concurrent.TimeUnit;

public final class OsTheme {

    private OsTheme() {}

    public static boolean isDark() {
        if (!System.getProperty("os.name", "").toLowerCase().contains("mac")) {
            return false;
        }
        try {
            Process p = new ProcessBuilder("defaults", "read", "-g", "AppleInterfaceStyle")
                    .start();
            p.waitFor(1, TimeUnit.SECONDS);
            return p.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }
}
