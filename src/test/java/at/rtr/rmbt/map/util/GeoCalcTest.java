package at.rtr.rmbt.map.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GeoCalcTest {

    @Test
    void latToMeters() {
        assertEquals(GeoCalc.latToMeters(49.99390813), 6445220.903278923);
    }

    @Test
    void lonToMeters() {
        assertEquals(GeoCalc.lonToMeters(16.31483016), 1816158.5857899422);
    }
}