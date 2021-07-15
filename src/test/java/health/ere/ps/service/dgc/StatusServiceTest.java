package health.ere.ps.service.dgc;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.http.Fault;
import de.gematik.ws.conn.cardservice.v8.CardInfoType;
import de.gematik.ws.conn.cardservice.v8.Cards;
import de.gematik.ws.conn.connectorcommon.v5.Status;
import de.gematik.ws.conn.eventservice.v7.GetCardsResponse;
import health.ere.ps.config.AppConfig;
import health.ere.ps.exception.connector.ConnectorCardsException;
import health.ere.ps.model.dgc.InstallationStatus;
import health.ere.ps.service.connector.cards.ConnectorCardsService;
import health.ere.ps.service.connector.endpoints.EndpointDiscoveryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.NoSuchElementException;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.unauthorized;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatusServiceTest {

    @Mock
    private ConnectorCardsService connectorCardsService;

    @Mock
    private EndpointDiscoveryService endpointDiscoveryService;

    @Mock
    private AppConfig appConfig;

    @InjectMocks
    private StatusService statusService;

    private WireMockServer wireMockServer;

    @BeforeEach
    void startup() {
        wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
        wireMockServer.start();
        assertTrue(wireMockServer.isRunning());
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        wireMockServer.stop();
        // TODO wireMockServer need time to terminate, it exception.
        Thread.sleep(2000);
    }

    @Test
    void testConnectivityFailed() {
        // given
        final String dgcPath = "/digital-green-certificate";
        final String idpPath = "/ipd-base-url";
        wireMockServer.stubFor(WireMock.get(idpPath).willReturn(aResponse().withFault(Fault.RANDOM_DATA_THEN_CLOSE)));
        wireMockServer.stubFor(WireMock.get(dgcPath).willReturn(unauthorized()));
        final String idpUrl = wireMockServer.url(idpPath);
        final String dgcUrl = wireMockServer.url(dgcPath);

        when(appConfig.getIdpBaseUrl()).thenReturn(idpUrl);
        when(appConfig.getDigitalGreenCertificateServiceIssuerAPI()).thenReturn(dgcUrl);

        // then
        final InstallationStatus installationStatus = statusService.collectStatus();
        assertEquals(idpUrl, installationStatus.getIdentityProviderRoute());
        assertEquals(InstallationStatus.State.FAIL, installationStatus.getIdentityProviderRouteState());
        assertEquals(dgcUrl, installationStatus.getCertificateServiceRoute());
        assertEquals(InstallationStatus.State.FAIL, installationStatus.getCertificateServiceRouteState());

        wireMockServer.verify(getRequestedFor(urlEqualTo(idpPath)));
        wireMockServer.verify(getRequestedFor(urlEqualTo(dgcPath)));
    }

    @Test
    void testConnectivityMissingUrls() {
        // given
        final String dgcPath = "/digital-green-certificate";

        wireMockServer.stubFor(WireMock.get(dgcPath).willReturn(ok()));
        final String dgcUrl = wireMockServer.url(dgcPath);

        when(appConfig.getIdpBaseUrl()).thenThrow(NoSuchElementException.class);
        when(appConfig.getDigitalGreenCertificateServiceIssuerAPI()).thenReturn(dgcUrl);

        // then
        final InstallationStatus installationStatus = statusService.collectStatus();
        assertNull(installationStatus.getIdentityProviderRoute());
        assertEquals(InstallationStatus.State.FAIL, installationStatus.getIdentityProviderRouteState());
        assertEquals(dgcUrl, installationStatus.getCertificateServiceRoute());
        assertEquals(InstallationStatus.State.OK, installationStatus.getCertificateServiceRouteState());

        wireMockServer.verify(getRequestedFor(urlEqualTo(dgcPath)));
    }

    @Test
    void testCardHandles() throws Exception {
        // given
        final String cardHandle = "SMCB-XXX";
        final GetCardsResponse response = new GetCardsResponse();
        final Status status = new Status();
        status.setResult("OK");
        response.setStatus(status);
        final Cards cards = new Cards();
        final CardInfoType cardHandleType = new CardInfoType();
        cardHandleType.setCardHandle(cardHandle);
        cards.getCard().add(cardHandleType);
        response.setCards(cards);
        when(connectorCardsService.getConnectorCards()).thenReturn(response);
        when(connectorCardsService.getCardHandle()).thenReturn(cardHandle);

        // then
        final InstallationStatus installationStatus = statusService.collectStatus();
        assertEquals(InstallationStatus.State.OK, installationStatus.getConnectorState());
        assertEquals(InstallationStatus.State.OK, installationStatus.getCardState());
        assertEquals(InstallationStatus.State.OK, installationStatus.getParameterState());
    }

    @Test
    void testCardHandlesButNotMatch() throws Exception {
        // given
        final String cardHandle = "SMCB-YYY";
        final GetCardsResponse response = new GetCardsResponse();
        final Status status = new Status();
        status.setResult("OK");
        response.setStatus(status);
        final Cards cards = new Cards();
        final CardInfoType cardHandleType = new CardInfoType();
        cardHandleType.setCardHandle(cardHandle);
        cards.getCard().add(cardHandleType);
        response.setCards(cards);

        when(connectorCardsService.getConnectorCards()).thenReturn(response);
        when(connectorCardsService.getCardHandle()).thenReturn(cardHandle);

        // then
        final InstallationStatus installationStatus = statusService.collectStatus();
        assertEquals(InstallationStatus.State.OK, installationStatus.getConnectorState());
        assertEquals(InstallationStatus.State.OK, installationStatus.getParameterState());
        assertEquals(InstallationStatus.State.OK, installationStatus.getCardState());
    }

    @Test
    void testCardHandleRequestFailed() throws Exception {
        // given
        when(connectorCardsService.getConnectorCards()).thenThrow(ConnectorCardsException.class);

        // then
        final InstallationStatus installationStatus = statusService.collectStatus();
        assertEquals(InstallationStatus.State.FAIL, installationStatus.getConnectorState());
        assertEquals(InstallationStatus.State.UNKNOWN, installationStatus.getCardState());
        assertEquals(InstallationStatus.State.UNKNOWN, installationStatus.getParameterState());
    }

    @Test
    void testCardHandleRequestNotOK() throws Exception{
        // given
        final GetCardsResponse response = new GetCardsResponse();
        final Status status = new Status();
        status.setResult("FAIL");
        response.setStatus(status);

        when(connectorCardsService.getConnectorCards()).thenReturn(response);
        // then
        final InstallationStatus installationStatus = statusService.collectStatus();
        assertEquals(InstallationStatus.State.OK, installationStatus.getConnectorState());
        assertEquals(InstallationStatus.State.FAIL, installationStatus.getParameterState());
        assertEquals(InstallationStatus.State.UNKNOWN, installationStatus.getCardState());
    }
}
