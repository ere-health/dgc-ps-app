package health.ere.ps.service.common.security;

import org.apache.commons.lang3.StringUtils;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.xml.ws.BindingProvider;

import health.ere.ps.exception.common.security.SecretsManagerException;

@Dependent
public class SecureSoapTransportConfigurer {
    @Inject
    SecretsManagerService secretsManagerService;

    private BindingProvider bindingProvider;

    public void init(SoapClient soapClient) {
        this.bindingProvider = soapClient.getBindingProvider().orElse(null);
    }

    public void configureSecureTransport(String endpointAddress,
                                         SecretsManagerService.SslContextType sslContextType,
                                         String tlsCertKeyStore,
                                         String tlsCertKeyStorePassword,
                                         String tlsCertTrustStore,
                                         String tlsCertTrustStorePassword) throws SecretsManagerException {
        if (bindingProvider != null && StringUtils.isNotBlank(endpointAddress)) {
            bindingProvider.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                    endpointAddress);

            secretsManagerService.configureSSLTransportContext(tlsCertKeyStore, tlsCertKeyStorePassword, sslContextType,
                    tlsCertTrustStore, tlsCertTrustStorePassword, bindingProvider);
        }
    }
}
