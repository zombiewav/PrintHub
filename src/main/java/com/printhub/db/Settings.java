package com.printhub.db;

import java.util.List;

/** Key/value settings stored in the database (rush fee, printer slots, auto-dispatch, ...). */
public class Settings {

    private final Database db;

    public Settings(Database db) {
        this.db = db;
    }

    private String get(String key) {
        List<String> rows = db.query("SELECT value FROM setting WHERE key = ?", rs -> rs.getString(1), key);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public int getInt(String key, int fallback) {
        String v = get(key);
        if (v == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public boolean getBool(String key, boolean fallback) {
        String v = get(key);
        return v == null ? fallback : Boolean.parseBoolean(v.trim());
    }

    public double getDouble(String key, double fallback) {
        String v = get(key);
        if (v == null) {
            return fallback;
        }
        try {
            return Double.parseDouble(v.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public void set(String key, String value) {
        db.update("INSERT INTO setting(key, value) VALUES (?, ?) ON CONFLICT(key) DO UPDATE SET value = excluded.value",
                key, value);
    }
}
