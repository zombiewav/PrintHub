package com.printhub.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.printhub.db.Database;
import com.printhub.model.PaperSize;
import com.printhub.model.Service;
import com.printhub.model.Supply;
import com.printhub.service.Session;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ShopSetupTest {

    private static final ShopSetup GOOD = new ShopSetup("  Bicol Print Center ", "Rosa Lim", "rosa", "secret1");

    @AfterEach
    void signOut() {
        Session.clear();
    }

    @Test
    void aNewShopGetsItsOwnerStarterPricesAndEmptyShelvesButNoSampleHistory() {
        Database db = Database.inMemory();
        Seeder.starter(db, GOOD);
        PrintHubData data = new PrintHubData(db, Path.of(System.getProperty("java.io.tmpdir"), "ph-setup-test"));

        assertFalse(data.isDemoShop());
        assertEquals("Bicol Print Center", data.shopName());
        assertNotNull(data.authenticate("rosa", "secret1"));
        assertEquals(com.printhub.model.Role.ADMIN, data.currentUser().getRoleType());
        assertNull(data.authenticate("admin", "admin123"), "no demo accounts in a real shop");
        assertNull(data.authenticate("lmendoza", "printhub"), "no demo accounts in a real shop");
        assertEquals(1, data.accounts().all().size());

        assertEquals(2.0, data.prices().get(Service.BW, PaperSize.A4));
        assertEquals(20.0, data.prices().getRushFee());
        assertEquals(10, data.supplies().size());
        for (Supply s : data.supplies()) {
            assertEquals(0, s.getStock(), s.getName());
        }
        assertEquals(2, data.discounts().size());

        assertTrue(data.allJobs().isEmpty());
        assertTrue(data.transactions().isEmpty());
        assertTrue(data.supplierList().isEmpty());
    }

    @Test
    void theDemoShopIsMarkedAsADemo() {
        PrintHubData data = new PrintHubData(Database.inMemory());
        assertTrue(data.isDemoShop());
        assertEquals("PrintHub", data.shopName());
    }

    @Test
    void settingUpTwiceDoesNothingTheSecondTime() {
        Database db = Database.inMemory();
        Seeder.starter(db, GOOD);
        Seeder.starter(db, new ShopSetup("Other Shop", "Someone Else", "other", "secret2"));
        PrintHubData data = new PrintHubData(db, Path.of(System.getProperty("java.io.tmpdir"), "ph-setup-test"));
        assertEquals("Bicol Print Center", data.shopName());
        assertNull(data.authenticate("other", "secret2"));
    }

    @Test
    void missingOrBadAnswersAreRefusedAndNothingIsSaved() {
        Database db = Database.inMemory();
        assertThrows(IllegalArgumentException.class, () -> Seeder.starter(db, new ShopSetup(" ", "Rosa", "rosa", "secret1")));
        assertThrows(IllegalArgumentException.class, () -> Seeder.starter(db, new ShopSetup("Shop", "", "rosa", "secret1")));
        assertThrows(IllegalArgumentException.class, () -> Seeder.starter(db, new ShopSetup("Shop", "Rosa", "", "secret1")));
        assertThrows(IllegalArgumentException.class, () -> Seeder.starter(db, new ShopSetup("Shop", "Rosa", "ro sa", "secret1")));
        assertThrows(IllegalArgumentException.class, () -> Seeder.starter(db, new ShopSetup("Shop", "Rosa", "rosa", "123")));
        assertThrows(IllegalArgumentException.class,
                () -> Seeder.starter(db, new ShopSetup("x".repeat(41), "Rosa", "rosa", "secret1")));
        assertTrue(db.isEmpty());
    }

    @Test
    void dataGoesToLocalAppDataOnlyWhenRunAsThePackagedExe() {
        assertEquals(Path.of("C:/work"), AppPaths.home(null, null, "C:/Users/a/AppData/Local", "C:/Users/a", "C:/work"));
        assertEquals(Path.of("C:/Users/a/AppData/Local", "PrintHub"),
                AppPaths.home(null, "C:/PrintHub/PrintHub.exe", "C:/Users/a/AppData/Local", "C:/Users/a", "C:/PrintHub"));
        assertEquals(Path.of("C:/Users/a", "AppData", "Local", "PrintHub"),
                AppPaths.home(null, "C:/PrintHub/PrintHub.exe", null, "C:/Users/a", "C:/PrintHub"));
        assertEquals(Path.of("D:/shopdata"),
                AppPaths.home("D:/shopdata", "C:/PrintHub/PrintHub.exe", "C:/Users/a/AppData/Local", "C:/Users/a", "C:/x"));
    }
}
