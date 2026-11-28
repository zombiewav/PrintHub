package com.printhub.data;

import java.nio.file.Path;

/**
 * Where PrintHub keeps its database and customer uploads.
 * <ul>
 * <li>The {@code printhub.home} system property, when set, wins.</li>
 * <li>Started from the packaged PrintHub.exe: {@code %LOCALAPPDATA%\PrintHub}. The program folder may be read-only
 * (Program Files) and gets replaced by the next version, so the shop's data must not live there.</li>
 * <li>Otherwise (run from NetBeans or the command line): the folder it was started from, as before.</li>
 * </ul>
 */
public final class AppPaths {

    private AppPaths() {
    }

    public static Path home() {
        return home(System.getProperty("printhub.home"), System.getProperty("jpackage.app-path"),
                System.getenv("LOCALAPPDATA"), System.getProperty("user.home"), System.getProperty("user.dir"));
    }

    /** The rules above, with every input passed in so they can be tested. */
    static Path home(String override, String packagedExe, String localAppData, String userHome, String workingDir) {
        if (override != null && !override.isBlank()) {
            return Path.of(override);
        }
        if (packagedExe != null && !packagedExe.isBlank()) {
            if (localAppData != null && !localAppData.isBlank()) {
                return Path.of(localAppData, "PrintHub");
            }
            return Path.of(userHome, "AppData", "Local", "PrintHub");
        }
        return Path.of(workingDir);
    }

    public static Path database() {
        return home().resolve("printhub.db");
    }

    public static Path uploads() {
        return home().resolve("uploads");
    }
}
