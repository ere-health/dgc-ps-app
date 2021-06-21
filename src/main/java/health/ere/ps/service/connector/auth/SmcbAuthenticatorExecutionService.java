package health.ere.ps.service.connector.auth;

import de.gematik.ws.conn.authsignatureservice.wsdl.v7.AuthSignatureService;
import de.gematik.ws.conn.authsignatureservice.wsdl.v7.AuthSignatureServicePortType;
import de.gematik.ws.conn.authsignatureservice.wsdl.v7.FaultMessage;
import de.gematik.ws.conn.connectorcommon.v5.Status;
import de.gematik.ws.conn.connectorcontext.v2.ContextType;
import de.gematik.ws.conn.signatureservice.v7.BinaryDocumentType;
import de.gematik.ws.conn.signatureservice.v7.ExternalAuthenticate;
import de.gematik.ws.conn.signatureservice.v7.ExternalAuthenticateResponse;
import health.ere.ps.config.AppConfig;
import health.ere.ps.exception.common.security.SecretsManagerException;
import health.ere.ps.service.common.security.SecretsManagerService;
import oasis.names.tc.dss._1_0.core.schema.SignatureObject;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.spi.DeploymentException;
import javax.inject.Inject;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Holder;

@ApplicationScoped
public class SmcbAuthenticatorExecutionService {

    @Inject
    SecretsManagerService secretsManagerService;

    @Inject
    AppConfig appConfig;

    private AuthSignatureServicePortType authSignatureService;

    @PostConstruct
    void init() throws SecretsManagerException {
        authSignatureService = new AuthSignatureService(getClass().getResource(
                "/AuthSignatureService_v7_4_1.wsdl")).getAuthSignatureServicePort();
        BindingProvider bp = (BindingProvider) authSignatureService;

        bp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                appConfig.getIdpConnectorAuthSignatureEndpointAddress());

        secretsManagerService.configureSSLTransportContext(
                appConfig.getConnectorTlsCertTrustStore()
                        .orElseThrow(() -> new DeploymentException("No connector tsl cert trust certificate present")),
                appConfig.getConnectorTlsCertTustStorePwd(), SecretsManagerService.SslContextType.TLS,
                SecretsManagerService.KeyStoreType.PKCS12, bp);
    }

    public ExternalAuthenticateResponse doExternalAuthenticate(String cardHandle, ContextType contextType,
                                       ExternalAuthenticate.OptionalInputs optionalInputs,
                                       BinaryDocumentType binaryDocumentType) throws FaultMessage {

        Holder<Status> statusHolder = new Holder<Status>();
        Holder<SignatureObject> signatureObjectHolder = new Holder<>();
        ExternalAuthenticateResponse response = new ExternalAuthenticateResponse();

        authSignatureService.externalAuthenticate(cardHandle, contextType, optionalInputs,
                binaryDocumentType, statusHolder, signatureObjectHolder);

        response.setStatus(statusHolder.value);
        response.setSignatureObject(signatureObjectHolder.value);

        return response;
    }
}
