package health.ere.ps.service.common.security;

import health.ere.ps.exception.common.security.SecretsManagerException;
import health.ere.ps.service.idp.crypto.CryptoLoader;
import health.ere.ps.ssl.SSLUtilities;
import org.bouncycastle.crypto.CryptoException;

import javax.crypto.KeyGenerator;
import javax.enterprise.context.ApplicationScoped;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.xml.ws.BindingProvider;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Key;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class SecretsManagerService {

    private static Logger log = Logger.getLogger(SecretsManagerService.class.getName());

    // TODO: remove enum
    public enum SslContextType {
        SSL("SSL"), TLS("TLS");

        SslContextType(String sslContextType) {
            this.sslContextType = sslContextType;
        }

        private String sslContextType;

        public String getSslContextType() {
            return sslContextType;
        }
    }

    public enum KeyStoreType {
        JKS("jks"), PKCS12("pkcs12");

        KeyStoreType(String keyStoreType) {
            this.keyStoreType = keyStoreType;
        }

        private String keyStoreType;

        public String getKeyStoreType() {
            return keyStoreType;
        }
    }

    public SecretsManagerService() {

    }

    public KeyStore createTrustStore(String trustStoreFilePath,
                               KeyStoreType keyStoreType,
                               char[] keyStorePassword)
            throws SecretsManagerException {
        KeyStore ks;

        try {
            ks = KeyStore.getInstance(keyStoreType.getKeyStoreType());

            ks.load(null, keyStorePassword);

            Path tsFile = Paths.get(trustStoreFilePath);

            if(!Files.exists(tsFile)) {
                if(tsFile.toFile().getParentFile() != null) {
                    tsFile.toFile().getParentFile().mkdirs();
                    tsFile.toFile().createNewFile();
                }
            }

            try(FileOutputStream trustStoreOutputStream = new FileOutputStream(trustStoreFilePath)) {
                ks.store(trustStoreOutputStream, keyStorePassword);
            }
        } catch (CertificateException | KeyStoreException | IOException | NoSuchAlgorithmException e) {
            throw new SecretsManagerException("Error creating trust store.", e);
        }

        return ks;
    }

    public KeyStore saveTrustedCertificate(String trustStoreFilePath, char[] keyStorePassword,
                                           String certificateAlias,
                                       Certificate certificate) throws CertificateException,
            KeyStoreException, IOException, NoSuchAlgorithmException {
        KeyStore trustStore = initializeTrustStoreFromFile(trustStoreFilePath,
                keyStorePassword);

        trustStore.setCertificateEntry(certificateAlias, certificate);

        try(FileOutputStream trustStoreOutputStream = new FileOutputStream(trustStoreFilePath)) {
            trustStore.store(trustStoreOutputStream, keyStorePassword);
        }

        return trustStore;
    }

    public KeyStore saveTrustedCertificate(String trustStoreFilePath, char[] keyStorePassword,
                                           String certificateAlias,
                                         byte[] certBytes) throws SecretsManagerException {
        try {
                X509Certificate x509Certificate =
                        CryptoLoader.getCertificateFromAsn1DERCertBytes(certBytes);

                return saveTrustedCertificate(trustStoreFilePath, keyStorePassword,
                        certificateAlias, x509Certificate);
            } catch (CertificateException | IOException | KeyStoreException |
                NoSuchAlgorithmException | CryptoException e) {
                throw new SecretsManagerException("Error saving certificate in trust store", e);
            }
    }

    public void deleteTrustStore(String trustStorePath) throws IOException {
        Files.delete(Paths.get(trustStorePath));
    }

    public KeyStore initializeTrustStoreFromFile(String trustStoreFilePath,
                                        char[] keyStorePassword)
            throws KeyStoreException, CertificateException, IOException, NoSuchAlgorithmException {
        KeyStore trustStore = KeyStore.getInstance(new File(trustStoreFilePath), keyStorePassword);
        
        return trustStore;
    }

    public KeyStore getKeyStoreFromInputStream(InputStream keyStoreInputStream,
                                               KeyStoreType keyStoreType,
                                               char[] keyStorePassword)
            throws KeyStoreException, CertificateException, IOException, NoSuchAlgorithmException {
        KeyStore trustStore = KeyStore.getInstance(keyStoreType.getKeyStoreType());
        trustStore.load(keyStoreInputStream, keyStorePassword);

        return trustStore;
    }

    public void configureSSLTransportContext(String keyStoreFilePath,
                                             String keyStorePassword,
                                             SslContextType sslContextType,
                                             KeyStoreType keyStoreType,
                                             String trustStoreFilePath,
                                             String trustStorePassword,
                                             KeyStoreType trustStoreType,
                                             BindingProvider bp)
            throws SecretsManagerException {
        try {
            SSLContext sc = createSSLContext(keyStoreFilePath, keyStorePassword, sslContextType, keyStoreType,
                    trustStoreFilePath, trustStorePassword, trustStoreType);

            bp.getRequestContext().put("com.sun.xml.ws.transport.https.client.SSLSocketFactory",
                    sc.getSocketFactory());

        } catch (IOException e) {
            // throw new SecretsManagerException("SSL transport configuration error.", e);
            log.log(Level.SEVERE, "Could not configure SecretsManagerService", e);
        }
    }

    /**
     * ATTENTION: if the trust store inputstream is null, all server certificates are accepted!
     *
     * @param keyStoreInputStream if the stream is null, no
     * @param keyStorePassword
     * @param sslContextType
     * @param keyStoreType
     * @param trustStoreInputStream ATTENTION:
     * @param trustStorePassword
     * @param trustStoreType
     * @return
     * @throws SecretsManagerException
     */
    private SSLContext createSSLContext(InputStream keyStoreInputStream, char[] keyStorePassword,
                                    SslContextType sslContextType, KeyStoreType keyStoreType,
                                       InputStream trustStoreInputStream, char[] trustStorePassword,
                                       KeyStoreType trustStoreType)
            throws SecretsManagerException {
        SSLContext sc;

        try {
            sc = SSLContext.getInstance(sslContextType.getSslContextType());

            KeyManager[] keyManagers;

            if (keyStoreInputStream != null) {

                KeyManagerFactory kmf =
                        KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());

                KeyStore ks = getKeyStoreFromInputStream(keyStoreInputStream, keyStoreType, keyStorePassword);

                kmf.init(ks, keyStorePassword);

                keyManagers = kmf.getKeyManagers();
            } else {
                keyManagers = null;
            }

            TrustManager[] trustManagers;

            if (trustStoreInputStream != null) {
                TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());

                KeyStore trustKeyStore = getKeyStoreFromInputStream(trustStoreInputStream, trustStoreType,
                        trustStorePassword);

                trustManagerFactory.init(trustKeyStore);
                trustManagers = trustManagerFactory.getTrustManagers();
            } else {
                // we have no choice but to trust all certificates if none is supplied since the connectors
                // do not have a 'properly' signed certificate
                trustManagers = new TrustManager[]{new SSLUtilities.FakeX509TrustManager()};
            }

            sc.init(keyManagers, trustManagers, null );
        } catch (NoSuchAlgorithmException | KeyStoreException | CertificateException | IOException
                | UnrecoverableKeyException | KeyManagementException e) {
            throw new SecretsManagerException("SSL context creation error.", e);
        }

        return sc;
    }

    public SSLContext createSSLContext(String keyStoreFile, String keyStorePassword, SslContextType sslContextType,
                                       KeyStoreType keyStoreType, String trustStoreFile, String trustStorePassword,
                                       KeyStoreType trustStoreType) throws IOException, SecretsManagerException {

        if (keyStoreFile == null) {
            try (FileInputStream trustStoreInputStream = new FileInputStream(trustStoreFile)) {
                return createSSLContext(null, null, sslContextType, null,
                        trustStoreInputStream, trustStorePassword.toCharArray(), trustStoreType);
            }
        } else {
            if (trustStoreFile == null) {
                try (FileInputStream keyStoreInputStream = new FileInputStream(keyStoreFile)) {
                    return createSSLContext(keyStoreInputStream, keyStorePassword.toCharArray(), sslContextType, keyStoreType, null, null, null);
                }
            } else {
                try (FileInputStream keyStoreInputStream = new FileInputStream(keyStoreFile); FileInputStream trustStoreInputStream = new FileInputStream(trustStoreFile)) {
                    return createSSLContext(keyStoreInputStream, keyStorePassword.toCharArray(), sslContextType,
                            keyStoreType, trustStoreInputStream, trustStorePassword.toCharArray(), trustStoreType);
                }
            }
        }
        // support for null keystore

    }

    public Key generateRandomKey(String keyGenAlgorithm) throws SecretsManagerException {
        //Creating a KeyGenerator object
        KeyGenerator keyGen = null;
        try {
            keyGen = KeyGenerator.getInstance(keyGenAlgorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new SecretsManagerException("Error generating random crypto key.", e);
        }

        //Creating a SecureRandom object
        SecureRandom secRandom = new SecureRandom();

        //Initializing the KeyGenerator
        keyGen.init(secRandom);

        //Creating/Generating a key
        Key key = keyGen.generateKey();

        return key;
    }
}
