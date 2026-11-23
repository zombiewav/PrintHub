package com.printhub.model;

/** A staff login. */
public class Account {

    private final String username;
    private final String password;
    private final String fullName;
    private final String role;

    public Account(String username, String password, String fullName, String role) {
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.role = role;
    }

    public boolean matches(String username, String password) {
        return this.username.equalsIgnoreCase(username) && this.password.equals(password);
    }

    public String getUsername() {
        return username;
    }

    public String getFullName() {
        return fullName;
    }

    public String getRole() {
        return role;
    }

    /** LM for Liza Mendoza. */
    public String initials() {
        StringBuilder sb = new StringBuilder();
        for (String part : fullName.trim().split("\\s+")) {
            if (!part.isEmpty() && sb.length() < 2) {
                sb.append(Character.toUpperCase(part.charAt(0)));
            }
        }
        return sb.toString();
    }
}
