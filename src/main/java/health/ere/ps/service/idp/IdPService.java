package health.ere.ps.service.idp;

import health.ere.ps.model.dgc.CallContext;
import health.ere.ps.model.idp.crypto.PkiIdentity;

import java.security.cert.X509Certificate;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import health.ere.ps.config.AppConfig;
import health.ere.ps.event.RequestBearerTokenFromIdpEvent;
import health.ere.ps.exception.common.security.SecretsManagerException;
import health.ere.ps.exception.connector.ConnectorCardCertificateReadException;
import health.ere.ps.exception.connector.ConnectorCardsException;
import health.ere.ps.exception.idp.IdpClientException;
import health.ere.ps.exception.idp.IdpException;
import health.ere.ps.exception.idp.IdpJoseException;
import health.ere.ps.model.idp.client.IdpTokenResult;
import health.ere.ps.service.common.security.SecureSoapTransportConfigurer;
import health.ere.ps.service.connector.cards.ConnectorCardsService;
import health.ere.ps.service.connector.certificate.CardCertificateReaderService;
import health.ere.ps.service.connector.endpoints.EndpointDiscoveryService;
import health.ere.ps.service.idp.client.IdpClient;
import health.ere.ps.service.idp.client.IdpHttpClientService;

@ApplicationScoped
public class IdPService {

    private static final Logger log = Logger.getLogger(IdPService.class.getName());

    @Inject
    IdpClient idpClient;

    @Inject
    CardCertificateReaderService cardCertificateReaderService;

    @Inject
    AppConfig appConfig;

    @Inject
    EndpointDiscoveryService endpointDiscoveryService;

    @Inject
    ConnectorCardsService connectorCardsService;

    @Inject
    SecureSoapTransportConfigurer secureSoapTransportConfigurer;

    @Inject
    Event<Exception> exceptionEvent;

    @PostConstruct
    void init() throws SecretsManagerException {
        secureSoapTransportConfigurer.init(connectorCardsService);

        secureSoapTransportConfigurer.configureSecureTransport(
                endpointDiscoveryService.getEventServiceEndpointAddress());
    }

    public void requestBearerToken(@Observes RequestBearerTokenFromIdpEvent requestBearerTokenFromIdpEvent) {
        try {
            String discoveryDocumentUrl = appConfig.getIdpBaseUrl() + IdpHttpClientService.DISCOVERY_DOCUMENT_URI;
            idpClient.init(appConfig.getClientId(), appConfig.getRedirectUrl(), discoveryDocumentUrl, true);
            idpClient.initializeClient();

            CallContext callContext = requestBearerTokenFromIdpEvent.getCallContext();

            String mandantId;

            String clientSystem;

            String workplace;

            String cardHandle;

            if (callContext == null) {
                mandantId = appConfig.getMandantId();
                clientSystem = appConfig.getClientSystemId();
                workplace = appConfig.getWorkplaceId();
                cardHandle = connectorCardsService.getConnectorCardHandle(
                        ConnectorCardsService.CardHandleType.SMC_B, mandantId, clientSystem, workplace).orElseThrow();
            } else {
                mandantId = Optional.ofNullable(callContext.getMandantId()).orElseGet(appConfig::getMandantId);
                clientSystem = Optional.ofNullable(callContext.getClientSystem()).orElseGet(appConfig::getClientSystemId);
                workplace = Optional.ofNullable(callContext.getWorkplace()).orElseGet(appConfig::getWorkplaceId);
                if (callContext.getCardHandle() != null) {
                    cardHandle = callContext.getCardHandle();
                }  else {
                    cardHandle = connectorCardsService.getConnectorCardHandle(
                            ConnectorCardsService.CardHandleType.SMC_B, mandantId, clientSystem, workplace).orElseThrow();
                }
            }

            X509Certificate x509Certificate =
                    cardCertificateReaderService.retrieveSmcbCardCertificate(mandantId, clientSystem, workplace,
                            cardHandle);

            IdpTokenResult idpTokenResult = idpClient.login(new PkiIdentity(x509Certificate), mandantId, clientSystem, workplace, cardHandle);
            requestBearerTokenFromIdpEvent.setBearerToken(idpTokenResult.getAccessToken().getRawString());
        } catch(IdpClientException | IdpException | IdpJoseException | ConnectorCardCertificateReadException |
                ConnectorCardsException e) {
            log.log(Level.WARNING, "Idp login did not work", e);
            requestBearerTokenFromIdpEvent.setException(e);
            exceptionEvent.fireAsync(e);
        }
    }
}
