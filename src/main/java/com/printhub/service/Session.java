package com.printhub.service;

import com.printhub.model.Account;
import com.printhub.model.Permission;

/** Who is signed in right now. Services use it to stamp "handled by" and to check permissions. */
public final class Session {

    private static volatile Account current;

    private Session() {
    }

    public static void set(Account account) {
        current = account;
    }

    public static Account current() {
        return current;
    }

    public static void clear() {
        current = null;
    }

    /** Username of the signed-in user, or "system" when nobody is signed in. */
    public static String username() {
        Account a = current;
        return a == null ? "system" : a.getUsername();
    }

    /** Throws SecurityException unless the signed-in user's role allows the permission. */
    public static void require(Permission permission) {
        Account a = current;
        if (a == null) {
            throw new SecurityException("Please sign in first.");
        }
        if (!a.getRoleType().allows(permission)) {
            throw new SecurityException("Admin only.");
        }
    }
}
