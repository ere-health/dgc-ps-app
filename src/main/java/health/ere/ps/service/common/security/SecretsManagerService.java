package health.ere.ps.service.common.security;

import com.sun.xml.ws.developer.JAXWSProperties;
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
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class SecretsManagerService {

    private static Logger LOG = Logger.getLogger(SecretsManagerService.class.getName());

    private static final String DATA_TYPE_PKCS = "data:application/x-pkcs12;base64,";

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

    /**
     * Configures the SSL transport for the given {@link BindingProvider}.
     * <p>
     * ATTENTION: if the trust store file path is null, all server certificates are accepted!
     *
     * @param keyStoreFilePath   file path with keystore content; if it is null, no client certificate will be used; prefix with 'jks:' for JKS stores
     * @param keyStorePassword   password to access the client certificate
     * @param sslContextType     type of ssl context; should be TLS
     * @param trustStoreFilePath file path with trust store content; ATTENTION: if it is null, all server certificates are accepted without any validation; prefix with 'jks:' for JKS stores
     * @param trustStorePassword password to access the trust store
     * @param verifyHostnames    set to false to disable hostname verification; ATTENTION: this may lead to MITM attacks
     * @param bp                 BindingProvider for which the ssl context is set up
     * @throws SecretsManagerException will be thrown in case of invalid parameters
     */
    public void configureSSLTransportContext(String keyStoreFilePath,
                                             String keyStorePassword,
                                             SslContextType sslContextType,
                                             String trustStoreFilePath,
                                             String trustStorePassword,
                                             boolean verifyHostnames,
                                             BindingProvider bp)
            throws SecretsManagerException {
        try {
            SSLContext sc = createSSLContext(keyStoreFilePath, keyStorePassword, sslContextType,
                    trustStoreFilePath, trustStorePassword);

            bp.getRequestContext().put(JAXWSProperties.SSL_SOCKET_FACTORY, sc.getSocketFactory());

            if (!verifyHostnames) {
                bp.getRequestContext().put(JAXWSProperties.HOSTNAME_VERIFIER, new SSLUtilities.FakeHostnameVerifier());
            }
        } catch (IOException e) {
            // throw new SecretsManagerException("SSL transport configuration error.", e);
            LOG.log(Level.SEVERE, "Could not configure SecretsManagerService", e);
        }
    }

    /**
     * Create a SSLContext with optional clientcertificate and optional trust store.
     * <p>
     * ATTENTION: if the trust store inputstream is null, all server certificates are accepted!
     *
     * @param keyStoreInputStream   inputstream with keystore content; if the stream is null, no client certificate will be used
     * @param keyStorePassword      password to access the client certificate
     * @param sslContextType        type of ssl context; should be TLS
     * @param keyStoreType          type of the keystore with the client certificate
     * @param trustStoreInputStream inputstream with trust store content; ATTENTION: if the stream is null, all server certificates are accepted without any validation
     * @param trustStorePassword    password to access the trust store
     * @param trustStoreType        type of the trust store with the server certificate
     * @return SSLContext
     * @throws SecretsManagerException will be thrown in case of invalid parameters
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
                LOG.log(Level.WARNING, "Created SSLContext will accept all server certificates without validation");
            }

            sc.init(keyManagers, trustManagers, null );
        } catch (NoSuchAlgorithmException | KeyStoreException | CertificateException | IOException
                | UnrecoverableKeyException | KeyManagementException e) {
            throw new SecretsManagerException("SSL context creation error.", e);
        }

        return sc;
    }

    /**
     * Create a SSLContext with optional clientcertificate and optional trust store.
     * <p>
     * ATTENTION: if the trust store inputstream is null, all server certificates are accepted!
     *
     * @param keyStoreFile       file path with keystore content; if it is null, no client certificate will be used; prefix with 'jks:' for JKS stores; some data-urls are supported
     * @param keyStorePassword   password to access the client certificate
     * @param sslContextType     type of ssl context; should be TLS
     * @param trustStoreFile     file path with trust store content; ATTENTION: if it is null, all server certificates are accepted without any validation; prefix with 'jks:' for JKS stores; some data-urls are supported
     * @param trustStorePassword password to access the trust store
     * @return SSLContext
     * @throws IOException             on accessing the file paths
     * @throws SecretsManagerException will be thrown in case of invalid parameters
     */
    public SSLContext createSSLContext(String keyStoreFile, String keyStorePassword, SslContextType sslContextType,
                                       String trustStoreFile, String trustStorePassword) throws IOException,
            SecretsManagerException {

        if (keyStoreFile == null) {
            if (trustStoreFile == null) {
                return createSSLContext(null, null, sslContextType, null, null, null, null);
            } else {
                try (InputStream trustStoreInputStream = createInputStream(trustStoreFile)) {
                    return createSSLContext(null, null, sslContextType, null,
                            trustStoreInputStream, trustStorePassword.toCharArray(), getKeyStoreType(trustStoreFile));
                } catch(FileNotFoundException ex) {
                    LOG.warning("trustStoreFile: "+trustStoreFile+" was not found. Returning default ssl context");
                    return createSSLContext(null, null, sslContextType, null, null, null, null);
                }
            }
        } else {
            if (trustStoreFile == null) {
                try (InputStream keyStoreInputStream = createInputStream(keyStoreFile)) {
                    return createSSLContext(keyStoreInputStream, keyStorePassword.toCharArray(), sslContextType, getKeyStoreType(keyStoreFile), null, null, null);
                } catch(FileNotFoundException ex) {
                    LOG.warning("keyStoreFile: "+keyStoreFile+" was not found. Returning default ssl context");
                    return createSSLContext(null, null, sslContextType, null, null, null, null);
                }
            } else {
                try (InputStream keyStoreInputStream = createInputStream(keyStoreFile);
                     InputStream trustStoreInputStream = createInputStream(trustStoreFile)) {
                    return createSSLContext(keyStoreInputStream, keyStorePassword.toCharArray(), sslContextType,
                            getKeyStoreType(keyStoreFile), trustStoreInputStream, trustStorePassword.toCharArray(),
                            getKeyStoreType(trustStoreFile));
                } catch(FileNotFoundException ex) {
                    LOG.warning("keyStoreFile: "+keyStoreFile+" or trustStoreFile: "+trustStoreFile+" was not found. Returning default ssl context");
                    return createSSLContext(null, null, sslContextType, null, null, null, null);
                }
            }
        }
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

    /**
     * Get the keystore type from a path string. If the path string has no (known) prefix, {@see KeyStoreType.PKCS12}
     * will be returned.
     *
     * @param path path from which the keystore type is to be extracted
     * @return key store type
     */
    private static KeyStoreType getKeyStoreType(String path) {
        if (path.startsWith("jks:")) {
            return KeyStoreType.JKS;
        } else if (path.startsWith(DATA_TYPE_PKCS)){
            return KeyStoreType.PKCS12;
        } else if (path.startsWith("data:")) {
            throw new IllegalArgumentException("Unsupported data content type in " + path);
        } else {
            return KeyStoreType.PKCS12;
        }
    }

    private static InputStream createInputStream(String path) throws FileNotFoundException {
        String normalizedPath;

        if (path.startsWith("p12:") || path.startsWith("jks:")) {
            normalizedPath = path.substring(4);
        } else {
            normalizedPath = path;
        }

        if (normalizedPath.startsWith(DATA_TYPE_PKCS)) {
            return new ByteArrayInputStream(Base64.getDecoder().decode(normalizedPath.substring(DATA_TYPE_PKCS.length())));
        } else if (normalizedPath.startsWith("data:")) {
            throw new IllegalArgumentException("Unsupported data content type in " + normalizedPath);
        } else {
            return new FileInputStream(normalizedPath);
        }
    }
}
