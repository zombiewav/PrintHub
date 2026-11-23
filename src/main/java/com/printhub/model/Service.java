package com.printhub.model;

/** A print service the shop sells. Each one is priced per paper size and uses its own materials. */
public enum Service {
    BW("B&W Printing", "per page", true, false, Material.PAPER),
    COLORED("Colored Printing", "per page", false, true, Material.PAPER),
    LAMINATION("Lamination", "per piece", false, false, Material.LAMINATION_FILM),
    ID_PICTURE("ID Picture", "per sheet", false, true, Material.PHOTO_PAPER),
    PHOTOCOPY("Photocopying", "per page", true, false, Material.PAPER),
    SCANNING("Scanning", "per page", false, false, Material.NONE),
    BINDING("Document Binding", "per piece", false, false, Material.NONE);

    /** The sheet-like material a service uses up, besides ink. */
    public enum Material {
        /** Bond paper of the order's paper size. */
        PAPER,
        /** A lamination film pouch of the order's size. */
        LAMINATION_FILM,
        /** Photo paper (the order's size, or 4R when there is none in that size). */
        PHOTO_PAPER,
        /** Nothing is taken from stock. */
        NONE
    }

    private final String label;
    private final String charged;
    private final boolean blackInk;
    private final boolean colorInk;
    private final Material material;

    Service(String label, String charged, boolean blackInk, boolean colorInk, Material material) {
        this.label = label;
        this.charged = charged;
        this.blackInk = blackInk;
        this.colorInk = colorInk;
        this.material = material;
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

    public Material material() {
        return material;
    }

    @Override
    public String toString() {
        return label;
    }
}
