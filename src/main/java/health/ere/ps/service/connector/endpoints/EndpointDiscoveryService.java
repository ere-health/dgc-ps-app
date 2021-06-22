package health.ere.ps.service.connector.endpoints;

import health.ere.ps.config.AppConfig;
import health.ere.ps.exception.common.security.SecretsManagerException;
import health.ere.ps.service.common.security.SecretsManagerService;
import health.ere.ps.ssl.SSLUtilities;
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
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class EndpointDiscoveryService {
    private static final Logger LOG = Logger.getLogger(EndpointDiscoveryService.class.getName());

    private String signatureServiceEndpoint;

    private String eventServiceEndpoint;

    private String certificateServiceEndpoint;

    private String authSignatureServiceEndpoint;

    private String cardServiceEndpoint;

    @Inject
    AppConfig appConfig;

    @Inject
    SecretsManagerService secretsManagerService;

    @PostConstruct
    void obtainConfiguration() throws IOException, ParserConfigurationException, SecretsManagerException {
        // code copied from IdpClient.java

        // having an ssl context does not interfere with non-ssl connections
        SSLContext sslContext = appConfig.getConnectorTlsCertTrustStore().isPresent() ? secretsManagerService.createSSLContext(appConfig.getConnectorTlsCertTrustStore().get(),
                appConfig.getConnectorTlsCertTustStorePwd(),
                SecretsManagerService.SslContextType.TLS,
                SecretsManagerService.KeyStoreType.PKCS12) : secretsManagerService.createAcceptAllSSLContext();

        ClientBuilder clientBuilder = ClientBuilder.newBuilder()
                .sslContext(sslContext);

        if (!appConfig.isConnectorVerifyHostname()) {
            // disable hostname verification
            clientBuilder = clientBuilder.hostnameVerifier(new SSLUtilities.FakeHostnameVerifier());
        }

        Invocation invocation = clientBuilder.build()
                .target(appConfig.getConnectorBaseUri())
                .path("/connector.sds")
                .request("application/xml")
                .buildGet();

        String signatureServiceEndpoint = "";

        String authSignatureServiceEndpoint = "";

        String cardServiceEndpoint = "";

        String eventServiceEndpoint = "";

        String certificateServiceEndpoint = "";

        try (InputStream inputStream = invocation.invoke(InputStream.class)) {
            Document document = DocumentBuilderFactory.newDefaultInstance()
                    .newDocumentBuilder()
                    .parse(inputStream);

            NodeList nodeList = document.getElementsByTagName("ServiceInformation");

            if (nodeList.getLength() != 1) {
                throw new IllegalArgumentException("Could not find single 'ServiceInformation'-tag");
            }

            NodeList serviceNodeList = nodeList.item(0).getChildNodes();

            for (int i = 0, n = serviceNodeList.getLength(); i < n; ++i) {
                Node node = serviceNodeList.item(i);

                if (!node.hasAttributes() || node.getAttributes().getNamedItem("Name") == null) {
                    break;
                }

                switch (node.getAttributes().getNamedItem("Name").getTextContent()) {
                    case "SignatureService": {
                        signatureServiceEndpoint = getEndpoint(node);
                        break;
                    }
                    case "AuthSignatureService": {
                        authSignatureServiceEndpoint = getEndpoint(node);
                        break;
                    }
                    case "CardService": {
                        cardServiceEndpoint = getEndpoint(node);
                        break;
                    }
                    case "EventService": {
                        eventServiceEndpoint = getEndpoint(node);
                        break;
                    }
                    case "CertificateService": {
                        certificateServiceEndpoint = getEndpoint(node);
                        break;
                    }
                }
            }

        } catch (SAXException | IllegalArgumentException e) {
            LOG.log(Level.SEVERE, "Could not parse connector.sds", e);
        }

        this.signatureServiceEndpoint = signatureServiceEndpoint;
        this.authSignatureServiceEndpoint = authSignatureServiceEndpoint;
        this.cardServiceEndpoint = cardServiceEndpoint;
        this.eventServiceEndpoint = eventServiceEndpoint;
        this.certificateServiceEndpoint = certificateServiceEndpoint;
    }

    public String getSignatureServiceEndpoint() {
        return signatureServiceEndpoint;
    }

    public String getEventServiceEndpoint() {
        return eventServiceEndpoint;
    }

    public String getCertificateServiceEndpoint() {
        return certificateServiceEndpoint;
    }

    public String getAuthSignatureServiceEndpoint() {
        return authSignatureServiceEndpoint;
    }

    public String getCardServiceEndpoint() {
        return cardServiceEndpoint;
    }

    private String getEndpoint(Node serviceNode) {
        NodeList versionsNode = ((Element) serviceNode).getElementsByTagName("Versions");

        if (versionsNode.getLength() == 0) {
            throw new IllegalArgumentException("No version tags found");
        }

        NodeList versionNodes = versionsNode.item(0).getChildNodes();

        for (int i = 0, n = versionNodes.getLength(); i < n; ++i) {
            Element element = (Element) versionNodes.item(i);

            NodeList endpointList = element.getElementsByTagName("EndpointTLS");

            if (endpointList.getLength() != 1 || !endpointList.item(0).hasAttributes()
                    || endpointList.item(0).getAttributes().getNamedItem("Location") == null) {
                continue;
            }

            String location = endpointList.item(0).getAttributes().getNamedItem("Location").getTextContent();

            if (location.startsWith(appConfig.getConnectorBaseUri())) {
                return location;
            }
        }

        throw new IllegalArgumentException("Invalid service node");
    }
}
