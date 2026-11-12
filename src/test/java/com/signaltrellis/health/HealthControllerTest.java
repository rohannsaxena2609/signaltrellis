package com.signaltrellis.health;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class HealthControllerTest {

    @Test
    void healthResponseIdentifiesRunningService() {
        var response = new HealthController().health();

        assertEquals("ok", response.status());
        assertEquals("SignalTrellis API", response.service());
        assertNotNull(response.timestamp());
    }
}
