package health.ere.ps.service.connector.auth;

import de.gematik.ws.conn.authsignatureservice.wsdl.v7.AuthSignatureService;
import de.gematik.ws.conn.authsignatureservice.wsdl.v7.AuthSignatureServicePortType;
import de.gematik.ws.conn.authsignatureservice.wsdl.v7.FaultMessage;
import de.gematik.ws.conn.connectorcommon.v5.Status;
import de.gematik.ws.conn.connectorcontext.v2.ContextType;
import de.gematik.ws.conn.signatureservice.v7.BinaryDocumentType;
import de.gematik.ws.conn.signatureservice.v7.ExternalAuthenticate;
import de.gematik.ws.conn.signatureservice.v7.ExternalAuthenticateResponse;
import health.ere.ps.exception.common.security.SecretsManagerException;
import health.ere.ps.service.connector.endpoints.EndpointDiscoveryService;
import oasis.names.tc.dss._1_0.core.schema.SignatureObject;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Holder;

@ApplicationScoped
public class SmcbAuthenticatorExecutionService {

    @Inject
    EndpointDiscoveryService endpointDiscoveryService;

    private AuthSignatureServicePortType authSignatureService;

    @PostConstruct
    void init() throws SecretsManagerException {
        authSignatureService = new AuthSignatureService(getClass().getResource(
                "/AuthSignatureService.wsdl")).getAuthSignatureServicePort();
        BindingProvider bp = (BindingProvider) authSignatureService;

        bp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                endpointDiscoveryService.getAuthSignatureServiceEndpointAddress());
        endpointDiscoveryService.configureSSLTransportContext(bp);
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
