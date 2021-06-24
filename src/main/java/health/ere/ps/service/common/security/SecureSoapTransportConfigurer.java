package health.ere.ps.service.common.security;

import health.ere.ps.service.connector.endpoints.EndpointDiscoveryService;
import org.apache.commons.lang3.StringUtils;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.xml.ws.BindingProvider;

import health.ere.ps.exception.common.security.SecretsManagerException;

@Dependent
public class SecureSoapTransportConfigurer {
    @Inject
    EndpointDiscoveryService endpointDiscoveryService;

    private BindingProvider bindingProvider;

    public void init(SoapClient soapClient) {
        this.bindingProvider = soapClient.getBindingProvider().orElse(null);
    }

    public void configureSecureTransport(String endpointAddress) throws SecretsManagerException {
        if (bindingProvider != null && StringUtils.isNotBlank(endpointAddress)) {
            bindingProvider.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                    endpointAddress);

            endpointDiscoveryService.configureSSLTransportContext(bindingProvider);
        }
    }
}
