package health.ere.ps.service.dgc;

import de.gematik.ws.conn.cardservice.v8.CardInfoType;
import de.gematik.ws.conn.cardservice.v8.Cards;
import de.gematik.ws.conn.eventservice.v7.GetCardsResponse;
import health.ere.ps.config.AppConfig;
import health.ere.ps.model.dgc.InstallationStatus;
import health.ere.ps.service.connector.cards.ConnectorCardsService;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Service to check the general status of the configuration by calling anc checking services depending on the configuration.
 * Its checks the IDP, the GDC-issuer and also the connector with card handle.
 */
@ApplicationScoped
public class StatusService {

    private static final Logger LOG = Logger.getLogger(StatusService.class.getName());

    @Inject
    ConnectorCardsService connectorCardsService;

    @Inject
    AppConfig appConfig;

    private final Client client = ClientBuilder.newBuilder()
            .connectTimeout(1, TimeUnit.MINUTES).build();

    /**
     * Returns a HealthStatus to fill with the different states.
     *
     * This methods creates CompletableFuture to speed up the execution and wait for all to complete. So that there
     * are 3 Futures running: two for requesting the routes of the IDP and the certificate issuer. One Future is used
     * for connector, parameters and SMC-B card status.
     *
     * @return a new filled HealthStatus.
     */
    public InstallationStatus collectStatus() {
        InstallationStatus installationStatus = new InstallationStatus();

        // wait for the requests any parallel them to save time to use max 1 minute.
        CompletableFuture.allOf(
                CompletableFuture.runAsync(() ->
                        setConnectorStates(installationStatus)
                ),
                CompletableFuture.runAsync(() ->
                        installationStatus.setIdentityProviderRouteState(requestStateOfPath(appConfig.getIdpBaseUrl()))
                ),
                CompletableFuture.runAsync(() ->
                        installationStatus.setCertificateServiceRouteState(
                                requestStateOfPath(appConfig.getDigitalGreenCertificateServiceIssuerAPI())
                        )
                )
        ).join();
        return installationStatus;
    }

    /**
     *
     * @param installationStatus to set the status to, must be not null.
     */
    private void setConnectorStates(InstallationStatus installationStatus) {
        try {
            final GetCardsResponse response = connectorCardsService.getConnectorCards();
            installationStatus.setConnectorState(InstallationStatus.State.OK);
            if ("OK".equals(response.getStatus().getResult())) {
                // if there are card handles connector and parameter must be correct:
                installationStatus.setParameterState(InstallationStatus.State.OK);
                List<String> cardHandles = Optional.ofNullable(response.getCards()).map(Cards::getCard)
                        .orElseGet(LinkedList::new)
                        .stream().map(CardInfoType::getCardHandle)
                        .collect(Collectors.toList());
                if (cardHandles.contains(appConfig.getCardHandle())) {
                    installationStatus.setCardState(InstallationStatus.State.OK);
                } else {
                    installationStatus.setCardState(InstallationStatus.State.FAIL);
                }
            } else {
                installationStatus.setParameterState(InstallationStatus.State.FAIL);
                installationStatus.setCardState(InstallationStatus.State.UNKNOWN);
            }

        } catch (Exception e) {
            LOG.log(Level.WARNING, "Could not set connector states", e);
            installationStatus.setConnectorState(InstallationStatus.State.FAIL);
            installationStatus.setParameterState(InstallationStatus.State.UNKNOWN);
            installationStatus.setCardState(InstallationStatus.State.UNKNOWN);
        }
    }

    /**
     * Returns OK if the status is 200 on a get request with the path given as parameter.
     * All other status codes and errors return FAIL.
     * @param path to check via get request must have a URL scheme, must be not null.
     * @return OK if response status code is 200, otherwise FAIL
     */
    private InstallationStatus.State requestStateOfPath(String path) {
        try {
            final Response response = client.target(path)
                    .request("*/*").get();
            return response.getStatus() == 200 ? InstallationStatus.State.OK : InstallationStatus.State.FAIL;
        } catch (Exception e) {
            LOG.severe("Check: route '" + path + "' not callable");
            return InstallationStatus.State.FAIL;
        }
    }
}
