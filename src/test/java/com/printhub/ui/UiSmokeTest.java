package com.printhub.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.printhub.data.PrintHubData;
import java.awt.GraphicsEnvironment;
import java.nio.file.Files;
import java.util.List;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Assumptions;
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
    void mainWindowBuildsForAdminAndForStaff() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display to build a JFrame");
        onSwingThread(() -> {
            PrintHubData data = PrintHubData.get();
            for (String[] login : new String[][] {{"admin", "admin123"}, {"lmendoza", "printhub"}}) {
                MainFrame frame = new MainFrame(data.authenticate(login[0], login[1]), () -> { });
                frame.show("suppliers");
                frame.dispose();
            }
        });
    }

    @Test
    void portalDialogBuildsWhileStoppedAndWhileRunning() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display to build a dialog");
        onSwingThread(() -> {
            PrintHubData data = PrintHubData.get();
            new PortalDialog(null).dispose();
            try {
                data.startPortal(0);
                new PortalDialog(null).dispose();
            } catch (java.io.IOException e) {
                throw new AssertionError(e);
            } finally {
                data.stopPortal();
            }
        });
    }

    @Test
    void existingScreensBuildAndRefresh() throws Exception {
        onSwingThread(() -> {
            PrintHubData data = PrintHubData.get();
            assertNotNull(data.authenticate("admin", "admin123"));
            List<JPanel> screens = List.of(new DashboardPanel(key -> { }), new NewOrderPanel(), new JobQueuePanel(),
                    new InventoryPanel(), new SalesPanel(), new PriceSettingsPanel(), new SuppliersPanel(), new ReportsPanel(), new AccountsPanel());
            for (JPanel screen : screens) {
                ((Refreshable) screen).refresh();
            }
            assertEquals(9, screens.size());
        });
    }
}
