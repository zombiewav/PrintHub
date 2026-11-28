package com.printhub.data;

import com.printhub.service.AccountService;

/** What the first-run screen asks a new shop: its name and the owner's admin account. */
public record ShopSetup(String shopName, String ownerName, String username, String password) {

    static final int MAX_SHOP_NAME = 40;

    /** Throws IllegalArgumentException with a message for the screen when something is missing or invalid. */
    public void check() {
        if (blank(shopName)) {
            throw new IllegalArgumentException("Enter the shop's name.");
        }
        if (shopName.trim().length() > MAX_SHOP_NAME) {
            throw new IllegalArgumentException("Keep the shop's name to " + MAX_SHOP_NAME + " characters.");
        }
        if (blank(ownerName)) {
            throw new IllegalArgumentException("Enter the owner's full name.");
        }
        if (blank(username)) {
            throw new IllegalArgumentException("Enter a username for signing in.");
        }
        if (!username.trim().matches("[A-Za-z0-9._-]{3,30}")) {
            throw new IllegalArgumentException("The username must be 3 to 30 letters or numbers (. _ - allowed), "
                    + "with no spaces.");
        }
        AccountService.checkPassword(password);
    }

    private static boolean blank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
