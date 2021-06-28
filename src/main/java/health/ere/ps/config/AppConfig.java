package health.ere.ps.config;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

/**
 * Configurations of the application.
 */
@ApplicationScoped
public class AppConfig {

    /**
     * Id of the client system, it can be group of systems. may required to call the connector.
     * see ConnectorContext.xsd in gematik specification.
     */
    @Inject @ConfigProperty(name = "connector.client.system.id")
    String clientSystemId;

    /**
     * Id of the workspace. may required to call the connector.
     * See ConnectorContext.xsd in gematik specification.
     */
    @Inject @ConfigProperty(name = "connector.mandant.id")
    String mandantId;

    /**
     * Id of the mandant.
     * See ConnectorContext.xsd in gematik specification.
     */
    @Inject @ConfigProperty(name = "connector.workplace.id")
    String workplaceId;

    @Inject @ConfigProperty(name = "connector.user.id")
    String userId;

    @Inject @ConfigProperty(name = "idp.client.id")
    String clientId;

    @Inject @ConfigProperty(name = "idp.base.url")
    String idpBaseUrl;

    @Inject @ConfigProperty(name = "idp.auth.request.redirect.url")
    String redirectUrl;

    @Inject @ConfigProperty(name = "digital-green-certificate-service.issuerAPIUrl")
    String digitalGreenCertificateServiceIssuerAPI;

    public String getClientId() {
        return clientId;
    }

    public String getClientSystemId() {
        return clientSystemId;
    }

    public String getWorkplaceId() {
        return workplaceId;
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
