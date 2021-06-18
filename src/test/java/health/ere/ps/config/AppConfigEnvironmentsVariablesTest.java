package health.ere.ps.config;

import health.ere.ps.LocalOfflineQuarkusTestProfile;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

import javax.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertEquals;

// ORDER is important, first set environment variable and than start quarkus
@SetEnvironmentVariable.SetEnvironmentVariables({
        // use the offline_test profile to not interfere with the other tests.
        @SetEnvironmentVariable(
                key = "_OFFLINE_TEST_IDP_CONNECTOR_AUTH_SIGNATURE_ENDPOINT_ADDRESS", value = "foobarbaz"),
        @SetEnvironmentVariable(
                key = "_OFFLINE_TEST_CONNECTOR_SIMULATOR_TITUSCLIENTCERTIFICATEPASSWORD", value = "tcpassword"),
        @SetEnvironmentVariable(
                key = "THIS_IS_A_TEST_PROPERTY", value = "this.is.a.test.property")
})
@QuarkusTest
@TestProfile(LocalOfflineQuarkusTestProfile.class)
class AppConfigEnvironmentsVariablesTest {

    /**
     * Creates new property to not interfere with others.
     */
    @ConfigProperty(name = "this.is.a.test.property")
    String testProperty;

    @Inject
    AppConfig config;

    /**
     * The test should take from environment variable and ignore the .env and application_properties file.
     */
    @Test
    void propertiesShouldBeFromEnvironmentVariables() {
        assertEquals("foobarbaz", config.getIdpConnectorAuthSignatureEndpointAddress());
        assertEquals("tcpassword", config.getIdpConnectorTlsCertTustStorePwd());
        assertEquals("this.is.a.test.property", testProperty);
    }

}