package health.ere.ps.service.idp.client;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.Optional;
import java.util.logging.LogManager;

import javax.inject.Inject;

import health.ere.ps.config.AppConfig;
import health.ere.ps.exception.common.security.SecretsManagerException;
import health.ere.ps.exception.connector.ConnectorCardCertificateReadException;
import health.ere.ps.exception.connector.ConnectorCardsException;
import health.ere.ps.exception.idp.IdpClientException;
import health.ere.ps.exception.idp.IdpException;
import health.ere.ps.exception.idp.IdpJoseException;
import health.ere.ps.model.idp.client.IdpTokenResult;
import health.ere.ps.service.common.security.SecretsManagerService;
import health.ere.ps.service.common.security.SecureSoapTransportConfigurer;
import health.ere.ps.service.connector.cards.ConnectorCardsService;
import health.ere.ps.service.connector.certificate.CardCertificateReaderService;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class IdpClientTest {

    @Inject
    AppConfig appConfig;

    @Inject
    IdpClient idpClient;

    @Inject
    CardCertificateReaderService cardCertificateReaderService;

    @Inject
    ConnectorCardsService connectorCardsService;

    @Inject
    SecureSoapTransportConfigurer secureSoapTransportConfigurer;

    @ConfigProperty(name = "idp.client.id")
    String clientId;

    @ConfigProperty(name = "idp.connector.client.system.id")
    String clientSystem;

    @ConfigProperty(name = "idp.connector.workplace.id")
    String workplace;

//    @ConfigProperty(name = "idp.connector.card.handle")
//    String cardHandle;

    @ConfigProperty(name = "idp.base.url")
    String idpBaseUrl;

    String discoveryDocumentUrl;

    @ConfigProperty(name = "idp.auth.request.redirect.url")
    String redirectUrl;

    @BeforeAll
    public static void init() {

        try {
			// https://community.oracle.com/thread/1307033?start=0&tstart=0
			LogManager.getLogManager().readConfiguration(
                IdpClientTest.class
							.getResourceAsStream("/logging.properties"));
		} catch (IOException e) {
			e.printStackTrace();
		}

        System.setProperty("com.sun.xml.ws.transport.http.client.HttpTransportPipe.dump", "true");
        System.setProperty("com.sun.xml.internal.ws.transport.http.client.HttpTransportPipe.dump", "true");
        System.setProperty("com.sun.xml.ws.transport.http.HttpAdapter.dump", "true");
        System.setProperty("com.sun.xml.internal.ws.transport.http.HttpAdapter.dump", "true");
        System.setProperty("com.sun.xml.ws.transport.http.HttpAdapter.dumpTreshold", "999999");
    }

    @BeforeEach
    void configureSecureTransport() throws SecretsManagerException {
        secureSoapTransportConfigurer.init(connectorCardsService);
        secureSoapTransportConfigurer.configureSecureTransport(
                appConfig.getEventServiceEndpointAddress(),
                SecretsManagerService.SslContextType.TLS,
                appConfig.getIdpConnectorTlsCertTrustStore(),
                appConfig.getIdpConnectorTlsCertTustStorePwd());
    }

    @Test
    public void test_Successful_Idp_Login_With_Connector_Smcb() throws IdpJoseException,
            IdpClientException, IdpException, ConnectorCardCertificateReadException,
            ConnectorCardsException {

        discoveryDocumentUrl = idpBaseUrl + IdpHttpClientService.DISCOVERY_DOCUMENT_URI;

        idpClient.init(clientId, redirectUrl, discoveryDocumentUrl, true);
        idpClient.initializeClient();

        Optional<String> cardHandle = connectorCardsService.getConnectorCardHandle(
                ConnectorCardsService.CardHandleType.SMC_B);

        X509Certificate x509Certificate = cardCertificateReaderService.retrieveSmcbCardCertificate(clientId,
                clientSystem, workplace, cardHandle.get());

        IdpTokenResult idpTokenResult = idpClient.login(x509Certificate);

        Assertions.assertNotNull(idpTokenResult, "Idp Token result present.");
        Assertions.assertNotNull(idpTokenResult.getAccessToken(), "Access Token present");
        Assertions.assertNotNull(idpTokenResult.getIdToken(), "Id Token present");
    }
}
