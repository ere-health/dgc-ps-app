package health.ere.ps.service.connector.cards;

import de.gematik.ws.conn.cardservice.v8.CardInfoType;
import de.gematik.ws.conn.cardservice.v8.Cards;
import de.gematik.ws.conn.connectorcontext.v2.ContextType;
import de.gematik.ws.conn.eventservice.v7.GetCards;
import de.gematik.ws.conn.eventservice.v7.GetCardsResponse;
import de.gematik.ws.conn.eventservice.wsdl.v7.EventService;
import de.gematik.ws.conn.eventservice.wsdl.v7.EventServicePortType;
import de.gematik.ws.conn.eventservice.wsdl.v7.FaultMessage;

import org.apache.commons.collections4.CollectionUtils;

import java.util.List;
import java.util.Optional;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.net.ssl.SSLContext;
import javax.xml.ws.BindingProvider;

import health.ere.ps.config.AppConfig;
import health.ere.ps.exception.common.security.SecretsManagerException;
import health.ere.ps.exception.connector.ConnectorCardsException;
import health.ere.ps.service.common.security.SecretsManagerService;
import health.ere.ps.service.common.security.SoapClient;
import health.ere.ps.service.connector.endpoints.EndpointDiscoveryService;
import health.ere.ps.ssl.SSLUtilities;


@ApplicationScoped
public class ConnectorCardsService implements SoapClient {

    @Inject
    AppConfig appConfig;

    private ContextType contextType;
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
    
    @Inject
    EndpointDiscoveryService endpointDiscoveryService;

    @Inject
    SecretsManagerService secretsManagerService;

    @PostConstruct
    void init() throws Exception {
        contextType = new ContextType();
        contextType.setMandantId(appConfig.getMandantId());
        contextType.setClientSystemId(appConfig.getClientSystemId());
        contextType.setWorkplaceId(appConfig.getWorkplaceId());
        contextType.setUserId(appConfig.getUserId());

        eventService = new EventService(getClass().getResource(
                "/EventService.wsdl")).getEventServicePort();
        
        // Set endpoint to configured endpoint
        BindingProvider bp = (BindingProvider) eventService;
        
        bp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
        endpointDiscoveryService.getEventServiceEndpointAddress());
        
        if("false".equals(endpointDiscoveryService.getConnectorVerifyHostname())) {
        	SSLUtilities.trustAllHostnames();
       }
    }

    public GetCardsResponse getConnectorCards()
            throws ConnectorCardsException {
        GetCards parameter = new GetCards();

        parameter.setContext(contextType);

        try {
            return eventService.getCards(parameter);
        } catch (FaultMessage e) {
            throw new ConnectorCardsException("Error getting connector card handles.", e);
        }
    }

    public Optional<List<CardInfoType>> getConnectorCardsInfo() throws ConnectorCardsException {
        GetCardsResponse response = getConnectorCards();
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

    public Optional<String> getConnectorCardHandle(CardHandleType cardHandleType)
            throws ConnectorCardsException {
        Optional<List<CardInfoType>> cardsInfoList = getConnectorCardsInfo();
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

    @Override
    public Optional<BindingProvider> getBindingProvider() {
        return Optional.ofNullable((BindingProvider) eventService);
    }
}
