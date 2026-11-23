package com.printhub.model;

/** A staff login. */
public class Account {

    private final String username;
    private String passwordHash;
    private String fullName;
    private Role roleType;

    public Account(String username, String passwordHash, String fullName, Role roleType) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.roleType = roleType;
    }

    /**
     * Temporary: the old in-memory data class builds accounts with a plain password and a role name.
     * Removed when PrintHubData moves to the database.
     */
    public Account(String username, String password, String fullName, String role) {
        this(username, password, fullName, "Admin".equalsIgnoreCase(role) ? Role.ADMIN : Role.STAFF);
    }

    /** Temporary plain-text check for the old in-memory data class. */
    public boolean matches(String username, String password) {
        return this.username.equalsIgnoreCase(username) && this.passwordHash.equals(password);
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    /** The role's label ("Admin" or "Staff"), shown in the sidebar. */
    public String getRole() {
        return roleType.label();
    }

    public Role getRoleType() {
        return roleType;
    }

    public void setRoleType(Role roleType) {
        this.roleType = roleType;
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
