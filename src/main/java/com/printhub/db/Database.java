package com.printhub.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * The SQLite connection and schema. One connection is shared by every service; services take
 * {@link #lock()} around each public method so Swing and web threads never interleave.
 */
public final class Database {

    /** Maps one row of a result set to an object. */
    @FunctionalInterface
    public interface RowMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }

    /** Unchecked wrapper for SQL errors. */
    public static class DataAccessException extends RuntimeException {
        public DataAccessException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private static final int SCHEMA_VERSION = 1;

    private final Connection connection;
    private final Object lock = new Object();

    private Database(Connection connection) {
        this.connection = connection;
        createSchema();
    }

    /** Opens (or creates) the database at a JDBC url such as jdbc:sqlite:printhub.db. */
    public static Database open(String jdbcUrl) {
        try {
            Connection c = DriverManager.getConnection(jdbcUrl);
            try (Statement st = c.createStatement()) {
                st.execute("PRAGMA foreign_keys = ON");
            }
            return new Database(c);
        } catch (SQLException e) {
            throw new DataAccessException("Cannot open database: " + e.getMessage(), e);
        }
    }

    /** A throwaway database that lives only as long as the object. Used by tests. */
    public static Database inMemory() {
        return open("jdbc:sqlite::memory:");
    }

    public Connection conn() {
        return connection;
    }

    public Object lock() {
        return lock;
    }

    public void close() {
        try {
            connection.close();
        } catch (SQLException ignored) {
            // nothing useful to do on close
        }
    }

    /** True until the first account exists, so the app knows to load the sample data. */
    public boolean isEmpty() {
        return query("SELECT COUNT(*) FROM account", rs -> rs.getInt(1)).get(0) == 0;
    }

    /** Runs INSERT / UPDATE / DELETE; returns the number of rows changed. */
    public int update(String sql, Object... params) {
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            bind(ps, params);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Database error: " + e.getMessage(), e);
        }
    }

    public <T> List<T> query(String sql, RowMapper<T> mapper, Object... params) {
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                List<T> rows = new ArrayList<>();
                while (rs.next()) {
                    rows.add(mapper.map(rs));
                }
                return rows;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Database error: " + e.getMessage(), e);
        }
    }

    private static void bind(PreparedStatement ps, Object[] params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            ps.setObject(i + 1, params[i]);
        }
    }

    private void createSchema() {
        String[] ddl = {
            "CREATE TABLE IF NOT EXISTS schema_version (version INTEGER NOT NULL)",
            "CREATE TABLE IF NOT EXISTS setting (key TEXT PRIMARY KEY, value TEXT NOT NULL)",
            "CREATE TABLE IF NOT EXISTS account (username TEXT PRIMARY KEY COLLATE NOCASE, password_hash TEXT NOT NULL,"
                + " full_name TEXT NOT NULL, role TEXT NOT NULL)",
            "CREATE TABLE IF NOT EXISTS supply (id TEXT PRIMARY KEY, name TEXT NOT NULL, category TEXT NOT NULL,"
                + " size TEXT NOT NULL, unit TEXT NOT NULL, stock INTEGER NOT NULL, reorder_level INTEGER NOT NULL,"
                + " unit_cost_centavos INTEGER NOT NULL DEFAULT 0, active INTEGER NOT NULL DEFAULT 1)",
            "CREATE TABLE IF NOT EXISTS supplier (id TEXT PRIMARY KEY, name TEXT NOT NULL, address TEXT,"
                + " phone TEXT, active INTEGER NOT NULL DEFAULT 1)",
            "CREATE TABLE IF NOT EXISTS supplier_supply (supplier_id TEXT NOT NULL REFERENCES supplier(id),"
                + " supply_id TEXT NOT NULL REFERENCES supply(id), PRIMARY KEY (supplier_id, supply_id))",
            "CREATE TABLE IF NOT EXISTS restock (id INTEGER PRIMARY KEY AUTOINCREMENT, time TEXT NOT NULL,"
                + " supplier_id TEXT NOT NULL REFERENCES supplier(id), supply_id TEXT NOT NULL REFERENCES supply(id),"
                + " qty INTEGER NOT NULL, total_centavos INTEGER NOT NULL, staff TEXT)",
            "CREATE TABLE IF NOT EXISTS stock_movement (id INTEGER PRIMARY KEY AUTOINCREMENT, time TEXT NOT NULL,"
                + " supply_id TEXT NOT NULL REFERENCES supply(id), type TEXT NOT NULL, qty INTEGER NOT NULL,"
                + " reason TEXT, job_id TEXT, staff TEXT)",
            "CREATE TABLE IF NOT EXISTS job_order (number INTEGER PRIMARY KEY, customer TEXT NOT NULL,"
                + " service TEXT NOT NULL, size TEXT NOT NULL, pages INTEGER NOT NULL, copies INTEGER NOT NULL,"
                + " rush INTEGER NOT NULL, status TEXT NOT NULL, payment_status TEXT NOT NULL, source TEXT NOT NULL,"
                + " file_path TEXT, total_centavos INTEGER NOT NULL DEFAULT 0, discount_name TEXT,"
                + " created_at TEXT NOT NULL, cancelled_at TEXT)",
            "CREATE TABLE IF NOT EXISTS payment (id TEXT PRIMARY KEY, time TEXT NOT NULL, customer TEXT NOT NULL,"
                + " job_id TEXT NOT NULL, amount_centavos INTEGER NOT NULL, method TEXT NOT NULL, reference TEXT,"
                + " status TEXT NOT NULL, discount_centavos INTEGER NOT NULL DEFAULT 0,"
                + " tendered_centavos INTEGER NOT NULL DEFAULT 0, change_centavos INTEGER NOT NULL DEFAULT 0,"
                + " handled_by TEXT)",
            "CREATE TABLE IF NOT EXISTS price (service TEXT NOT NULL, size TEXT NOT NULL,"
                + " centavos INTEGER NOT NULL, PRIMARY KEY (service, size))",
            "CREATE TABLE IF NOT EXISTS discount (name TEXT PRIMARY KEY, kind TEXT NOT NULL, value REAL NOT NULL)"
        };
        try (Statement st = connection.createStatement()) {
            for (String sql : ddl) {
                st.execute(sql);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Cannot create schema: " + e.getMessage(), e);
        }
        if (query("SELECT COUNT(*) FROM schema_version", rs -> rs.getInt(1)).get(0) == 0) {
            update("INSERT INTO schema_version(version) VALUES (?)", SCHEMA_VERSION);
        }
    }
}
