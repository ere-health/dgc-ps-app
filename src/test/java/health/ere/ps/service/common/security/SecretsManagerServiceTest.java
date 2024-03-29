package health.ere.ps.service.common.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Base64;

import javax.inject.Inject;
import javax.net.ssl.SSLContext;

import health.ere.ps.exception.common.security.SecretsManagerException;
import health.ere.ps.model.idp.crypto.PkiKeyResolver;
import io.quarkus.test.junit.QuarkusTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@ExtendWith(PkiKeyResolver.class)
class SecretsManagerServiceTest {
    @Inject
    SecretsManagerService secretsManagerService;

    static char[] trustStorePassword;
    static final String TITUS_IDP_TRUST_STORE = "ps_erp_incentergy_01.p12";

    Path tempTrustStoreFile;

    @BeforeAll
    public static void init() {
        trustStorePassword = "password1".toCharArray();
    }
    
    @BeforeEach
    void setUp() throws IOException {
        tempTrustStoreFile = Files.createTempFile("temp-truststore", ".dat");
    }

    @AfterEach
    void tearDown() throws IOException {
        Files.deleteIfExists(tempTrustStoreFile);
    }

    @Test
    void createTrustStore()
            throws SecretsManagerException {
        KeyStore ks =
                secretsManagerService.createTrustStore(tempTrustStoreFile.toFile().getAbsolutePath(),
                SecretsManagerService.KeyStoreType.PKCS12, trustStorePassword);
        assertTrue(ks.getType().equalsIgnoreCase(
                SecretsManagerService.KeyStoreType.PKCS12.getKeyStoreType()));
    }

    @Test
    void initializeTrustStoreFromFile() throws IOException, CertificateException,
            KeyStoreException, NoSuchAlgorithmException {
        int ksSize;

        try(InputStream is = getClass().getResourceAsStream("/certs/" + TITUS_IDP_TRUST_STORE)) {
            KeyStore ks =
                    secretsManagerService.getKeyStoreFromInputStream(is,
                            SecretsManagerService.KeyStoreType.PKCS12, "00".toCharArray());
            ksSize = ks.size();
        }

        assertTrue(ksSize > 0);
    }

    @Test
    void createSSLContextFromDataUrl() throws IOException, SecretsManagerException {
        try (InputStream inputStream = getClass().getResourceAsStream("/certs/" + TITUS_IDP_TRUST_STORE)) {
            assertNotNull(inputStream);

            String dataUrl = "data:application/x-pkcs12;base64," + Base64.getEncoder().encodeToString(inputStream.readAllBytes());

            SSLContext sslContext = secretsManagerService.createSSLContext(dataUrl, "00", SecretsManagerService.SslContextType.TLS, null, null);

            assertNotNull(sslContext);
        }
    }
}
