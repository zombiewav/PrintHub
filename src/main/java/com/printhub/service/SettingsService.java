package com.printhub.service;

import com.printhub.db.Database;
import com.printhub.db.Settings;
import com.printhub.model.Permission;

/**
 * The shop settings an admin can change: whether the next paid job starts by itself, how many jobs can print at
 * once, how long an unpaid online order waits, and how much ink one page uses. The queue and the stock
 * estimator read these values every time, so a saved change takes effect immediately.
 */
public class SettingsService {

    public record ShopSettings(boolean autoDispatch, int printerSlots, int unpaidTimeoutMinutes, int inkPerPage) {
    }

    private final Database db;
    private final Settings settings;

    public SettingsService(Database db) {
        this.db = db;
        this.settings = new Settings(db);
    }

    public ShopSettings load() {
        synchronized (db.lock()) {
            return new ShopSettings(settings.getBool("autoDispatch", true), settings.getInt("printerSlots", 1),
                    settings.getInt("unpaidTimeoutMinutes", 30), settings.getInt("inkPerPage", 1));
        }
    }

    /** Admin only. Rejects values outside the allowed ranges without saving anything. */
    public void save(ShopSettings s) {
        Session.require(Permission.EDIT_PRICES);
        if (s.printerSlots() < 1 || s.printerSlots() > 10) {
            throw new IllegalArgumentException("Printer slots must be between 1 and 10.");
        }
        if (s.unpaidTimeoutMinutes() < 5 || s.unpaidTimeoutMinutes() > 1440) {
            throw new IllegalArgumentException("The unpaid timeout must be between 5 and 1440 minutes.");
        }
        if (s.inkPerPage() < 1 || s.inkPerPage() > 10) {
            throw new IllegalArgumentException("Ink per page must be between 1 and 10.");
        }
        synchronized (db.lock()) {
            db.inTransaction(() -> {
                settings.set("autoDispatch", Boolean.toString(s.autoDispatch()));
                settings.set("printerSlots", Integer.toString(s.printerSlots()));
                settings.set("unpaidTimeoutMinutes", Integer.toString(s.unpaidTimeoutMinutes()));
                settings.set("inkPerPage", Integer.toString(s.inkPerPage()));
            });
        }
    }
}
