package lk.icbt.cis6003.dentalclinic;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Smoke test: proves the Spring application context starts and every bean can be wired.
 *
 * <p>This is the first test of the project. If it fails, nothing else can be trusted,
 * so it runs on every push through GitHub Actions.</p>
 *
 * <p>Test ID: TC-000 (see the test plan in Task C).</p>
 */
@SpringBootTest
@ActiveProfiles("test")
class DentalClinicApplicationTests {

    @Test
    @DisplayName("TC-000: the application context loads successfully")
    void contextLoads() {
        // Passing without throwing is the assertion: Spring built every bean correctly.
    }
}
