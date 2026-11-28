package com.printhub.service;

import com.printhub.db.Database;
import com.printhub.model.Account;
import com.printhub.model.Permission;
import com.printhub.model.Role;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Staff and admin accounts: sign in, sign out, add, change roles and passwords. */
public class AccountService {

    private static final int MIN_PASSWORD = 6;

    private final Database db;
    /** Accounts by lower-case username. */
    private final Map<String, Account> byName = new LinkedHashMap<>();

    public AccountService(Database db) {
        this.db = db;
        for (Account a : db.query("SELECT username, password_hash, full_name, role FROM account ORDER BY rowid",
                rs -> new Account(rs.getString(1), rs.getString(2), rs.getString(3), Role.valueOf(rs.getString(4))))) {
            byName.put(key(a.getUsername()), a);
        }
    }

    private static String key(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }

    /** Returns the account and signs it in, or null when the username or password is wrong. */
    public Account authenticate(String username, String password) {
        synchronized (db.lock()) {
            Account a = byName.get(key(username));
            if (a != null && PasswordHasher.verify(password, a.getPasswordHash())) {
                Session.set(a);
                return a;
            }
            return null;
        }
    }

    public void logout() {
        Session.clear();
    }

    public List<Account> all() {
        synchronized (db.lock()) {
            return new ArrayList<>(byName.values());
        }
    }

    public Account add(String username, String password, String fullName, Role role) {
        Session.require(Permission.MANAGE_ACCOUNTS);
        synchronized (db.lock()) {
            String name = username == null ? "" : username.trim();
            if (name.isEmpty()) {
                throw new IllegalArgumentException("Enter a username.");
            }
            if (fullName == null || fullName.trim().isEmpty()) {
                throw new IllegalArgumentException("Enter the full name.");
            }
            if (byName.containsKey(key(name))) {
                throw new IllegalArgumentException("That username is already taken.");
            }
            checkPassword(password);
            Account a = new Account(name, PasswordHasher.hash(password), fullName.trim(), role);
            db.update("INSERT INTO account(username, password_hash, full_name, role) VALUES (?,?,?,?)",
                    a.getUsername(), a.getPasswordHash(), a.getFullName(), role.name());
            byName.put(key(name), a);
            return a;
        }
    }

    /** A signed-in user changing their own password. */
    public void changePassword(String username, String oldPassword, String newPassword) {
        synchronized (db.lock()) {
            Account me = Session.current();
            if (me == null || !key(me.getUsername()).equals(key(username))) {
                throw new SecurityException("You can only change your own password.");
            }
            if (!PasswordHasher.verify(oldPassword, me.getPasswordHash())) {
                throw new IllegalArgumentException("The current password is wrong.");
            }
            checkPassword(newPassword);
            store(me, PasswordHasher.hash(newPassword));
        }
    }

    /** An admin setting someone's password without knowing the old one. */
    public void resetPassword(String username, String newPassword) {
        Session.require(Permission.MANAGE_ACCOUNTS);
        synchronized (db.lock()) {
            checkPassword(newPassword);
            store(require(username), PasswordHasher.hash(newPassword));
        }
    }

    public void setRole(String username, Role role) {
        Session.require(Permission.MANAGE_ACCOUNTS);
        synchronized (db.lock()) {
            Account a = require(username);
            if (a.getRoleType() == Role.ADMIN && role != Role.ADMIN && adminCount() <= 1) {
                throw new IllegalStateException("There must be at least one admin.");
            }
            db.update("UPDATE account SET role = ? WHERE username = ?", role.name(), a.getUsername());
            a.setRoleType(role);
        }
    }

    public void delete(String username) {
        Session.require(Permission.MANAGE_ACCOUNTS);
        synchronized (db.lock()) {
            Account a = require(username);
            if (a.getRoleType() == Role.ADMIN && adminCount() <= 1) {
                throw new IllegalStateException("There must be at least one admin.");
            }
            db.update("DELETE FROM account WHERE username = ?", a.getUsername());
            byName.remove(key(username));
        }
    }

    private Account require(String username) {
        Account a = byName.get(key(username));
        if (a == null) {
            throw new IllegalArgumentException("No account named " + username + ".");
        }
        return a;
    }

    private int adminCount() {
        int n = 0;
        for (Account a : byName.values()) {
            if (a.getRoleType() == Role.ADMIN) {
                n++;
            }
        }
        return n;
    }

    private void store(Account a, String hash) {
        db.update("UPDATE account SET password_hash = ? WHERE username = ?", hash, a.getUsername());
        a.setPasswordHash(hash);
    }

    /** Throws when a password is too short to use. */
    public static void checkPassword(String password) {
        if (password == null || password.length() < MIN_PASSWORD) {
            throw new IllegalArgumentException("Password must be at least " + MIN_PASSWORD + " characters.");
        }
    }
}
