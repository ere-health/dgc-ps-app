package health.ere.ps.service.connector.endpoints;

import health.ere.ps.config.AppConfig;
import health.ere.ps.exception.common.security.SecretsManagerException;
import health.ere.ps.service.common.security.SecretsManagerService;
import health.ere.ps.ssl.SSLUtilities;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.net.ssl.SSLContext;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class EndpointDiscoveryService {
    private static final Logger LOG = Logger.getLogger(EndpointDiscoveryService.class.getName());

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

    @ConfigProperty(name = "auth-signature-service.endpoint.address", defaultValue = "")
    String authSignatureServiceEndpointAddress;

    @ConfigProperty(name = "card-service.endpoint.address", defaultValue = "")
    String cardServiceEndpointAddress;

    @ConfigProperty(name = "certificate-service.endpoint.address", defaultValue = "")
    String certificateServiceEndpointAddress;

    @ConfigProperty(name = "event-service.endpoint.address", defaultValue = "")
    String eventServiceEndpointAddress;

    @ConfigProperty(name = "connector.base-uri")
    String connectorBaseUri;

    @ConfigProperty(name = "connector.verify-hostname", defaultValue = "true")
    String connectorVerifyHostname;

    @Inject
    SecretsManagerService secretsManagerService;

    @PostConstruct
    void obtainConfiguration() throws IOException, ParserConfigurationException, SecretsManagerException {
        // code copied from IdpClient.java

        // having an ssl context does not interfere with non-ssl connections
        SSLContext sslContext = connectorTlsCertTrustStore.isPresent() ? secretsManagerService.createSSLContext(connectorTlsCertTrustStore.get(),
                connectorTlsCertTustStorePwd,
                SecretsManagerService.SslContextType.TLS,
                SecretsManagerService.KeyStoreType.PKCS12) : secretsManagerService.createAcceptAllSSLContext();

        ClientBuilder clientBuilder = ClientBuilder.newBuilder()
                .sslContext(sslContext);

        if ("false".equals(connectorVerifyHostname)) {
            // disable hostname verification
            clientBuilder = clientBuilder.hostnameVerifier(new SSLUtilities.FakeHostnameVerifier());
        }

        Invocation invocation = clientBuilder.build()
                .target(connectorBaseUri)
                .path("/connector.sds")
                .request("application/xml")
                .buildGet();

        try (InputStream inputStream = invocation.invoke(InputStream.class)) {
            Document document = DocumentBuilderFactory.newDefaultInstance()
                    .newDocumentBuilder()
                    .parse(inputStream);

            Node serviceInformationNode = getNodeWithTag(document.getDocumentElement(), "ServiceInformation");

            if (serviceInformationNode == null) {
                throw new IllegalArgumentException("Could not find single 'ServiceInformation'-tag");
            }

            NodeList serviceNodeList = serviceInformationNode.getChildNodes();

            for (int i = 0, n = serviceNodeList.getLength(); i < n; ++i) {
                Node node = serviceNodeList.item(i);

                if (!node.hasAttributes() || node.getAttributes().getNamedItem("Name") == null) {
                    break;
                }

                switch (node.getAttributes().getNamedItem("Name").getTextContent()) {
                    case "AuthSignatureService": {
                        authSignatureServiceEndpointAddress = getEndpoint(node);
                        break;
                    }
                    case "CardService": {
                        cardServiceEndpointAddress = getEndpoint(node);
                        break;
                    }
                    case "EventService": {
                        eventServiceEndpointAddress = getEndpoint(node);
                        break;
                    }
                    case "CertificateService": {
                        certificateServiceEndpointAddress = getEndpoint(node);
                        break;
                    }
                }
            }

        } catch (SAXException | IllegalArgumentException e) {
            LOG.log(Level.SEVERE, "Could not parse connector.sds", e);
        }
    }

    public String getAuthSignatureServiceEndpointAddress() {
        return authSignatureServiceEndpointAddress;
    }

    public String getCardServiceEndpointAddress() {
        return cardServiceEndpointAddress;
    }

    public String getCertificateServiceEndpointAddress() {
        return certificateServiceEndpointAddress;
    }

    public String getEventServiceEndpointAddress() {
        return eventServiceEndpointAddress;
    }

    public Optional<String> getConnectorTlsCertTrustStore() {
        return connectorTlsCertTrustStore;
    }

    public String getConnectorTlsCertTrustStorePwd() {
        return StringUtils.defaultString(connectorTlsCertTustStorePwd).trim();
    }

    private String getEndpoint(Node serviceNode) {
        Node versionsNode = getNodeWithTag(serviceNode, "Versions");

        if (versionsNode == null) {
            throw new IllegalArgumentException("No version tags found");
        }

        NodeList versionNodes = versionsNode.getChildNodes();

        for (int i = 0, n = versionNodes.getLength(); i < n; ++i) {
            Node endpointNode = getNodeWithTag(versionNodes.item(i), "EndpointTLS");

            if (endpointNode == null || !endpointNode.hasAttributes()
                    || endpointNode.getAttributes().getNamedItem("Location") == null) {
                continue;
            }

            String location = endpointNode.getAttributes().getNamedItem("Location").getTextContent();

            if (location.startsWith(connectorBaseUri)) {
                return location;
            }
        }

        throw new IllegalArgumentException("Invalid service node");
    }

    private static Node getNodeWithTag(Node node, String tagName) {
        NodeList nodeList = node.getChildNodes();

        for (int i = 0, n = nodeList.getLength(); i < n; ++i) {
            Node childNode = nodeList.item(i);

            // ignore namespace entirely
            if (tagName.equals(childNode.getNodeName()) || childNode.getNodeName().endsWith(":" + tagName)) {
                return childNode;
            }
        }

        return null;
    }
}
