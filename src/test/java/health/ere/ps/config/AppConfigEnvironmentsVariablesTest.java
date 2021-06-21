package health.ere.ps.config;

import health.ere.ps.EnvironmentQuarkusTestProfile;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

import javax.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertEquals;

// ORDER is important, first set environment variable and than start quarkus
@SetEnvironmentVariable.SetEnvironmentVariables({
        // use the offline_test profile to not interfere with the other tests.
        @SetEnvironmentVariable(
                key = "_ENV_TEST_AUTH_SIGNATURE_ENDPOINT_ADDRESS", value = "foobarbaz"),
        @SetEnvironmentVariable(
                key = "_ENV_TEST_CONNECTOR_CERT_AUTH_STORE_FILE_PASSWORD", value = "tcpassword"),
})
@QuarkusTest
@TestProfile(EnvironmentQuarkusTestProfile.class)
class AppConfigEnvironmentsVariablesTest {

    @Inject
    AppConfig config;

    /**
     * The test should take from environment variable and ignore the .env and application_properties file.
     */
    @Test
    void propertiesShouldBeFromEnvironmentVariables() {
        assertEquals("foobarbaz", config.getAuthSignatureEndpointAddress());
        assertEquals("tcpassword", config.getConnectorTlsCertTustStorePwd());
    }

}
