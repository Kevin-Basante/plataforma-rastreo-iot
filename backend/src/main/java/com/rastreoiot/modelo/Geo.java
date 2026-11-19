package com.rastreoiot.modelo;

/** Cálculos geográficos básicos. */
public final class Geo {

    private static final double RADIO_TIERRA_M = 6_371_000;

    private Geo() {
    }

    /** Distancia en metros entre dos coordenadas (fórmula de Haversine). */
    public static double distanciaMetros(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * RADIO_TIERRA_M * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    public static boolean coordenadasValidas(Double lat, Double lon) {
        return lat != null && lon != null && lat >= -90 && lat <= 90 && lon >= -180 && lon <= 180;
    }
}
