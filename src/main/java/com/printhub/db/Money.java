package com.printhub.db;

/** Pesos in the Java API, whole centavos in the database. */
public final class Money {

    private Money() {
    }

    public static long toCentavos(double pesos) {
        return Math.round(pesos * 100);
    }

    public static double fromCentavos(long centavos) {
        return centavos / 100.0;
    }
}
