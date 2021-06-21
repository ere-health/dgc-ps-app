package health.ere.ps.config;

import org.apache.commons.lang3.StringUtils;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.context.ApplicationScoped;
import java.util.Optional;

/**
 * Configurations of the application.
 */
@ApplicationScoped
public class AppConfig {

    /**
     * Certificate to authenticate at the connector.
     */
    @ConfigProperty(name = "connector.cert.auth.store.file")
    Optional<String> connectorTlsCertTrustStore;

    /**
     * Password of the certificate to authenticate at the connector.
     * The default value is a empty sting, so that the password must not be set.
     */
    @ConfigProperty(name = "connector.cert.auth.store.file.password", defaultValue = "")
    String connectorTlsCertTustStorePwd;

    /**
     * Id of the client system, it can be group of systems. may required to call the connector.
     * see ConnectorContext.xsd in gematik specification.
     */
    @ConfigProperty(name = "connector.client.system.id")
    String clientSystem;

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
    String workplace;

    /**
     * Card handle for the connector.
     * See ConnectorCommons.xsd in gematik specification.
     */
    @ConfigProperty(name = "connector.card.handle")
    String cardHandle;

    @ConfigProperty(name = "idp.client.id")
    String clientId;

    @ConfigProperty(name = "idp.connector.auth-signature.endpoint.address")
    String idpConnectorAuthSignatureEndpointAddress;

    @ConfigProperty(name = "signature-service.context.mandantId")
    String signatureServiceContextMandantId;

    @ConfigProperty(name = "signature-service.context.clientSystemId")
    String signatureServiceContextClientSystemId;

    @ConfigProperty(name = "signature-service.context.workplaceId")
    String signatureServiceContextWorkplaceId;

    @ConfigProperty(name = "signature-service.context.userId")
    String signatureServiceContextUserId;

    @ConfigProperty(name = "auth-signature-service.endpointAddress", defaultValue = "")
    String authSignatureServiceEndpointAddress;

    @ConfigProperty(name = "auth-signature-service.smbcCardHandle", defaultValue = "")
    String authSignatureServiceSmbcCardHandle;

    @ConfigProperty(name = "event-service.endpointAddress")
    String eventServiceEndpointAddress;

    @ConfigProperty(name = "idp.base.url")
    String idpBaseUrl;

    @ConfigProperty(name = "idp.auth.request.redirect.url")
    String redirectUrl;

    @ConfigProperty(name = "card-service.endpointAddress", defaultValue = "")
    String cardServiceEndpointAddress;

    @ConfigProperty(name = "digital-green-certificate-service.issuerAPIUrl")
    String digitalGreenCertificateServiceIssuerAPI;

    public Optional<String> getConnectorTlsCertTrustStore() {

        return connectorTlsCertTrustStore;
    }

    public String getConnectorTlsCertTustStorePwd() {
        return StringUtils.defaultString(connectorTlsCertTustStorePwd).trim();
    }

    public String getClientId() {

        return clientId;
    }

    public String getClientSystem() {

        return clientSystem;
    }

    public String getWorkplace() {

        return workplace;
    }

    public String getCardHandle() {

        return cardHandle;
    }

    public String getIdpConnectorAuthSignatureEndpointAddress() {
        return idpConnectorAuthSignatureEndpointAddress;
    }

    public String getSignatureServiceContextMandantId() {
        return signatureServiceContextMandantId;
    }

    public String getSignatureServiceContextClientSystemId() {
        return signatureServiceContextClientSystemId;
    }

    public String getSignatureServiceContextWorkplaceId() {
        return signatureServiceContextWorkplaceId;
    }

    public String getSignatureServiceContextUserId() {
        return signatureServiceContextUserId;
    }

    public String getEventServiceEndpointAddress() {
        return eventServiceEndpointAddress;
    }

    public String getMandantId() {
        return this.mandantId;
    }

    public void setMandantId(String mandantId) {
        this.mandantId = mandantId;
    }

    public String getAuthSignatureServiceEndpointAddress() {
        return authSignatureServiceEndpointAddress;
    }

    public String getAuthSignatureServiceSmbcCardHandle() {
        return authSignatureServiceSmbcCardHandle;
    }

    public String getIdpBaseUrl() {
        return idpBaseUrl;
    }

    public String getRedirectUrl() {
        return redirectUrl;
    }

    public String getCardServiceEndpointAddress() {
        return cardServiceEndpointAddress;
    }

    public String getDigitalGreenCertificateServiceIssuerAPI() {
        return digitalGreenCertificateServiceIssuerAPI;
    }

}
