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
    void failedTransactionRollsBackEveryWrite() {
        Database db = Database.inMemory();
        try {
            db.inTransaction(() -> {
                db.update("INSERT INTO setting(key,value) VALUES('a','1')");
                throw new IllegalStateException("boom");
            });
        } catch (IllegalStateException expected) {
            assertEquals("boom", expected.getMessage());
        }
        assertEquals(0, db.query("SELECT COUNT(*) FROM setting", rs -> rs.getInt(1)).get(0));
    }

    @Test
    void nestedTransactionsCommitTogether() {
        Database db = Database.inMemory();
        db.inTransaction(() -> {
            db.update("INSERT INTO setting(key,value) VALUES('a','1')");
            db.inTransaction(() -> db.update("INSERT INTO setting(key,value) VALUES('b','2')"));
        });
        assertEquals(2, db.query("SELECT COUNT(*) FROM setting", rs -> rs.getInt(1)).get(0));
    }

    @Test
    void olderDatabaseGetsTheNewColumnsAndKeepsItsRows() throws Exception {
        java.nio.file.Path file = java.nio.file.Files.createTempFile("printhub-v1", ".db");
        file.toFile().deleteOnExit();
        try (java.sql.Connection c = java.sql.DriverManager.getConnection("jdbc:sqlite:" + file);
                Statement st = c.createStatement()) {
            st.execute("CREATE TABLE schema_version (version INTEGER NOT NULL)");
            st.execute("INSERT INTO schema_version VALUES (1)");
            st.execute("CREATE TABLE job_order (number INTEGER PRIMARY KEY, customer TEXT NOT NULL, service TEXT NOT NULL,"
                    + " size TEXT NOT NULL, pages INTEGER NOT NULL, copies INTEGER NOT NULL, rush INTEGER NOT NULL,"
                    + " status TEXT NOT NULL, payment_status TEXT NOT NULL, source TEXT NOT NULL, file_path TEXT,"
                    + " total_centavos INTEGER NOT NULL DEFAULT 0, discount_name TEXT,"
                    + " discount_centavos INTEGER NOT NULL DEFAULT 0, created_at TEXT NOT NULL, cancelled_at TEXT)");
            st.execute("INSERT INTO job_order(number, customer, service, size, pages, copies, rush, status,"
                    + " payment_status, source, created_at) VALUES (7,'Old Customer','BW','A4',1,1,0,'PENDING','PAID',"
                    + "'WALK_IN','2026-11-24T09:00')");
            st.execute("CREATE TABLE payment (id TEXT PRIMARY KEY, time TEXT NOT NULL, customer TEXT NOT NULL,"
                    + " job_id TEXT NOT NULL, amount_centavos INTEGER NOT NULL, method TEXT NOT NULL, reference TEXT,"
                    + " status TEXT NOT NULL, discount_centavos INTEGER NOT NULL DEFAULT 0,"
                    + " tendered_centavos INTEGER NOT NULL DEFAULT 0, change_centavos INTEGER NOT NULL DEFAULT 0,"
                    + " handled_by TEXT)");
        }
        Database db = Database.open("jdbc:sqlite:" + file);
        assertEquals("Old Customer", db.query("SELECT customer FROM job_order", rs -> rs.getString(1)).get(0));
        db.update("UPDATE job_order SET detail = 'Spiral'");
        db.update("INSERT INTO payment(id,time,customer,job_id,amount_centavos,method,status,sender)"
                + " VALUES('T1','2026-11-24T09:00','C','JOB-0007',100,'GCASH','PAID','0917')");
        assertEquals("Spiral", db.query("SELECT detail FROM job_order", rs -> rs.getString(1)).get(0));
        assertEquals(2, db.query("SELECT version FROM schema_version", rs -> rs.getInt(1)).get(0));
        db.close();
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
