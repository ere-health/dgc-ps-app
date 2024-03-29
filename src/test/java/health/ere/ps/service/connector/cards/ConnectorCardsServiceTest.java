package health.ere.ps.service.connector.cards;

import health.ere.ps.service.common.security.SecureSoapTransportConfigurer;
import health.ere.ps.service.connector.endpoints.EndpointDiscoveryService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.logging.Logger;

import javax.inject.Inject;

import health.ere.ps.exception.common.security.SecretsManagerException;
import health.ere.ps.exception.connector.ConnectorCardsException;
import health.ere.ps.service.common.security.SecretsManagerService;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@QuarkusTest
class ConnectorCardsServiceTest {
    private static final Logger LOG = Logger.getLogger(ConnectorCardsServiceTest.class.getName());

    @Inject
    ConnectorCardsService connectorCardsService;

    @Inject
    SecureSoapTransportConfigurer secureSoapTransportConfigurer;

    @Inject
    EndpointDiscoveryService endpointDiscoveryService;

    @BeforeEach
    void configureSecureTransport() throws SecretsManagerException {
        secureSoapTransportConfigurer.init(connectorCardsService);

        secureSoapTransportConfigurer.configureSecureTransport(
                endpointDiscoveryService.getEventServiceEndpointAddress());
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "TEST_ENVIRONMENT", matches = "RU", disabledReason = "Only works with a connector")
    void test_Successful_Retrieval_Of_SMC_B_Card_Handle() throws ConnectorCardsException {
        Optional<String> cardHandle = connectorCardsService.getConnectorCardHandle(
                ConnectorCardsService.CardHandleType.SMC_B);
        Assertions.assertTrue(cardHandle.isPresent(), "Card handle result is present");

        LOG.info("Card handle: " + cardHandle.get());
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "TEST_ENVIRONMENT", matches = "RU", disabledReason = "Only works with a connector")
    void test_Successful_Retrieval_Of_eHBA_Card_Handle() throws ConnectorCardsException {
        Optional<String> cardHandle = connectorCardsService.getConnectorCardHandle(
                ConnectorCardsService.CardHandleType.HBA);
        Assertions.assertTrue(cardHandle.isPresent(), "Card handle result is present");

        LOG.info("Card handle: " + cardHandle.get());
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "TEST_ENVIRONMENT", matches = "RU", disabledReason = "Only works with a connector")
    void test_Unsuccessful_Retrieval_Of_Unsupported_KVK_Card_Handle() {
        Assertions.assertThrows(ConnectorCardsException.class,
                () -> {
                    Optional<String> cardHandle = connectorCardsService.getConnectorCardHandle(
                            ConnectorCardsService.CardHandleType.KVK);
                }, "ConnectorCardsException thrown for missing or unsupported card handle");
    }
}