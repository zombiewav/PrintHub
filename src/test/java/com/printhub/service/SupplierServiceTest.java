package com.printhub.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.printhub.db.Database;
import com.printhub.model.Account;
import com.printhub.model.MovementType;
import com.printhub.model.Restock;
import com.printhub.model.Role;
import com.printhub.model.Supplier;
import com.printhub.model.Supply;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SupplierServiceTest {

    private Database db;
    private InventoryService inv;
    private SupplierService suppliers;
    private Supply paper;
    private Supply ink;
    private Supplier prime;

    @BeforeEach
    void setUp() {
        db = Database.inMemory();
        inv = new InventoryService(db);
        suppliers = new SupplierService(db, inv);
        Session.set(new Account("admin", "x", "Admin", Role.ADMIN));
        paper = inv.add(new Supply("SUP-002", "Bond Paper 70gsm", "Paper", "Short", "sheets", 20, 50));
        ink = inv.add(new Supply("SUP-004", "Black Ink", "Ink", Supply.NO_SIZE, "pages", 8, 5));
        prime = suppliers.add("Prime Paper Trading", "Quezon Ave, Lucena", "0917 555 0101", List.of(paper));
    }

    @AfterEach
    void tearDown() {
        Session.clear();
    }

    @Test
    void restockRaisesStockRecordsPurchaseAndUnitCost() {
        Restock r = suppliers.restock(prime, paper, 100, 500);
        assertEquals(120, paper.getStock());
        assertEquals(5.0, paper.getUnitCost(), 0.0001);
        assertEquals(100, r.getQty());
        assertEquals(500, r.getTotalCost(), 0.0001);
        assertEquals(1, suppliers.history(prime).size());
        assertEquals(MovementType.IN, inv.movements(paper).get(0).getType());
    }

    @Test
    void historyAndExpensesPerSupplier() {
        Supplier inkCo = suppliers.add("Ink Depot", "Manila", "0917 555 0202", List.of(ink));
        suppliers.restock(prime, paper, 100, 500);
        suppliers.restock(prime, paper, 60, 300);
        suppliers.restock(inkCo, ink, 10, 250);
        assertEquals(800, suppliers.totalExpenses(prime), 0.0001);
        assertEquals(250, suppliers.totalExpenses(inkCo), 0.0001);
        assertEquals(1050, suppliers.totalExpenses(null), 0.0001);
        assertEquals(3, suppliers.history(null).size());
        assertEquals(60, suppliers.history(prime).get(0).getQty()); // newest first
    }

    @Test
    void suppliersForSupplyListsProviders() {
        Supplier other = suppliers.add("Paper World", "Manila", "0917 555 0303", List.of(paper, ink));
        List<Supplier> forPaper = suppliers.suppliersFor(paper);
        assertEquals(2, forPaper.size());
        assertTrue(forPaper.contains(prime) && forPaper.contains(other));
        assertEquals(List.of(other), suppliers.suppliersFor(ink));
    }

    @Test
    void cannotRestockProductTheSupplierDoesNotProvide() {
        assertThrows(IllegalArgumentException.class, () -> suppliers.restock(prime, ink, 10, 100));
        assertEquals(8, ink.getStock());
    }

    @Test
    void invalidRestockRejectedAndStockUnchanged() {
        assertThrows(IllegalArgumentException.class, () -> suppliers.restock(prime, paper, 0, 100));
        assertThrows(IllegalArgumentException.class, () -> suppliers.restock(prime, paper, 10, -1));
        assertEquals(20, paper.getStock());
        assertTrue(suppliers.history(prime).isEmpty());
    }

    @Test
    void staffCannotDeleteSupplier() {
        Session.set(new Account("lm", "x", "Liza", Role.STAFF));
        assertThrows(SecurityException.class, () -> suppliers.delete(prime.getId()));
        assertNotNull(suppliers.find(prime.getId()));
    }

    @Test
    void deletedSupplierDisappearsButPurchaseHistoryStays() {
        suppliers.restock(prime, paper, 100, 500);
        suppliers.delete(prime.getId());
        assertNull(suppliers.find(prime.getId()));
        assertTrue(suppliers.all().isEmpty());
        assertEquals(500, suppliers.totalExpenses(null), 0.0001);
        assertEquals("SPL-002", suppliers.newId());
    }

    @Test
    void updateChangesDetailsAndProducts() {
        prime.setPhone("0999 000 0000");
        prime.getSupplyIds().add(ink.getId());
        suppliers.update(prime);
        SupplierService reloaded = new SupplierService(db, inv);
        Supplier s = reloaded.find(prime.getId());
        assertEquals("0999 000 0000", s.getPhone());
        assertTrue(s.getSupplyIds().contains("SUP-004"));
    }

    @Test
    void blankNameRejected() {
        assertThrows(IllegalArgumentException.class, () -> suppliers.add(" ", "x", "y", List.of()));
    }

    @Test
    void suppliersAndPurchasesSurviveRestart() {
        suppliers.restock(prime, paper, 100, 500);
        SupplierService reloaded = new SupplierService(db, new InventoryService(db));
        assertEquals(1, reloaded.all().size());
        assertEquals(500, reloaded.totalExpenses(null), 0.0001);
        assertEquals(1, reloaded.history(null).size());
    }
}
