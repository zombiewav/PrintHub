package com.printhub.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.printhub.data.PrintHubData;
import java.nio.file.Files;
import java.util.List;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Builds every screen against a fresh sample database and refreshes it, so wiring mistakes show up without clicking. */
class UiSmokeTest {

    @BeforeAll
    static void useThrowawayDatabase() throws Exception {
        System.setProperty("user.dir", Files.createTempDirectory("printhub-ui").toString());
        PrintHubTheme.install();
    }

    private static void onSwingThread(Runnable work) throws Exception {
        SwingUtilities.invokeAndWait(work);
    }

    @Test
    void existingScreensBuildAndRefresh() throws Exception {
        onSwingThread(() -> {
            PrintHubData data = PrintHubData.get();
            assertNotNull(data.authenticate("admin", "admin123"));
            List<JPanel> screens = List.of(new DashboardPanel(key -> { }), new NewOrderPanel(), new JobQueuePanel(),
                    new InventoryPanel(), new SalesPanel(), new PriceSettingsPanel());
            for (JPanel screen : screens) {
                ((Refreshable) screen).refresh();
            }
            assertEquals(6, screens.size());
        });
    }
}
