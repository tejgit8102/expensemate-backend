package com.expensemate.expensemate_backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
    "app.jwt.secret=mysupersecretkey123456789012345678901234567890",
    "app.jwt.expiration=86400000"
})
public class JwtConfigTest {

    @Test
    public void contextLoads() {
        // This test will pass if the JWT configuration is correct
    }
}

