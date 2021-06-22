package health.ere.ps.service.connector.endpoints;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import health.ere.ps.config.AppConfig;
import health.ere.ps.exception.common.security.SecretsManagerException;
import health.ere.ps.service.common.security.SecretsManagerService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okXml;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EndpointDiscoveryServiceTest {
    private static final String BIND_ADDRESS = "127.0.0.1";

    private static final int BIND_PORT = 8123;

    private static final String BASE_URI = "http://" + BIND_ADDRESS + ":" + BIND_PORT + "/";

    @Mock
    private AppConfig appConfig;

    @Mock
    private SecretsManagerService secretsManagerService;

    private WireMockServer wireMockServer;

    @BeforeEach
    void setupMock() {
        wireMockServer = new WireMockServer(WireMockConfiguration.options().bindAddress(BIND_ADDRESS).port(BIND_PORT));

        wireMockServer.start();
    }

    @AfterEach
    void stopMock() {
        wireMockServer.stop();
    }

    @Test
    void testConstructor() throws IOException, ParserConfigurationException, SecretsManagerException {
        String signatureServiceEndpoint = BASE_URI + "testSignatureService";

        String authSignatureServiceEndpoint = BASE_URI + "testAuthSignatureServiceEndpoint";

        String cardServiceEndpoint = BASE_URI + "testCardServiceEndpoint";

        String eventServiceEndpoint = BASE_URI + "testEventServiceEndpoint";

        String certificateServiceEndpoint = BASE_URI + "testCertificateServiceEndpoint";

        // disable SSL context
        when(appConfig.getConnectorTlsCertTrustStore()).thenReturn(Optional.empty());

        mockEndpoints(signatureServiceEndpoint, authSignatureServiceEndpoint, cardServiceEndpoint,
                eventServiceEndpoint, certificateServiceEndpoint);

        EndpointDiscoveryService endpointDiscoveryService = new EndpointDiscoveryService(appConfig,
                secretsManagerService, BASE_URI);

        assertEquals(signatureServiceEndpoint, endpointDiscoveryService.getSignatureServiceEndpoint());
        assertEquals(authSignatureServiceEndpoint, endpointDiscoveryService.getAuthSignatureServiceEndpoint());
        assertEquals(cardServiceEndpoint, endpointDiscoveryService.getCardServiceEndpoint());
        assertEquals(eventServiceEndpoint, endpointDiscoveryService.getEventServiceEndpoint());
        assertEquals(certificateServiceEndpoint, endpointDiscoveryService.getCertificateServiceEndpoint());
    }

    private void mockEndpoints(String signatureServiceEndpoint, String authSignatureServiceEndpoint,
                               String cardServiceEndpoint, String eventServiceEndpoint,
                               String certificateServiceEndpoint) {

        // TODO add some namespaces
        wireMockServer.stubFor(get("/connector.sds").willReturn(okXml("<ConnectorServices>" +
                "<ServiceInformation>" +
                Map.of("SignatureService", signatureServiceEndpoint,
                        "AuthSignatureService", authSignatureServiceEndpoint,
                        "CardService", cardServiceEndpoint,
                        "EventService", eventServiceEndpoint,
                        "CertificateService", certificateServiceEndpoint)
                        .entrySet()
                        .stream()
                        .map(entry -> "<Service Name=\"" + entry.getKey() + "\">" +
                                "<Versions><Version>" +
                                "<EndpointTLS Location=\"" + entry.getValue() + "\"/>" +
                                "</Version></Versions>" +
                                "</Service>")
                        .collect(Collectors.joining()) +
                "</ServiceInformation>" +
                "</ConnectorServices>")));
    }
}
