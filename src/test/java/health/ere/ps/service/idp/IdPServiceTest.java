package health.ere.ps.service.idp;

import health.ere.ps.config.AppConfig;
import health.ere.ps.event.RequestBearerTokenFromIdpEvent;
import health.ere.ps.exception.connector.ConnectorCardCertificateReadException;
import health.ere.ps.exception.connector.ConnectorCardsException;
import health.ere.ps.exception.idp.IdpClientException;
import health.ere.ps.exception.idp.IdpException;
import health.ere.ps.exception.idp.IdpJoseException;
import health.ere.ps.model.dgc.CallContext;
import health.ere.ps.model.idp.client.IdpTokenResult;
import health.ere.ps.model.idp.client.token.JsonWebToken;
import health.ere.ps.model.idp.crypto.PkiIdentity;
import health.ere.ps.service.connector.cards.ConnectorCardsService;
import health.ere.ps.service.connector.certificate.CardCertificateReaderService;
import health.ere.ps.service.idp.client.IdpClient;
import health.ere.ps.service.idp.client.IdpHttpClientService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.enterprise.event.Event;
import java.security.cert.X509Certificate;
import java.util.Optional;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdPServiceTest {
    private static final String CLIENT_ID = "testClientId";

    private static final String IDP_BASE_URL = "testIdpBaseUrl";

    private static final String REDIRECT_URL = "testRedirectUrl";

    @Mock
    private AppConfig appConfig;

    @Mock
    private IdpClient idpClient;

    @Mock
    private ConnectorCardsService connectorCardsService;

    @Mock
    private CardCertificateReaderService cardCertificateReaderService;

    @Mock
    private Event<Exception> exceptionEvent;

    @InjectMocks
    private IdPService idpService;

    @BeforeEach
    void setConfigValues() {
        idpService.clientId = CLIENT_ID;
        idpService.idpBaseUrl = IDP_BASE_URL;
        idpService.redirectUrl = REDIRECT_URL;
    }

    @Test
    void requestBearerToken() throws ConnectorCardsException, ConnectorCardCertificateReadException, IdpJoseException,
            IdpClientException, IdpException {

        String mandantId = "testMandantId";

        String clientSystem = "testClientSystem";

        String workplace = "testWorkplace";

        String cardHandle = "testCardHandle";

        X509Certificate x509Certificate = mock(X509Certificate.class);

        String token = "testToken";

        IdpTokenResult idpTokenResult = mock(IdpTokenResult.class);

        JsonWebToken accessToken = mock(JsonWebToken.class);

        when(appConfig.getMandantId()).thenReturn(mandantId);
        when(appConfig.getClientSystem()).thenReturn(clientSystem);
        when(appConfig.getWorkplace()).thenReturn(workplace);
        when(connectorCardsService.getConnectorCardHandle(ConnectorCardsService.CardHandleType.SMC_B))
                .thenReturn(Optional.of(cardHandle));
        when(cardCertificateReaderService.retrieveSmcbCardCertificate(mandantId, clientSystem, workplace, cardHandle))
                .thenReturn(x509Certificate);
        when(idpClient.login(new PkiIdentity(x509Certificate))).thenReturn(idpTokenResult);
        when(idpTokenResult.getAccessToken()).thenReturn(accessToken);
        when(accessToken.getRawString()).thenReturn(token);

        RequestBearerTokenFromIdpEvent requestBearerTokenFromIdpEvent = mock(RequestBearerTokenFromIdpEvent.class);

        idpService.requestBearerToken(requestBearerTokenFromIdpEvent);

        verify(requestBearerTokenFromIdpEvent).setBearerToken(token);
        verify(requestBearerTokenFromIdpEvent).getCallContext();
        verifyNoMoreInteractions(requestBearerTokenFromIdpEvent);
        verifyNoInteractions(x509Certificate);

        InOrder inOrder = inOrder(idpClient, cardCertificateReaderService);

        inOrder.verify(idpClient).init(CLIENT_ID, REDIRECT_URL,
                IDP_BASE_URL + IdpHttpClientService.DISCOVERY_DOCUMENT_URI, true);
        inOrder.verify(idpClient).initializeClient();
        inOrder.verify(cardCertificateReaderService).retrieveSmcbCardCertificate(mandantId, clientSystem, workplace,
                cardHandle);
        inOrder.verify(idpClient).login(new PkiIdentity(x509Certificate));
    }

    @Test
    void requestBearerTokenWithCallContext() throws ConnectorCardCertificateReadException, IdpJoseException,
            IdpClientException, IdpException {

        String mandantId = "testMandantId";

        String clientSystem = "testClientSystem";

        String workplace = "testWorkplace";

        String cardHandle = "testCardHandle";

        X509Certificate x509Certificate = mock(X509Certificate.class);

        String token = "testToken";

        IdpTokenResult idpTokenResult = mock(IdpTokenResult.class);

        JsonWebToken accessToken = mock(JsonWebToken.class);

        RequestBearerTokenFromIdpEvent requestBearerTokenFromIdpEvent = mock(RequestBearerTokenFromIdpEvent.class);

        CallContext callContext = mock(CallContext.class);

        when(requestBearerTokenFromIdpEvent.getCallContext()).thenReturn(callContext);
        when(callContext.getMandantId()).thenReturn(mandantId);
        when(callContext.getClientSystem()).thenReturn(clientSystem);
        when(callContext.getWorkplace()).thenReturn(workplace);
        when(callContext.getCardHandle()).thenReturn(cardHandle);
        when(cardCertificateReaderService.retrieveSmcbCardCertificate(mandantId, clientSystem, workplace, cardHandle))
                .thenReturn(x509Certificate);
        when(idpClient.login(new PkiIdentity(x509Certificate))).thenReturn(idpTokenResult);
        when(idpTokenResult.getAccessToken()).thenReturn(accessToken);
        when(accessToken.getRawString()).thenReturn(token);

        idpService.requestBearerToken(requestBearerTokenFromIdpEvent);

        verify(requestBearerTokenFromIdpEvent).setBearerToken(token);
        verify(requestBearerTokenFromIdpEvent).getCallContext();
        verifyNoMoreInteractions(requestBearerTokenFromIdpEvent);
        verifyNoInteractions(x509Certificate);
        verifyNoInteractions(appConfig);
        verifyNoInteractions(connectorCardsService);

        InOrder inOrder = inOrder(idpClient, cardCertificateReaderService);

        inOrder.verify(idpClient).init(CLIENT_ID, REDIRECT_URL,
                IDP_BASE_URL + IdpHttpClientService.DISCOVERY_DOCUMENT_URI, true);
        inOrder.verify(idpClient).initializeClient();
        inOrder.verify(cardCertificateReaderService).retrieveSmcbCardCertificate(mandantId, clientSystem, workplace,
                cardHandle);
        inOrder.verify(idpClient).login(new PkiIdentity(x509Certificate));
    }

    @Test
    void requestBearerTokenWithException() throws IdpJoseException, IdpClientException, IdpException {
        // no mock to avoid NPE when writing the log
        IdpClientException idpClientException = new IdpClientException("test");

        when(idpClient.initializeClient()).thenThrow(idpClientException);

        RequestBearerTokenFromIdpEvent requestBearerTokenFromIdpEvent = mock(RequestBearerTokenFromIdpEvent.class);

        idpService.requestBearerToken(requestBearerTokenFromIdpEvent);

        verifyNoInteractions(requestBearerTokenFromIdpEvent);
        verify(exceptionEvent).fireAsync(idpClientException);
    }
}
