package health.ere.ps.service.connector.endpoints;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;

@ApplicationScoped
public class EndpointDiscoveryService {
    private final String signatureServiceEndpoint;

    private final String eventServiceEndpoint;

    private final String certificateServiceEndpoint;

    private final String authSignatureServiceEndpoint;

    private final String cardServiceEndpoint;

    public EndpointDiscoveryService(@ConfigProperty(name = "connector.base-uri", defaultValue = "") String connectorBaseUri) throws IOException, ParserConfigurationException {
        // code copied from IdpClient.java
        // TODO support for client cert
        Invocation invocation = ClientBuilder.newClient()
                .target(connectorBaseUri)
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

                // TODO exception handling
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
            e.printStackTrace();
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

    private static String getEndpoint(Node serviceNode) {
        // TODO check for correct base url
        // TODO exception handling
        NodeList versionNodes = ((Element) serviceNode).getElementsByTagName("Versions").item(0).getChildNodes();

        for (int i = 0, n = versionNodes.getLength(); i < n; ++i) {
            Element element = (Element) versionNodes.item(i);

            // TODO exception handling
            return element.getElementsByTagName("EndpointTLS").item(0).getAttributes().getNamedItem("Location").getTextContent();
        }

        throw new IllegalArgumentException("Invalid service node");
    }
}
