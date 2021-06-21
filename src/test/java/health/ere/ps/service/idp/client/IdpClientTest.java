package health.ere.ps.service.idp.client;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
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
import health.ere.ps.model.idp.crypto.PkiIdentity;
import health.ere.ps.service.common.security.SecretsManagerService;
import health.ere.ps.service.common.security.SecureSoapTransportConfigurer;
import health.ere.ps.service.connector.cards.ConnectorCardsService;
import health.ere.ps.service.connector.certificate.CardCertificateReaderService;
import io.quarkus.test.junit.QuarkusTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;

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

    String discoveryDocumentUrl;

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
        if (appConfig.getConnectorTlsCertTrustStore().isPresent()) {
            secureSoapTransportConfigurer.configureSecureTransport(
                    appConfig.getEventServiceEndpointAddress(),
                    SecretsManagerService.SslContextType.TLS,
                    appConfig.getConnectorTlsCertTrustStore().get(),
                    appConfig.getConnectorTlsCertTustStorePwd());
        }
    }


    @Test
    @Disabled("This test will only work in the Telematik RU Infrastructure")
    public void test_Successful_Idp_Login_With_Connector_Smcb() throws IdpJoseException,
            IdpClientException, IdpException, ConnectorCardCertificateReadException,
            ConnectorCardsException {

        discoveryDocumentUrl = appConfig.getIdpBaseUrl() + IdpHttpClientService.DISCOVERY_DOCUMENT_URI;

        idpClient.init(appConfig.getClientId(), appConfig.getRedirectUrl(), discoveryDocumentUrl, true);
        idpClient.initializeClient();

        String cardHandle = connectorCardsService.getConnectorCardHandle(
                ConnectorCardsService.CardHandleType.SMC_B).orElse(null);
        assertNotNull(cardHandle);

        X509Certificate x509Certificate = cardCertificateReaderService.retrieveSmcbCardCertificate(
                appConfig.getMandantId(),
                appConfig.getClientSystem(),
                appConfig.getWorkplace(),
                cardHandle);

        IdpTokenResult idpTokenResult = idpClient.login(new PkiIdentity(x509Certificate));

        assertNotNull(idpTokenResult, "Idp Token result present.");
        assertNotNull(idpTokenResult.getAccessToken(), "Access Token present");
        assertNotNull(idpTokenResult.getIdToken(), "Id Token present");
    }
}
