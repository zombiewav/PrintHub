package com.printhub.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.printhub.db.Database;
import com.printhub.db.Settings;
import com.printhub.model.Account;
import com.printhub.model.Role;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SettingsServiceTest {

    private Database db;
    private SettingsService service;

    @BeforeEach
    void setUp() {
        db = Database.inMemory();
        service = new SettingsService(db);
        Session.set(new Account("admin", "x", "Admin", Role.ADMIN));
    }

    @AfterEach
    void tearDown() {
        Session.clear();
    }

    @Test
    void defaultsMatchTheDocumentedOnes() {
        SettingsService.ShopSettings s = service.load();
        assertTrue(s.autoDispatch());
        assertEquals(1, s.printerSlots());
        assertEquals(30, s.unpaidTimeoutMinutes());
        assertEquals(1, s.inkPerPage());
    }

    @Test
    void savedSettingsAreReadBackByTheQueueAndEstimatorSettings() {
        service.save(new SettingsService.ShopSettings(false, 3, 45, 2));
        SettingsService.ShopSettings s = new SettingsService(db).load();
        assertFalse(s.autoDispatch());
        assertEquals(3, s.printerSlots());
        assertEquals(45, s.unpaidTimeoutMinutes());
        assertEquals(2, s.inkPerPage());
        // the services read the same keys, so a saved change takes effect without a restart
        Settings raw = new Settings(db);
        assertFalse(raw.getBool("autoDispatch", true));
        assertEquals(3, raw.getInt("printerSlots", 1));
        assertEquals(45, raw.getInt("unpaidTimeoutMinutes", 30));
        assertEquals(2, raw.getInt("inkPerPage", 1));
    }

    @Test
    void outOfRangeValuesAreRejectedAndNothingIsSaved() {
        assertThrows(IllegalArgumentException.class, () -> service.save(new SettingsService.ShopSettings(true, 0, 30, 1)));
        assertThrows(IllegalArgumentException.class, () -> service.save(new SettingsService.ShopSettings(true, 11, 30, 1)));
        assertThrows(IllegalArgumentException.class, () -> service.save(new SettingsService.ShopSettings(true, 1, 4, 1)));
        assertThrows(IllegalArgumentException.class, () -> service.save(new SettingsService.ShopSettings(true, 1, 1441, 1)));
        assertThrows(IllegalArgumentException.class, () -> service.save(new SettingsService.ShopSettings(true, 1, 30, 0)));
        assertEquals(1, service.load().printerSlots());
    }

    @Test
    void staffCanReadButNotChangeSettings() {
        Session.set(new Account("lm", "x", "Liza", Role.STAFF));
        assertEquals(1, service.load().printerSlots());
        assertThrows(SecurityException.class, () -> service.save(new SettingsService.ShopSettings(true, 2, 30, 1)));
    }
}
