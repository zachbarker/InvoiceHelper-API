package com.invoiceflow;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class InvoiceflowApiApplicationTests {

    @Test
    void contextLoads() {
        // Verifies the Spring context wires up cleanly (DB, Security, JPA config)
        // against the H2 test profile. Expand with real integration tests as
        // features land.
    }
}
