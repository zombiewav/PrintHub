package com.printhub.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.printhub.db.Database;
import com.printhub.model.Account;
import com.printhub.model.Role;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AccountServiceTest {

    private Database db;
    private AccountService accounts;

    @BeforeEach
    void setUp() {
        db = Database.inMemory();
        db.update("INSERT INTO account(username,password_hash,full_name,role) VALUES(?,?,?,?)",
                "admin", PasswordHasher.hash("admin123"), "Shop Admin", "ADMIN");
        db.update("INSERT INTO account(username,password_hash,full_name,role) VALUES(?,?,?,?)",
                "lmendoza", PasswordHasher.hash("printhub"), "Liza Mendoza", "STAFF");
        accounts = new AccountService(db);
    }

    @AfterEach
    void tearDown() {
        Session.clear();
    }

    private void loginAsAdmin() {
        assertNotNull(accounts.authenticate("admin", "admin123"));
    }

    @Test
    void hashIsSaltedAndVerifies() {
        String a = PasswordHasher.hash("printhub");
        String b = PasswordHasher.hash("printhub");
        assertNotEquals(a, b);
        assertTrue(PasswordHasher.verify("printhub", a));
        assertTrue(PasswordHasher.verify("printhub", b));
        assertFalse(PasswordHasher.verify("wrong", a));
    }

    @Test
    void authenticateSetsSession() {
        Account a = accounts.authenticate("lmendoza", "printhub");
        assertNotNull(a);
        assertEquals("Liza Mendoza", a.getFullName());
        assertEquals(Role.STAFF, a.getRoleType());
        assertEquals("lmendoza", Session.username());
        accounts.logout();
        assertEquals("system", Session.username());
    }

    @Test
    void wrongPasswordOrUnknownUserReturnsNull() {
        assertNull(accounts.authenticate("lmendoza", "nope"));
        assertNull(accounts.authenticate("ghost", "printhub"));
        assertEquals("system", Session.username());
    }

    @Test
    void usernameMatchIsCaseInsensitive() {
        assertNotNull(accounts.authenticate("LMendoza", "printhub"));
    }

    @Test
    void changePasswordRequiresOldPassword() {
        accounts.authenticate("lmendoza", "printhub");
        assertThrows(IllegalArgumentException.class, () -> accounts.changePassword("lmendoza", "wrong", "newpass1"));
        accounts.changePassword("lmendoza", "printhub", "newpass1");
        assertNull(accounts.authenticate("lmendoza", "printhub"));
        assertNotNull(accounts.authenticate("lmendoza", "newpass1"));
    }

    @Test
    void newPasswordMustBeAtLeastSixCharacters() {
        accounts.authenticate("lmendoza", "printhub");
        assertThrows(IllegalArgumentException.class, () -> accounts.changePassword("lmendoza", "printhub", "abc"));
    }

    @Test
    void staffCannotAddAccount() {
        accounts.authenticate("lmendoza", "printhub");
        assertThrows(SecurityException.class, () -> accounts.add("newbie", "secret1", "New Bie", Role.STAFF));
    }

    @Test
    void adminAddsAccountThatCanLogIn() {
        loginAsAdmin();
        accounts.add("newbie", "secret1", "New Bie", Role.STAFF);
        assertEquals(3, accounts.all().size());
        assertNotNull(accounts.authenticate("newbie", "secret1"));
    }

    @Test
    void duplicateUsernameRejected() {
        loginAsAdmin();
        assertThrows(IllegalArgumentException.class, () -> accounts.add("LMENDOZA", "secret1", "Dup", Role.STAFF));
    }

    @Test
    void adminCanResetAnotherPassword() {
        loginAsAdmin();
        accounts.resetPassword("lmendoza", "reset123");
        assertNotNull(accounts.authenticate("lmendoza", "reset123"));
    }

    @Test
    void cannotDeleteOrDemoteLastAdmin() {
        loginAsAdmin();
        assertThrows(IllegalStateException.class, () -> accounts.delete("admin"));
        assertThrows(IllegalStateException.class, () -> accounts.setRole("admin", Role.STAFF));
        accounts.setRole("lmendoza", Role.ADMIN);
        accounts.setRole("admin", Role.STAFF);
        assertEquals(Role.STAFF, accounts.all().stream().filter(a -> a.getUsername().equals("admin"))
                .findFirst().get().getRoleType());
    }

    @Test
    void accountsSurviveRestart() {
        loginAsAdmin();
        accounts.add("newbie", "secret1", "New Bie", Role.STAFF);
        AccountService reopened = new AccountService(db);
        assertNotNull(reopened.authenticate("newbie", "secret1"));
    }
}
