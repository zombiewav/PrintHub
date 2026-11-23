package com.printhub.model;

/** A print service the shop sells. Each one is priced per paper size and uses different ink. */
public enum Service {
    BW("B&W Printing", "per page", true, false),
    COLORED("Colored Printing", "per page", false, true),
    LAMINATION("Lamination", "per piece", false, false),
    ID_PICTURE("ID Picture", "per sheet", false, true),
    PHOTOCOPY("Photocopying", "per page", true, false),
    SCANNING("Scanning", "per page", false, false),
    BINDING("Document Binding", "per piece", false, false);

    private final String label;
    private final String charged;
    private final boolean blackInk;
    private final boolean colorInk;

    Service(String label, String charged, boolean blackInk, boolean colorInk) {
        this.label = label;
        this.charged = charged;
        this.blackInk = blackInk;
        this.colorInk = colorInk;
    }

    public String label() {
        return label;
    }

    /** How the unit price is charged: per page, per piece, per sheet. */
    public String charged() {
        return charged;
    }

    public boolean usesBlackInk() {
        return blackInk;
    }

    public boolean usesColorInk() {
        return colorInk;
    }

    @Override
    public String toString() {
        return label;
    }
}
