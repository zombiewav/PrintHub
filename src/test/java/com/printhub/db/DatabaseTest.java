package com.printhub.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import org.junit.jupiter.api.Test;

class DatabaseTest {

    @Test
    void schemaCreatesAllTables() throws SQLException {
        Database db = Database.inMemory();
        List<String> expected = List.of("account", "supply", "supplier", "supplier_supply", "restock",
                "stock_movement", "job_order", "payment", "price", "discount", "setting", "schema_version");
        for (String table : expected) {
            try (Statement st = db.conn().createStatement();
                    ResultSet rs = st.executeQuery(
                            "SELECT name FROM sqlite_master WHERE type='table' AND name='" + table + "'")) {
                assertTrue(rs.next(), "missing table " + table);
            }
        }
    }

    @Test
    void foreignKeysAreOn() throws SQLException {
        Database db = Database.inMemory();
        try (Statement st = db.conn().createStatement(); ResultSet rs = st.executeQuery("PRAGMA foreign_keys")) {
            assertTrue(rs.next());
            assertEquals(1, rs.getInt(1));
        }
    }

    @Test
    void emptyUntilAnAccountExists() throws SQLException {
        Database db = Database.inMemory();
        assertTrue(db.isEmpty());
        try (Statement st = db.conn().createStatement()) {
            st.executeUpdate("INSERT INTO account(username,password_hash,full_name,role) VALUES('a','x','A','ADMIN')");
        }
        assertTrue(!db.isEmpty());
    }

    @Test
    void moneyRoundsToCentavos() {
        assertEquals(30, Money.toCentavos(0.1 + 0.2));
        assertEquals(12.5, Money.fromCentavos(1250), 0.0001);
    }

    @Test
    void settingsFallBackToDefault() {
        Database db = Database.inMemory();
        Settings settings = new Settings(db);
        assertEquals(1, settings.getInt("printerSlots", 1));
        settings.set("printerSlots", "3");
        assertEquals(3, settings.getInt("printerSlots", 1));
        assertTrue(settings.getBool("autoDispatch", true));
        settings.set("autoDispatch", "false");
        assertTrue(!settings.getBool("autoDispatch", true));
    }
}
