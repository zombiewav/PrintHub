package com.printhub.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.printhub.db.Database;
import com.printhub.model.Account;
import com.printhub.model.MovementType;
import com.printhub.model.PaperSize;
import com.printhub.model.Role;
import com.printhub.model.StockMovement;
import com.printhub.model.Supply;
import com.printhub.model.WasteReason;
import com.printhub.model.WasteRecord;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InventoryServiceTest {

    private Database db;
    private InventoryService inv;
    private Supply shortPaper;

    @BeforeEach
    void setUp() {
        db = Database.inMemory();
        inv = new InventoryService(db);
        Session.set(new Account("admin", "x", "Admin", Role.ADMIN));
        shortPaper = inv.add(new Supply("SUP-002", "Bond Paper 70gsm", "Paper", "Short", "sheets", 100, 50));
    }

    @AfterEach
    void tearDown() {
        Session.clear();
    }

    @Test
    void restockAddsStockAndWritesInMovement() {
        inv.restock(shortPaper, 50);
        assertEquals(150, shortPaper.getStock());
        List<StockMovement> history = inv.movements(shortPaper);
        assertEquals(1, history.size());
        assertEquals(MovementType.IN, history.get(0).getType());
        assertEquals(50, history.get(0).getQty());
        assertEquals("admin", history.get(0).getStaff());
    }

    @Test
    void deductWritesOutMovementWithJobId() {
        inv.deduct(shortPaper, 30, "JOB-0001");
        assertEquals(70, shortPaper.getStock());
        StockMovement m = inv.movements(shortPaper).get(0);
        assertEquals(MovementType.OUT, m.getType());
        assertEquals("JOB-0001", m.getJobId());
    }

    @Test
    void deductMoreThanStockRejectedAndNothingChanges() {
        assertThrows(IllegalArgumentException.class, () -> inv.deduct(shortPaper, 101, "JOB-0001"));
        assertEquals(100, shortPaper.getStock());
        assertTrue(inv.movements(shortPaper).isEmpty());
    }

    @Test
    void wasteOfFiveSheetsDeductsAndLogsPaperJam() {
        inv.recordWaste(shortPaper, 5, WasteReason.PAPER_JAM, "JOB-0230");
        assertEquals(95, shortPaper.getStock());
        List<WasteRecord> log = inv.wasteLog();
        assertEquals(1, log.size());
        assertEquals(WasteReason.PAPER_JAM, log.get(0).getReason());
        assertEquals(5, log.get(0).getQty());
        assertEquals("JOB-0230", log.get(0).getJobId());
        assertEquals(MovementType.WASTE, inv.movements(shortPaper).get(0).getType());
    }

    @Test
    void damagedReasonIsLoggedAsDamagedMovement() {
        inv.recordWaste(shortPaper, 2, WasteReason.DAMAGED, null);
        assertEquals(MovementType.DAMAGED, inv.movements(shortPaper).get(0).getType());
        assertNull(inv.wasteLog().get(0).getJobId());
    }

    @Test
    void wasteLargerThanStockRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> inv.recordWaste(shortPaper, 101, WasteReason.MISPRINT, null));
        assertEquals(100, shortPaper.getStock());
        assertTrue(inv.movements(shortPaper).isEmpty());
        assertTrue(inv.wasteLog().isEmpty());
    }

    @Test
    void lowStockIncludesAtOrBelowReorder() {
        inv.add(new Supply("SUP-004", "Black Ink", "Ink", Supply.NO_SIZE, "pages", 8, 5));
        Supply longPaper = inv.add(new Supply("SUP-003", "Bond Paper 70gsm", "Paper", "Long", "sheets", 10, 20));
        inv.deduct(shortPaper, 50, "JOB-0002"); // 50 left, reorder level 50 -> low
        List<Supply> low = inv.lowStock();
        assertTrue(low.contains(shortPaper));
        assertTrue(low.contains(longPaper));
        assertEquals(2, low.size());
    }

    @Test
    void softDeleteHidesSupplyButKeepsHistory() {
        inv.restock(shortPaper, 10);
        inv.delete("SUP-002");
        assertNull(inv.find("SUP-002"));
        assertTrue(inv.supplies().isEmpty());
        assertEquals(1, inv.movements(null).size());
        assertEquals("SUP-003", inv.newId());
    }

    @Test
    void staffCannotDelete() {
        Session.set(new Account("lm", "x", "Liza", Role.STAFF));
        assertThrows(SecurityException.class, () -> inv.delete("SUP-002"));
        assertNotNull(inv.find("SUP-002"));
    }

    @Test
    void valuationSumsStockTimesUnitCost() {
        shortPaper.setUnitCost(0.5);
        inv.update(shortPaper);
        Supply ink = inv.add(new Supply("SUP-004", "Black Ink", "Ink", Supply.NO_SIZE, "pages", 10, 5));
        ink.setUnitCost(2);
        inv.update(ink);
        assertEquals(100 * 0.5 + 10 * 2, inv.valuation(), 0.0001);
    }

    @Test
    void returnedSupplyKeepsIdentity() {
        assertSame(inv.find("SUP-002"), inv.find("SUP-002"));
        assertSame(shortPaper, inv.supplies().get(0));
    }

    @Test
    void updateWritesAdjustMovementWhenStockChanged() {
        shortPaper.setName("Bond Paper 80gsm");
        shortPaper.setStock(120);
        inv.update(shortPaper);
        StockMovement m = inv.movements(shortPaper).get(0);
        assertEquals(MovementType.ADJUST, m.getType());
        assertEquals(20, m.getQty());
        Supply reloaded = new InventoryService(db).find("SUP-002");
        assertEquals("Bond Paper 80gsm", reloaded.getName());
        assertEquals(120, reloaded.getStock());
    }

    @Test
    void updateWithoutStockChangeWritesNoMovement() {
        shortPaper.setReorderLevel(80);
        inv.update(shortPaper);
        assertTrue(inv.movements(shortPaper).isEmpty());
    }

    @Test
    void findPaperAndInkLookUpBySizeAndColor() {
        Supply black = inv.add(new Supply("SUP-004", "Black Ink (Epson 003)", "Ink", Supply.NO_SIZE, "pages", 10, 5));
        Supply color = inv.add(new Supply("SUP-005", "Color Ink Set (Epson 003)", "Ink", Supply.NO_SIZE, "pages", 10, 5));
        assertSame(shortPaper, inv.findPaper(PaperSize.SHORT));
        assertNull(inv.findPaper(PaperSize.LONG));
        assertSame(black, inv.findInk(false));
        assertSame(color, inv.findInk(true));
    }

    @Test
    void findFilmAndPhotoPaperLookUpBySizeWithA4RFallbackForPhotoPaper() {
        Supply film = inv.add(new Supply("SUP-006", "Lamination Film Pouch", "Lamination Film", "A4", "pcs", 60, 100));
        Supply photoA4 = inv.add(new Supply("SUP-008", "Glossy Photo Paper", "Photo Paper", "A4", "sheets", 90, 50));
        Supply photo4R = inv.add(new Supply("SUP-009", "Glossy Photo Paper", "Photo Paper", "4R", "sheets", 140, 100));
        assertSame(film, inv.findFilm(PaperSize.A4));
        assertNull(inv.findFilm(PaperSize.LONG));
        assertSame(photoA4, inv.findPhotoPaper(PaperSize.A4));
        assertSame(photo4R, inv.findPhotoPaper(PaperSize.LONG));
    }

    @Test
    void newIdContinuesAfterHighest() {
        assertEquals("SUP-003", inv.newId());
    }

    @Test
    void duplicateIdAndBlankNameRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> inv.add(new Supply("SUP-002", "Dup", "Paper", "A4", "sheets", 1, 1)));
        assertThrows(IllegalArgumentException.class,
                () -> inv.add(new Supply("SUP-009", " ", "Paper", "A4", "sheets", 1, 1)));
    }

    @Test
    void suppliesAndMovementsSurviveRestart() {
        inv.restock(shortPaper, 25);
        InventoryService reopened = new InventoryService(db);
        assertEquals(125, reopened.find("SUP-002").getStock());
        assertEquals(1, reopened.movements(null).size());
    }

    @Test
    void zeroOrNegativeQuantityRejected() {
        assertThrows(IllegalArgumentException.class, () -> inv.restock(shortPaper, 0));
        assertThrows(IllegalArgumentException.class, () -> inv.deduct(shortPaper, -1, "JOB-0001"));
        assertThrows(IllegalArgumentException.class, () -> inv.recordWaste(shortPaper, 0, WasteReason.DAMAGED, null));
    }
}
