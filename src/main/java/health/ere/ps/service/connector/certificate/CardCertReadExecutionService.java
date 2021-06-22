package health.ere.ps.service.connector.certificate;

import de.gematik.ws.conn.certificateservice.v6.ReadCardCertificate;
import de.gematik.ws.conn.certificateservice.v6.ReadCardCertificateResponse;
import de.gematik.ws.conn.certificateservice.wsdl.v6.CertificateService;
import de.gematik.ws.conn.certificateservice.wsdl.v6.CertificateServicePortType;
import de.gematik.ws.conn.certificateservice.wsdl.v6.FaultMessage;
import de.gematik.ws.conn.certificateservicecommon.v2.CertRefEnum;
import de.gematik.ws.conn.certificateservicecommon.v2.X509DataInfoListType;
import de.gematik.ws.conn.connectorcommon.v5.Status;
import de.gematik.ws.conn.connectorcontext.v2.ContextType;
import health.ere.ps.exception.connector.ConnectorCardCertificateReadException;
import health.ere.ps.service.common.security.SecretsManagerService;
import health.ere.ps.service.connector.endpoints.EndpointDiscoveryService;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Holder;
import java.util.logging.Logger;

@ApplicationScoped
public class CardCertReadExecutionService {

    private static Logger log = Logger.getLogger(CardCertReadExecutionService.class.getName());

    @Inject
    EndpointDiscoveryService endpointDiscoveryService;

    @Inject
    SecretsManagerService secretsManagerService;

    private CertificateServicePortType certificateService;

    static {
        System.setProperty("javax.xml.accessExternalDTD", "all");
    }

    @PostConstruct
    void init() throws Exception {
        certificateService = new CertificateService(getClass().getResource("/CertificateService_v6_0_1.wsdl")).getCertificateServicePort();
        
        // Set endpoint to configured endpoint
        BindingProvider bp = (BindingProvider) certificateService;
        
        bp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
        endpointDiscoveryService.getCertificateServiceEndpointAddress());
        
        if (endpointDiscoveryService.getConnectorTlsCertTrustStore().isPresent()) {
            String path = endpointDiscoveryService.getConnectorTlsCertTrustStore().get();
            secretsManagerService.configureSSLTransportContext(path, endpointDiscoveryService.getConnectorTlsCertTrustStorePwd(),
                    SecretsManagerService.SslContextType.TLS, SecretsManagerService.KeyStoreType.PKCS12, bp);
        }
    }

    /**
     * Reads the AUT certificate of a card.
     *
     * @param invocationContext The invocation context via which the card can be accessed.
     * @param cardHandle        The handle of the card whose AUT certificate is to be read.
     * @return The read AUT certificate.
     */
    public ReadCardCertificateResponse doReadCardCertificate(
            InvocationContext invocationContext, String cardHandle)
                throws ConnectorCardCertificateReadException {
        ContextType contextType = invocationContext.convertToContextType();

        ReadCardCertificate.CertRefList certRefList = new ReadCardCertificate.CertRefList();
        certRefList.getCertRef().add(CertRefEnum.C_AUT);

        Holder<Status> statusHolder = new Holder<Status>();
        Holder<X509DataInfoListType> certHolder = new Holder<X509DataInfoListType>();

        try {
            certificateService.readCardCertificate(cardHandle, contextType, certRefList,
                    statusHolder, certHolder);
        } catch (FaultMessage faultMessage) {
            new ConnectorCardCertificateReadException("Exception reading aut certificate",
                    faultMessage);
        }

        ReadCardCertificateResponse readCardCertificateResponse = new ReadCardCertificateResponse();

        readCardCertificateResponse.setStatus(statusHolder.value);
        readCardCertificateResponse.setX509DataInfoList(certHolder.value);

        return readCardCertificateResponse;
    }

}
