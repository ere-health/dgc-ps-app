package health.ere.ps.config;

import org.apache.commons.lang3.StringUtils;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class AppConfig {
	
	@Inject @ConfigProperty(name = "idp.connector.cert.auth.store.file")
    String idpConnectorTlsCertTrustStore;

    @Inject @ConfigProperty(name = "idp.connector.cert.auth.store.file.password")
    String idpConnectorTlsCertTustStorePwd;

    @Inject @ConfigProperty(name = "idp.client.id")
    String clientId;

    @Inject @ConfigProperty(name = "idp.connector.client.system.id")
    String clientSystem;

    @Inject @ConfigProperty(name = "idp.connector.mandant.id")
    String mandantId;

    @Inject @ConfigProperty(name = "idp.connector.workplace.id")
    String workplace;

    @Inject @ConfigProperty(name = "idp.connector.card.handle")
    String cardHandle;

    @Inject @ConfigProperty(name = "idp.connector.auth-signature.endpoint.address")
    String idpConnectorAuthSignatureEndpointAddress;

    @Inject @ConfigProperty(name = "signature-service.context.mandantId")
    String signatureServiceContextMandantId;

    @Inject @ConfigProperty(name = "signature-service.context.clientSystemId")
    String signatureServiceContextClientSystemId;

    @Inject @ConfigProperty(name = "signature-service.context.workplaceId")
    String signatureServiceContextWorkplaceId;

    @Inject @ConfigProperty(name = "signature-service.context.userId")
    String signatureServiceContextUserId;

    @Inject @ConfigProperty(name = "connector.simulator.titusClientCertificate")
    String titusClientCertificate;

    @Inject @ConfigProperty(name = "event-service.endpointAddress")
    String eventServiceEndpointAddress;

    public String getIdpConnectorTlsCertTrustStore() {

        return idpConnectorTlsCertTrustStore;
    }

    public String getIdpConnectorTlsCertTustStorePwd() {
		return StringUtils.defaultString(
			idpConnectorTlsCertTustStorePwd).trim();
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

    public String getTitusClientCertificate() {
        return titusClientCertificate;
    }

    public String getMandantId() {
        return this.mandantId;
    }

    public void setMandantId(String mandantId) {
        this.mandantId = mandantId;
    }
}
