package health.ere.ps.config;

import org.apache.commons.lang3.StringUtils;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import health.ere.ps.exception.connector.ConnectorCardsException;
import health.ere.ps.service.connector.cards.ConnectorCardsService;
import health.ere.ps.service.connector.cards.ConnectorCardsService.CardHandleType;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import java.util.Optional;

/**
 * Configurations of the application.
 */
@ApplicationScoped
public class AppConfig {

    /**
     * Id of the client system, it can be group of systems. may required to call the connector.
     * see ConnectorContext.xsd in gematik specification.
     */
    @ConfigProperty(name = "connector.client.system.id")
    String clientSystemId;

    /**
     * Id of the workspace. may required to call the connector.
     * See ConnectorContext.xsd in gematik specification.
     */
    @ConfigProperty(name = "connector.mandant.id")
    String mandantId;

    /**
     * Id of the mandant.
     * See ConnectorContext.xsd in gematik specification.
     */
    @ConfigProperty(name = "connector.workplace.id")
    String workplaceId;

    /**
     * Card handle for the connector.
     * See ConnectorCommons.xsd in gematik specification.
     */
    @ConfigProperty(name = "connector.card.handle")
    Optional<String> cardHandle;

    @ConfigProperty(name = "connector.user.id")
    String userId;

    @ConfigProperty(name = "idp.client.id")
    String clientId;

    @ConfigProperty(name = "idp.base.url")
    String idpBaseUrl;

    @ConfigProperty(name = "idp.auth.request.redirect.url")
    String redirectUrl;

    @ConfigProperty(name = "digital-green-certificate-service.issuerAPIUrl")
    String digitalGreenCertificateServiceIssuerAPI;

    @Inject
    ConnectorCardsService connectorCardsService;

    public String getClientId() {
        return clientId;
    }

    public String getClientSystemId() {
        return clientSystemId;
    }

    public String getWorkplaceId() {
        return workplaceId;
    }

    public String getCardHandle() {
        if (cardHandle.isPresent()) {
            return cardHandle.get();
        } else {
            try {
                return connectorCardsService.getConnectorCardHandle(CardHandleType.SMC_B).orElseThrow();
            } catch (ConnectorCardsException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public String getUserId() {
        return userId;
    }

    public String getMandantId() {
        return this.mandantId;
    }

    public String getIdpBaseUrl() {
        return idpBaseUrl;
    }

    public String getRedirectUrl() {
        return redirectUrl;
    }

    public String getDigitalGreenCertificateServiceIssuerAPI() {
        return digitalGreenCertificateServiceIssuerAPI;
    }
}
