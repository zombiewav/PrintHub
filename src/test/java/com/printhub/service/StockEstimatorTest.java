package com.printhub.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.printhub.db.Database;
import com.printhub.db.Settings;
import com.printhub.model.JobOrder;
import com.printhub.model.JobStatus;
import com.printhub.model.PaperSize;
import com.printhub.model.Service;
import com.printhub.model.StockEstimate;
import com.printhub.model.Supply;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StockEstimatorTest {

    private Database db;
    private Settings settings;
    private InventoryService inv;
    private StockEstimator estimator;
    private Supply shortPaper;
    private Supply longPaper;
    private Supply black;
    private Supply color;
    private Supply photoA4;
    private Supply photo4R;
    private Supply filmShort;

    @BeforeEach
    void setUp() {
        db = Database.inMemory();
        settings = new Settings(db);
        inv = new InventoryService(db);
        estimator = new StockEstimator(inv, settings);
        shortPaper = inv.add(new Supply("SUP-002", "Bond Paper 70gsm", "Paper", "Short", "sheets", 1000, 500));
        longPaper = inv.add(new Supply("SUP-003", "Bond Paper 70gsm", "Paper", "Long", "sheets", 180, 300));
        black = inv.add(new Supply("SUP-004", "Black Ink (Epson 003)", "Ink", Supply.NO_SIZE, "pages", 2400, 1000));
        color = inv.add(new Supply("SUP-005", "Color Ink Set (Epson 003)", "Ink", Supply.NO_SIZE, "pages", 420, 500));
        photoA4 = inv.add(new Supply("SUP-008", "Glossy Photo Paper 180gsm", "Photo Paper", "A4", "sheets", 90, 50));
        photo4R = inv.add(new Supply("SUP-009", "Glossy Photo Paper 180gsm", "Photo Paper", "4R", "sheets", 140, 100));
        filmShort = inv.add(new Supply("SUP-010", "Lamination Film Pouch", "Lamination Film", "Short", "pcs", 100, 100));
    }

    private JobOrder job(int no, Service service, PaperSize size, int pages, int copies, JobStatus status) {
        JobOrder j = new JobOrder(no, "C", service, size, pages, copies, false, LocalTime.of(9, 0));
        j.setStatus(status);
        return j;
    }

    private StockEstimate.Line line(List<StockEstimate.Line> lines, String label) {
        return lines.stream().filter(l -> l.getLabel().equals(label)).findFirst().get();
    }

    @Test
    void fiftyCopiesOfThreePagesNeeds150Sheets() {
        StockEstimate est = estimator.estimate(Service.BW, PaperSize.SHORT, 3 * 50, List.of());
        StockEstimate.Line paper = line(est.getPaper(), "Short");
        assertTrue(paper.isUsed());
        assertEquals(150, paper.getRequired());
        assertEquals(1000, paper.getAvailable());
        assertEquals(850, paper.getRemainingAfter());
        assertTrue(est.isSufficient());
        assertFalse(line(est.getPaper(), "Long").isUsed());
    }

    @Test
    void twoJobsThatFitAloneButNotTogetherAreInsufficient() {
        JobOrder active = job(1, Service.BW, PaperSize.LONG, 10, 10, JobStatus.PENDING); // needs 100 Long sheets
        StockEstimate est = estimator.estimate(Service.BW, PaperSize.LONG, 100, List.of(active));
        StockEstimate.Line paper = line(est.getPaper(), "Long");
        assertEquals(80, paper.getAvailable());
        assertEquals(20, paper.shortBy());
        assertFalse(est.isSufficient());
        assertTrue(estimator.estimate(Service.BW, PaperSize.LONG, 100, List.of()).isSufficient());
    }

    @Test
    void processingJobsReserveButCompletedAndCancelledDoNot() {
        List<JobOrder> jobs = List.of(
                job(1, Service.BW, PaperSize.LONG, 10, 10, JobStatus.PROCESSING),
                job(2, Service.BW, PaperSize.LONG, 10, 10, JobStatus.COMPLETED),
                job(3, Service.BW, PaperSize.LONG, 10, 10, JobStatus.CANCELLED));
        StockEstimate est = estimator.estimate(Service.BW, PaperSize.LONG, 1, jobs);
        assertEquals(80, line(est.getPaper(), "Long").getAvailable());
    }

    @Test
    void inkIsCountedPerServiceAndScalesWithInkPerPageSetting() {
        Map<Supply, Integer> bw = estimator.needs(Service.BW, PaperSize.SHORT, 10);
        assertEquals(10, bw.get(shortPaper));
        assertEquals(10, bw.get(black));
        assertNull(bw.get(color));
        settings.set("inkPerPage", "2");
        assertEquals(20, estimator.needs(Service.BW, PaperSize.SHORT, 10).get(black));
        assertEquals(20, estimator.needs(Service.COLORED, PaperSize.SHORT, 10).get(color));
    }

    @Test
    void laminationUsesFilmOfTheSameSizeAndNoPaperOrInk() {
        Map<Supply, Integer> needs = estimator.needs(Service.LAMINATION, PaperSize.SHORT, 5);
        assertEquals(1, needs.size());
        assertEquals(5, needs.get(filmShort));
        StockEstimate est = estimator.estimate(Service.LAMINATION, PaperSize.SHORT, 5, List.of());
        assertTrue(line(est.getOther(), "Lamination film").isUsed());
        assertEquals(5, line(est.getOther(), "Lamination film").getRequired());
        assertEquals(95, line(est.getOther(), "Lamination film").getRemainingAfter());
        assertFalse(line(est.getPaper(), "Short").isUsed());
        assertFalse(line(est.getInk(), "Black ink").isUsed());
        assertFalse(line(est.getInk(), "Color ink").isUsed());
        assertFalse(line(est.getOther(), "Photo paper").isUsed());
    }

    @Test
    void idPictureUsesPhotoPaperAndColorInkButNoBondPaper() {
        Map<Supply, Integer> needs = estimator.needs(Service.ID_PICTURE, PaperSize.A4, 3);
        assertEquals(2, needs.size());
        assertEquals(3, needs.get(photoA4));
        assertEquals(3, needs.get(color));
        StockEstimate est = estimator.estimate(Service.ID_PICTURE, PaperSize.A4, 3, List.of());
        assertTrue(line(est.getOther(), "Photo paper").isUsed());
        assertEquals(87, line(est.getOther(), "Photo paper").getRemainingAfter());
        assertFalse(line(est.getPaper(), "Short").isUsed());
    }

    @Test
    void idPictureFallsBackTo4RPhotoPaperWhenNoPhotoPaperInThatSize() {
        Map<Supply, Integer> needs = estimator.needs(Service.ID_PICTURE, PaperSize.SHORT, 2);
        assertEquals(2, needs.get(photo4R));
        assertNull(needs.get(photoA4));
    }

    @Test
    void scanningAndBindingUseNoMaterialsSoTheyAlwaysFit() {
        for (Service service : new Service[] {Service.SCANNING, Service.BINDING}) {
            assertTrue(estimator.needs(service, PaperSize.A4, 1_000_000).isEmpty(), service.label());
            StockEstimate est = estimator.estimate(service, PaperSize.A4, 1_000_000, List.of());
            assertTrue(est.isSufficient(), service.label());
        }
    }

    @Test
    void photocopyingUsesPaperAndBlackInk() {
        Map<Supply, Integer> needs = estimator.needs(Service.PHOTOCOPY, PaperSize.SHORT, 10);
        assertEquals(10, needs.get(shortPaper));
        assertEquals(10, needs.get(black));
    }

    @Test
    void filmClaimedByQueuedLaminationJobsIsNotAvailableAgain() {
        JobOrder active = job(1, Service.LAMINATION, PaperSize.SHORT, 90, 1, JobStatus.PENDING);
        StockEstimate est = estimator.estimate(Service.LAMINATION, PaperSize.SHORT, 20, List.of(active));
        StockEstimate.Line film = line(est.getOther(), "Lamination film");
        assertEquals(10, film.getAvailable());
        assertEquals(10, film.shortBy());
        assertFalse(est.isSufficient());
    }

    @Test
    void missingSupplyCountsAsNothingAvailable() {
        Database empty = Database.inMemory();
        InventoryService noStock = new InventoryService(empty);
        StockEstimate est = new StockEstimator(noStock, new Settings(empty))
                .estimate(Service.COLORED, PaperSize.A4, 10, List.of());
        assertFalse(est.isSufficient());
        assertEquals(0, line(est.getInk(), "Color ink").getAvailable());
    }

    @Test
    void estimateListsEveryPaperSizeAndBothInks() {
        StockEstimate est = estimator.estimate(Service.BW, PaperSize.SHORT, 1, List.of());
        assertEquals(PaperSize.values().length, est.getPaper().size());
        assertEquals(2, est.getInk().size());
    }
}
