package health.ere.ps.service.connector.certificate;

import org.apache.commons.lang3.ArrayUtils;
import org.bouncycastle.crypto.CryptoException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.inject.Inject;

import health.ere.ps.config.AppConfig;
import health.ere.ps.exception.connector.ConnectorCardCertificateReadException;
import health.ere.ps.service.connector.cards.ConnectorCardsService;
import health.ere.ps.service.idp.crypto.CryptoLoader;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@QuarkusTest
class CardCertificateReaderServiceTest {
    @Inject
    CardCertificateReaderService cardCertificateReaderService;

    @Inject
    ConnectorCardsService connectorCardsService;

    @Inject
    AppConfig appConfig;

    @Test
    @EnabledIfEnvironmentVariable(named = "TEST_ENVIRONMENT", matches = "RU",
            disabledReason = "Only works with a connector")
    void test_Successful_ReadCardCertificate_API_Call() throws ConnectorCardCertificateReadException {
        Assertions.assertTrue(ArrayUtils.isNotEmpty(
                cardCertificateReaderService.readCardCertificate(appConfig.getClientId(),
                        appConfig.getClientSystemId(),
                        appConfig.getWorkplaceId(), connectorCardsService.getCardHandle())),
                "Smart card certificate was retrieved");
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "TEST_ENVIRONMENT", matches = "RU",
            disabledReason = "Only works with a connector")
    void test_Successful_X509Certificate_Creation_From_ReadCardCertificate_API_Call()
            throws ConnectorCardCertificateReadException, CertificateException,
            CryptoException {
        byte[] base64_Decoded_Asn1_DER_Format_CertBytes =
                cardCertificateReaderService.readCardCertificate(appConfig.getClientId(),
                        appConfig.getClientSystemId(),
                        appConfig.getWorkplaceId(), connectorCardsService.getCardHandle());
        Assertions.assertTrue(ArrayUtils.isNotEmpty(base64_Decoded_Asn1_DER_Format_CertBytes),
                "Smart card certificate was retrieved");

        X509Certificate x509Certificate = CryptoLoader.getCertificateFromAsn1DERCertBytes(
                base64_Decoded_Asn1_DER_Format_CertBytes);

        x509Certificate.checkValidity();
    }
}