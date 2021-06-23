package health.ere.ps.service.connector.endpoints;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import health.ere.ps.exception.common.security.SecretsManagerException;
import health.ere.ps.service.common.security.SecretsManagerService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okXml;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Although this is an integration test, we still use mockito to inject values. This is sufficient for the current
 * implementation.
 */
@ExtendWith(MockitoExtension.class)
class EndpointDiscoveryServiceIntegrationTest {
    private static final String BIND_ADDRESS = "127.0.0.1";

    private static final int BIND_PORT = 8123;

    private static final String BASE_URI = "https://" + BIND_ADDRESS + ":" + BIND_PORT + "/";

    @TempDir
    File tempDir;

    // test the real secrets manager
    @Spy
    private SecretsManagerService secretsManagerService;

    @InjectMocks
    private EndpointDiscoveryService endpointDiscoveryService;

    private WireMockServer wireMockServer;

    @BeforeEach
    void setupMock() {
        wireMockServer = new WireMockServer(WireMockConfiguration.options()
                .bindAddress(BIND_ADDRESS)
                .httpDisabled(true)
                .httpsPort(BIND_PORT));

        wireMockServer.start();
    }

    @AfterEach
    void stopMock() {
        wireMockServer.stop();
    }

    @Test
    void obtainConfiguration() throws IOException, ParserConfigurationException, SecretsManagerException {
        String authSignatureServiceEndpoint = BASE_URI + "testAuthSignatureServiceEndpoint";

        String cardServiceEndpoint = BASE_URI + "testCardServiceEndpoint";

        String eventServiceEndpoint = BASE_URI + "testEventServiceEndpoint";

        String certificateServiceEndpoint = BASE_URI + "testCertificateServiceEndpoint";

        // disable client ssl certificate SSL context
        endpointDiscoveryService.connectorTlsCertAuthStoreFile = Optional.empty();
        endpointDiscoveryService.connectorVerifyHostname = "false";
        endpointDiscoveryService.connectorBaseUri = BASE_URI;

        // verify server certificate of wiremock server
        try (InputStream inputStream = getClass().getClassLoader().getResource("keystore").openConnection().getInputStream()) {
            File file = new File(tempDir, "test-keystore");

            Files.write(file.toPath(), inputStream.readAllBytes());
            endpointDiscoveryService.connectorTlsCertTrustStoreFile = Optional.of(file.getAbsolutePath());
        }
        endpointDiscoveryService.connectorTlsCertTrustStorePwd = "password";

        // setup fallbacks to check prioritizing
        endpointDiscoveryService.fallbackAuthSignatureServiceEndpointAddress = Optional.of(authSignatureServiceEndpoint + "changed");
        endpointDiscoveryService.fallbackCardServiceEndpointAddress = Optional.of(cardServiceEndpoint + "changed");
        endpointDiscoveryService.fallbackCertificateServiceEndpointAddress = Optional.of(certificateServiceEndpoint + "changed");
        endpointDiscoveryService.fallbackEventServiceEndpointAddress = Optional.of(eventServiceEndpoint + "changed");

        mockEndpoints(authSignatureServiceEndpoint, cardServiceEndpoint, eventServiceEndpoint,
                certificateServiceEndpoint);

        endpointDiscoveryService.obtainConfiguration();

        assertEquals(authSignatureServiceEndpoint, endpointDiscoveryService.getAuthSignatureServiceEndpointAddress());
        assertEquals(cardServiceEndpoint, endpointDiscoveryService.getCardServiceEndpointAddress());
        assertEquals(eventServiceEndpoint, endpointDiscoveryService.getEventServiceEndpointAddress());
        assertEquals(certificateServiceEndpoint, endpointDiscoveryService.getCertificateServiceEndpointAddress());
    }

    @Test
    void obtainConfigurationWithFallbackEndpoints() throws SecretsManagerException, IOException, ParserConfigurationException {
        String authSignatureServiceEndpoint = BASE_URI + "testAuthSignatureServiceEndpoint";

        String cardServiceEndpoint = BASE_URI + "testCardServiceEndpoint";

        String eventServiceEndpoint = BASE_URI + "testEventServiceEndpoint";

        String certificateServiceEndpoint = BASE_URI + "testCertificateServiceEndpoint";

        // disable client ssl certificate SSL context
        endpointDiscoveryService.connectorTlsCertAuthStoreFile = Optional.empty();
        endpointDiscoveryService.connectorVerifyHostname = "false";
        endpointDiscoveryService.connectorBaseUri = BASE_URI;

        // disable server certificate verification of wiremock server
        endpointDiscoveryService.connectorTlsCertTrustStoreFile = Optional.empty();

        // setup fallbacks
        endpointDiscoveryService.fallbackAuthSignatureServiceEndpointAddress = Optional.of(authSignatureServiceEndpoint);
        endpointDiscoveryService.fallbackCardServiceEndpointAddress = Optional.of(cardServiceEndpoint);
        endpointDiscoveryService.fallbackCertificateServiceEndpointAddress = Optional.of(certificateServiceEndpoint);
        endpointDiscoveryService.fallbackEventServiceEndpointAddress = Optional.of(eventServiceEndpoint);

        // mocking with empty strings causes the location to be discarded
        mockEndpoints("", "", "", "");


        endpointDiscoveryService.obtainConfiguration();

        assertEquals(authSignatureServiceEndpoint, endpointDiscoveryService.getAuthSignatureServiceEndpointAddress());
        assertEquals(cardServiceEndpoint, endpointDiscoveryService.getCardServiceEndpointAddress());
        assertEquals(eventServiceEndpoint, endpointDiscoveryService.getEventServiceEndpointAddress());
        assertEquals(certificateServiceEndpoint, endpointDiscoveryService.getCertificateServiceEndpointAddress());
    }

    private void mockEndpoints(String authSignatureServiceEndpoint,
                               String cardServiceEndpoint, String eventServiceEndpoint,
                               String certificateServiceEndpoint) {

        wireMockServer.stubFor(get("/connector.sds").willReturn(okXml("<ConnectorServices " +
                "xmlns=\"http://localhost/ns0/v0.1\" " +
                "xmlns:ns10=\"http://localhost/ns10/v1.23\">" +
                "<ns10:ServiceInformation>" +
                Map.of("AuthSignatureService", authSignatureServiceEndpoint,
                        "CardService", cardServiceEndpoint,
                        "EventService", eventServiceEndpoint,
                        "CertificateService", certificateServiceEndpoint)
                        .entrySet()
                        .stream()
                        .map(entry -> "<ns10:Service Name=\"" + entry.getKey() + "\">" +
                                "<ns10:Versions><ns10:Version>" +
                                "<ns10:EndpointTLS Location=\"" + entry.getValue() + "\"/>" +
                                "</ns10:Version></ns10:Versions>" +
                                "</ns10:Service>")
                        .collect(Collectors.joining()) +
                "</ns10:ServiceInformation>" +
                "</ConnectorServices>")));
    }
}
