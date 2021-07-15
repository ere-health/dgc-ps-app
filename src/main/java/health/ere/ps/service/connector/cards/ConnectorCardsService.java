package health.ere.ps.service.connector.cards;

import de.gematik.ws.conn.cardservice.v8.CardInfoType;
import de.gematik.ws.conn.cardservice.v8.Cards;
import de.gematik.ws.conn.connectorcontext.v2.ContextType;
import de.gematik.ws.conn.eventservice.v7.GetCards;
import de.gematik.ws.conn.eventservice.v7.GetCardsResponse;
import de.gematik.ws.conn.eventservice.wsdl.v7.EventService;
import de.gematik.ws.conn.eventservice.wsdl.v7.EventServicePortType;
import de.gematik.ws.conn.eventservice.wsdl.v7.FaultMessage;

import health.ere.ps.exception.common.security.SecretsManagerException;
import health.ere.ps.service.connector.endpoints.EndpointDiscoveryService;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;
import java.util.Optional;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.xml.ws.BindingProvider;

import health.ere.ps.config.AppConfig;
import health.ere.ps.exception.connector.ConnectorCardsException;
import health.ere.ps.service.common.security.SoapClient;
import org.eclipse.microprofile.config.inject.ConfigProperty;


@ApplicationScoped
public class ConnectorCardsService implements SoapClient {

    @Inject
    AppConfig appConfig;

    /**
     * Card handle for the connector.
     * See ConnectorCommons.xsd in gematik specification.
     */
    @Inject @ConfigProperty(name = "connector.card.handle")
    Optional<String> cardHandle;

    @Inject
    EndpointDiscoveryService endpointDiscoveryService;

    private EventServicePortType eventService;

    public enum CardHandleType {
        EGK("EGK"),
        HBA_Q_SIG("HBA-qSig"),
        HBA("HBA"),
        SMC_B("SMC-B"),
        HSM_B("HSM-B"),
        SMC_KT("SMC-KT"),
        KVK("KVK"),
        ZOD_2_0("ZOD_2.0"),
        UNKNOWN("UNKNOWN"),
        HBA_X("HBAx"),
        SM_B("SM-B");

        private String cardHandleType;

        CardHandleType(String cardHandleType) {
            this.cardHandleType = cardHandleType;
        }

        public String getCardHandleType() {
            return cardHandleType;
        }
    }

    public String getCardHandle(String mandantId, String clientSystemId, String workplaceId) {
        if (cardHandle.isPresent()) {
            return cardHandle.get();
        } else {
            try {
                return this.getConnectorCardHandle(CardHandleType.SMC_B, mandantId, clientSystemId, workplaceId).orElseThrow();
            } catch (ConnectorCardsException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public String getCardHandle() {
        return getCardHandle(null, null, null);
    }

    @PostConstruct
    void init() throws SecretsManagerException {
        eventService = new EventService(getClass().getResource(
                "/EventService.wsdl")).getEventServicePort();

        // Set endpoint to configured endpoint; copied from CardCertReadExecutionService
        BindingProvider bp = (BindingProvider) eventService;

        bp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                endpointDiscoveryService.getEventServiceEndpointAddress());
        endpointDiscoveryService.configureSSLTransportContext(bp);
    }

    public GetCardsResponse getConnectorCards(String mandantId, String clientSystemId, String workplaceId)
            throws ConnectorCardsException {
        GetCards parameter = new GetCards();

        ContextType contextType = new ContextType();

        contextType.setMandantId(Optional.ofNullable(mandantId).orElseGet(appConfig::getMandantId));
        contextType.setClientSystemId(Optional.ofNullable(clientSystemId).orElseGet(appConfig::getClientSystemId));
        contextType.setWorkplaceId(Optional.ofNullable(workplaceId).orElseGet(appConfig::getWorkplaceId));
        contextType.setUserId(appConfig.getUserId());

        parameter.setContext(contextType);

        try {
            return eventService.getCards(parameter);
        } catch (FaultMessage e) {
            throw new ConnectorCardsException("Error getting connector card handles.", e);
        }
    }

    public GetCardsResponse getConnectorCards() throws ConnectorCardsException {
        return getConnectorCards(null, null, null);
    }

    public Optional<List<CardInfoType>> getConnectorCardsInfo(String mandantId, String clientSystemId, String workplaceId) throws ConnectorCardsException {
        GetCardsResponse response = getConnectorCards(mandantId, clientSystemId, workplaceId);
        List<CardInfoType> cardHandleTypeList = null;

        if(response != null) {
            Cards cards = response.getCards();

            cardHandleTypeList = cards.getCard();

            if(CollectionUtils.isEmpty(cardHandleTypeList)) {
                throw new ConnectorCardsException("Error. Did not receive and card handle data.");
            }
        }

        return Optional.ofNullable(cardHandleTypeList);
    }

    public Optional<String> getConnectorCardHandle(CardHandleType cardHandleType, String mandantId, String clientSystemId, String workplaceId)
            throws ConnectorCardsException {
        Optional<List<CardInfoType>> cardsInfoList = getConnectorCardsInfo(mandantId, clientSystemId, workplaceId);
        String cardHandle = null;

        if(cardsInfoList.isPresent()) {
            Optional<CardInfoType> cardHndl =
                    cardsInfoList.get().stream().filter(ch ->
                            ch.getCardType().value().equalsIgnoreCase(
                            cardHandleType.getCardHandleType())).findFirst();
            if(cardHndl.isPresent()) {
                cardHandle = cardHndl.get().getCardHandle();
            } else {
                throw new ConnectorCardsException(String.format("No card handle found for card " +
                        "handle type %s", cardHandleType.getCardHandleType()));
            }
        }

        return Optional.ofNullable(cardHandle);
    }

    public Optional<String> getConnectorCardHandle(CardHandleType cardHandleType) throws ConnectorCardsException {
        return getConnectorCardHandle(cardHandleType, null, null, null);
    }

    @Override
    public Optional<BindingProvider> getBindingProvider() {
        return Optional.ofNullable((BindingProvider) eventService);
    }
}
