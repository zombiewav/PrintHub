package com.printhub.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.printhub.db.Database;
import com.printhub.model.Account;
import com.printhub.model.Discount;
import com.printhub.model.PaperSize;
import com.printhub.model.Role;
import com.printhub.model.Service;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PricingServiceTest {

    private Database db;
    private PricingService pricing;

    @BeforeEach
    void setUp() {
        db = Database.inMemory();
        pricing = new PricingService(db);
        Session.set(new Account("admin", "x", "Admin", Role.ADMIN));
        pricing.prices().set(Service.BW, PaperSize.A4, 2);
        pricing.prices().set(Service.BW, PaperSize.LONG, 3);
        pricing.prices().set(Service.COLORED, PaperSize.A4, 10);
        pricing.save();
    }

    @AfterEach
    void tearDown() {
        Session.clear();
    }

    @Test
    void rushFeeDefaultsToTwentyPesos() {
        assertEquals(20, pricing.prices().getRushFee(), 0.0001);
    }

    @Test
    void totalMatchesExistingExample() {
        assertEquals(620, pricing.total(Service.COLORED, PaperSize.A4, 12, 5, true, null), 0.0001);
    }

    @Test
    void paperSizeChangesPrice() {
        assertEquals(3, pricing.total(Service.BW, PaperSize.LONG, 1, 1, false, null), 0.0001);
        assertEquals(2, pricing.total(Service.BW, PaperSize.A4, 1, 1, false, null), 0.0001);
    }

    @Test
    void percentDiscountAppliesBeforeRushFee() {
        Discount student = new Discount("Student", Discount.Kind.PERCENT, 10);
        assertEquals(20 - 2 + 20, pricing.total(Service.BW, PaperSize.A4, 10, 1, true, student), 0.0001);
    }

    @Test
    void discountLargerThanSubtotalLeavesOnlyRushFee() {
        Discount big = new Discount("Big", Discount.Kind.FIXED, 500);
        assertEquals(20, pricing.total(Service.BW, PaperSize.A4, 10, 1, true, big), 0.0001);
        assertEquals(0, pricing.total(Service.BW, PaperSize.A4, 10, 1, false, big), 0.0001);
    }

    @Test
    void savePersistsEditedPricesAndRushFeeAcrossReload() {
        pricing.prices().set(Service.BW, PaperSize.A4, 2.5);
        pricing.prices().setRushFee(35);
        pricing.save();
        PricingService reloaded = new PricingService(db);
        assertEquals(2.5, reloaded.prices().get(Service.BW, PaperSize.A4), 0.0001);
        assertEquals(35, reloaded.prices().getRushFee(), 0.0001);
        assertEquals(10, reloaded.prices().get(Service.COLORED, PaperSize.A4), 0.0001);
    }

    @Test
    void invalidQuantitiesRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> pricing.total(Service.BW, PaperSize.A4, 5, 0, false, null));
        assertThrows(IllegalArgumentException.class,
                () -> pricing.total(Service.BW, PaperSize.A4, 0, 5, false, null));
        assertThrows(IllegalArgumentException.class,
                () -> pricing.total(Service.BW, PaperSize.A4, 100000, 100000, false, null));
    }

    @Test
    void staffCannotSavePrices() {
        Session.set(new Account("lm", "x", "Liza", Role.STAFF));
        assertThrows(SecurityException.class, () -> pricing.save());
    }

    @Test
    void discountsPersistAndOnlyAdminDeletes() {
        pricing.addDiscount(new Discount("Student", Discount.Kind.PERCENT, 10));
        pricing.addDiscount(new Discount("Promo", Discount.Kind.FIXED, 5));
        PricingService reloaded = new PricingService(db);
        assertEquals(2, reloaded.discounts().size());
        assertEquals(Discount.Kind.PERCENT, reloaded.discounts().get(0).getKind());
        Session.set(new Account("lm", "x", "Liza", Role.STAFF));
        assertThrows(SecurityException.class, () -> reloaded.deleteDiscount("Promo"));
        Session.set(new Account("admin", "x", "Admin", Role.ADMIN));
        reloaded.deleteDiscount("Promo");
        assertEquals(1, reloaded.discounts().size());
        assertTrue(new PricingService(db).discounts().stream().noneMatch(d -> d.getName().equals("Promo")));
    }
}
