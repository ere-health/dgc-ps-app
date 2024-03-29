package health.ere.ps.service.idp.client;

import com.diffplug.common.base.Errors;
import com.diffplug.common.base.Throwing;

import health.ere.ps.config.AppConfig;
import health.ere.ps.service.connector.cards.ConnectorCardsService;
import health.ere.ps.service.connector.endpoints.EndpointDiscoveryService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jboss.logging.Logger;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.lang.JoseException;

import de.gematik.ws.conn.authsignatureservice.wsdl.v7.AuthSignatureService;
import de.gematik.ws.conn.authsignatureservice.wsdl.v7.AuthSignatureServicePortType;
import de.gematik.ws.conn.authsignatureservice.wsdl.v7.FaultMessage;
import de.gematik.ws.conn.cardservice.wsdl.v8.CardService;
import de.gematik.ws.conn.cardservice.wsdl.v8.CardServicePortType;
import de.gematik.ws.conn.cardservicecommon.v2.PinResultEnum;
import de.gematik.ws.conn.connectorcommon.v5.Status;
import de.gematik.ws.conn.connectorcontext.v2.ContextType;

import java.io.StringReader;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.security.Security;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Holder;

import health.ere.ps.exception.idp.IdpClientException;
import health.ere.ps.exception.idp.IdpException;
import health.ere.ps.exception.idp.IdpJoseException;
import health.ere.ps.model.idp.client.AuthenticationRequest;
import health.ere.ps.model.idp.client.AuthenticationResponse;
import health.ere.ps.model.idp.client.AuthorizationRequest;
import health.ere.ps.model.idp.client.AuthorizationResponse;
import health.ere.ps.model.idp.client.DiscoveryDocumentResponse;
import health.ere.ps.model.idp.client.IdpConstants;
import health.ere.ps.model.idp.client.IdpTokenResult;
import health.ere.ps.model.idp.client.ImpfnachweisAuthenticationRequest;
import health.ere.ps.model.idp.client.ImpfnachweisAuthorizationResponse;
import health.ere.ps.model.idp.client.TokenRequest;
import health.ere.ps.model.idp.client.authentication.AuthenticationChallenge;
import health.ere.ps.model.idp.client.authentication.AuthenticationResponseBuilder;
import health.ere.ps.model.idp.client.brainPoolExtension.BrainpoolAlgorithmSuiteIdentifiers;
import health.ere.ps.model.idp.client.field.ClaimName;
import health.ere.ps.model.idp.client.field.CodeChallengeMethod;
import health.ere.ps.model.idp.client.field.IdpScope;
import health.ere.ps.model.idp.client.token.IdpJwe;
import health.ere.ps.model.idp.client.token.JsonWebToken;
import health.ere.ps.model.idp.crypto.PkiIdentity;
import health.ere.ps.service.idp.client.authentication.UriUtils;
import health.ere.ps.service.idp.crypto.KeyAnalysis;
import health.ere.ps.service.idp.crypto.jose4j.JsonWebSignatureWithExternalAuthentification;

import static org.jose4j.jws.AlgorithmIdentifiers.RSA_PSS_USING_SHA256;

@Dependent
public class IdpClient implements IIdpClient {
    private static final Logger LOG = Logger.getLogger(IdpClient.class.getName());

    @Inject
    AuthenticatorClient authenticatorClient;

    @Inject
    AppConfig appConfig;

    @Inject
    ConnectorCardsService connectorCardsService;

    @Inject
    EndpointDiscoveryService endpointDiscoveryService;

    private String clientId;
    private String redirectUrl;
    private String discoveryDocumentUrl;
    private boolean shouldVerifyState;
    private Set<IdpScope> scopes = Set.of(IdpScope.OPENID);
    private CodeChallengeMethod codeChallengeMethod = CodeChallengeMethod.S256;

    private DiscoveryDocumentResponse discoveryDocumentResponse;

    AuthSignatureServicePortType authSignatureService;
    CardServicePortType cardService;

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    @PostConstruct
    public void initAuthSignatureService() {
        try {
            authSignatureService = new AuthSignatureService(getClass().getResource("/AuthSignatureService.wsdl")).getAuthSignatureServicePort();
            /* Set endpoint to configured endpoint */
            BindingProvider bp = (BindingProvider) authSignatureService;

            bp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                    endpointDiscoveryService.getAuthSignatureServiceEndpointAddress());
            endpointDiscoveryService.configureSSLTransportContext(bp);

            cardService = new CardService(getClass().getResource("/CardService.wsdl")).getCardServicePort();
            /* Set endpoint to configured endpoint */
            bp = (BindingProvider) cardService;
            bp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                    endpointDiscoveryService.getCardServiceEndpointAddress());
            endpointDiscoveryService.configureSSLTransportContext(bp);

        } catch(Exception ex) {
            LOG.error("Could not init AuthSignatureService or CardService for IdpClient", ex);
        }
    }

    public void init(String clientId, String redirectUrl, String discoveryDocumentUrl,
                     boolean shouldVerifyState) {
        this.clientId = clientId;
        this.redirectUrl = redirectUrl;
        this.discoveryDocumentUrl = discoveryDocumentUrl;
        this.shouldVerifyState = shouldVerifyState;
    }

    public IdpClient() {
    }

    /**
     * Create a context type.
     */
    private ContextType createContextType(String mandantId, String clientSystemId, String workplaceId) {
        ContextType contextType = new ContextType();
        contextType.setMandantId(Optional.ofNullable(mandantId).orElseGet(appConfig::getMandantId));
        contextType.setClientSystemId(Optional.ofNullable(clientSystemId).orElseGet(appConfig::getClientSystemId));
        contextType.setWorkplaceId(Optional.ofNullable(workplaceId).orElseGet(appConfig::getWorkplaceId));
        contextType.setUserId(appConfig.getUserId());
        return contextType;
    }

    private String signServerChallenge(final String challengeToSign, final X509Certificate certificate,
                                       final Function<Pair<String, String>, String> contentSigner)
            throws IdpJoseException {
        final JwtClaims claims = new JwtClaims();
        claims.setClaim(ClaimName.NESTED_JWT.getJoseName(), challengeToSign);
        final JsonWebSignature jsonWebSignature = new JsonWebSignature();
        jsonWebSignature.setPayload(claims.toJson());
        jsonWebSignature.setHeader("typ", "JWT");
        jsonWebSignature.setHeader("cty", "NJWT");
        if (KeyAnalysis.isEcKey(certificate.getPublicKey())) {
            // ProviderContext providerCtx = new ProviderContext();
            // providerCtx.getGeneralProviderContext().setKeyPairGeneratorProvider("BC");
            // providerCtx.getGeneralProviderContext().setKeyAgreementProvider("BC");
            // providerCtx.getSuppliedKeyProviderContext().setKeyPairGeneratorProvider("BC");
            // providerCtx.getSuppliedKeyProviderContext().setKeyAgreementProvider("BC");
            // jsonWebSignature.setProviderContext(providerCtx);
            jsonWebSignature.setAlgorithmHeaderValue(
                    BrainpoolAlgorithmSuiteIdentifiers.BRAINPOOL256_USING_SHA256);
        } else {
            jsonWebSignature.setAlgorithmHeaderValue(RSA_PSS_USING_SHA256);
        }
        PublicKey idpPublicKey = getDiscoveryDocumentResponse().getIdpEnc();
        JsonWebToken jwt = new JsonWebToken(
            contentSigner.apply(Pair.of(
                jsonWebSignature.getHeaders().getEncodedHeader(),
                jsonWebSignature.getEncodedPayload())));

        jwt.getHeaderClaims().remove("alg");
        jwt.getHeaderClaims().put("alg", BrainpoolAlgorithmSuiteIdentifiers.BRAINPOOL256_USING_SHA256);
        
        String signedServerChallengeJwt = jwt
                .encrypt(idpPublicKey)
                .getRawString();

        return signedServerChallengeJwt;
    }

    @Override
    public IdpTokenResult login(final PkiIdentity idpIdentity, String mandantId, String clientSystem, String workplace,
                                String cardHandle)
            throws IdpException, IdpClientException, IdpJoseException {
        assertThatIdpIdentityIsValid(idpIdentity);

        String actualCardHandle = Optional.ofNullable(cardHandle).orElseGet(() -> connectorCardsService.getCardHandle(mandantId, clientSystem, workplace));

        ContextType contextType = createContextType(mandantId, clientSystem, workplace);

        return login(idpIdentity.getCertificate(),
            Errors.rethrow().wrap((Throwing.Function<Pair<String, String>, String>) jwtPair -> {
                final JsonWebSignatureWithExternalAuthentification jws =
                        new JsonWebSignatureWithExternalAuthentification(authSignatureService,
                                actualCardHandle,
                                contextType);
                jws.setPayload(new String(Base64.getUrlDecoder().decode(jwtPair.getRight())));
                Optional.ofNullable(jwtPair.getLeft())
                    .map(b64Header -> new String(Base64.getUrlDecoder().decode(b64Header)))
                    .map(com.google.gson.JsonParser::parseString)
                    .map(com.google.gson.JsonElement::getAsJsonObject)
                    .map(com.google.gson.JsonObject::entrySet)
                    .stream()
                    .flatMap(Set::stream)
                    .forEach(entry -> jws.setHeader(entry.getKey(),
                        entry.getValue().getAsString()));
                        
                jws.setCertificateChainHeaderValue(idpIdentity.getCertificate());
                jws.setKey(idpIdentity.getPrivateKey());
                try {
                    return jws.getCompactSerialization();
                } catch (JoseException e) {
                    throw new IdpClientException("Error during encryption", e);
                }
            }), contextType, actualCardHandle);
    }

    private IdpTokenResult login(final X509Certificate certificate,
        final Function<Pair<String, String>, String> contentSigner, ContextType contextType, String cardHandle)
            throws IdpClientException, IdpException, IdpJoseException {
        assertThatClientIsInitialized();

        final String codeVerifier = ClientUtilities.generateCodeVerifier();
        final String nonce = RandomStringUtils.randomAlphanumeric(20);

        // Authorization
        final String state = RandomStringUtils.randomAlphanumeric(20);
        LOG.debug("Performing Authorization with remote-URL: " +
                getDiscoveryDocumentResponse().getAuthorizationEndpoint());
        final AuthorizationResponse authorizationResponse =
            getAuthenticatorClient()
                .doAuthorizationRequest(AuthorizationRequest.builder()
                        .clientId(getClientId())
                        .link(getDiscoveryDocumentResponse().getAuthorizationEndpoint())
                        .codeChallenge(ClientUtilities.generateCodeChallenge(codeVerifier))
                        .codeChallengeMethod(getCodeChallengeMethod())
                        .redirectUri(getRedirectUrl())
                        .state(state)
                        .scopes(getScopes())
                        .nonce(nonce)
                        .build());

        // Authentication
        LOG.debug("Performing Authentication with remote-URL: " +
            getDiscoveryDocumentResponse().getAuthorizationEndpoint());
        AuthenticationResponse authenticationResponse;
        if(authorizationResponse instanceof ImpfnachweisAuthorizationResponse) {
            ImpfnachweisAuthorizationResponse impfnachweisAuthorizationResponse = (ImpfnachweisAuthorizationResponse) authorizationResponse;
            ImpfnachweisAuthenticationRequest authenticationRequest = new ImpfnachweisAuthenticationRequest();
            authenticationRequest.setAuthenticationEndpointUrl(impfnachweisAuthorizationResponse.getLocation());
            
            JsonWebSignatureWithExternalAuthentification jws = new JsonWebSignatureWithExternalAuthentification(
                    authSignatureService, cardHandle, contextType
            );
            byte[] signedChallenge;
            try {
                signedChallenge = jws.signBytes(impfnachweisAuthorizationResponse.getChallenge().getBytes());
            } catch (JoseException e) {
                if (isAccessRequirementsNotFulfilledError(e)) {
                    try {
                        Holder<Status> status = new Holder<>();
                        Holder<PinResultEnum> pinResultEnum = new Holder<>();
                        Holder<BigInteger> error = new Holder<>();
                        cardService.verifyPin(contextType,
                                cardHandle,
                                "PIN.SMC", status, pinResultEnum, error);
                    } catch (Exception e1) {
                        throw new IdpClientException("Could not unlock SMC-B", e1, IdpClientException.Origin.CONNECTOR);
                    }

                    try {
                        // try again
                        signedChallenge = jws.signBytes(impfnachweisAuthorizationResponse.getChallenge().getBytes());
                    } catch (JoseException e1) {
                        if (isAccessRequirementsNotFulfilledError(e1)) {
                            throw new IdpClientException("Could not unlock SMC-B", e1, IdpClientException.Origin.CONNECTOR);
                        } else {
                            throw new IdpJoseException(e);
                        }
                    }
                } else {
                    throw new IdpJoseException(e);
                }
            }
            Client client = ClientBuilder.newClient();
            Response response;
            try {
                response = client.target(impfnachweisAuthorizationResponse.getLocation()).request()
                    .header("x-auth-signed-challenge", new String(Base64.getEncoder().encode(signedChallenge)))
                    .header("x-auth-certificate", new String(Base64.getEncoder().encode(certificate.getEncoded())))
                    .get();
            } catch (CertificateEncodingException e1) {
                throw new IdpClientException(e1);
            }

            if (response.getStatus() != 302) {
                throw new IdpClientException("Unexpected response from " + URI.create(impfnachweisAuthorizationResponse.getLocation()).getHost(), IdpClientException.Origin.IDP);
            }

            URL url;
            try {
                url = new URL(response.getHeaderString("Location").replace("connector://", "http://"));
            } catch (MalformedURLException e) {
                throw new IdpException(e);
            }
            List<Map.Entry<String, String>> list = Pattern.compile("&")
                .splitAsStream(url.getQuery())
                .map(s -> Arrays.copyOf(s.split("=", 2), 2))
                .map(o -> Map.entry(decode(o[0]), decode(o[1])))
                .collect(Collectors.toList());
            String code = null;
            String sessionState = null;
            for(Map.Entry<String, String> m : list) {
                if("code".equals(m.getKey())) {
                    code = m.getValue();
                } else if("session_state".equals(m.getKey())) {
                    sessionState = m.getValue();
                }
            }

            String tokenUrl = getDiscoveryDocumentResponse().getTokenEndpoint();

            Form form = new Form();
            form.param("grant_type", "authorization_code")
                .param("redirect_uri", "connector://authenticated")
                .param("client_id", getClientId())
                .param("session_state", sessionState)
                .param("code_verifier", codeVerifier)
                .param("code", code);
     
            WebTarget target = client.target(tokenUrl);
            response.close();
            response = target.
                        request(MediaType.APPLICATION_FORM_URLENCODED)
                        .accept(MediaType.APPLICATION_JSON)
                        .post(Entity.form(form));   

            JsonObject jsonObject = getJsonObject(response);
            
            final String tokenType = jsonObject.getString("token_type");
            final int expiresIn = jsonObject.getInt("expires_in");
    
            return IdpTokenResult.builder()
                    .tokenType(tokenType)
                    .expiresIn(expiresIn)
                    .accessToken(new JsonWebToken(jsonObject.getString("access_token")))
                    .idToken(new JsonWebToken(jsonObject.getString("id_token")))
                    .build();

        } else {
            authenticationResponse =
                getAuthenticatorClient()
                    .performAuthentication(AuthenticationRequest.builder()
                            .authenticationEndpointUrl(
                                getDiscoveryDocumentResponse().getAuthorizationEndpoint())
                            .signedChallenge(new IdpJwe(
                                signServerChallenge(
                                    authorizationResponse.getAuthenticationChallenge().getChallenge().getRawString(),
                                    certificate, contentSigner)))
                            .build());
        }
        if (isShouldVerifyState()) {
            final String stringInTokenUrl = UriUtils
                .extractParameterValue(authenticationResponse.getLocation(), "state");
            if (!state.equals(stringInTokenUrl)) {
                throw new IdpClientException("state-parameter unexpected changed");
            }
        }

        // get Token
        LOG.debug("Performing getToken with remote-URL: " +
                getDiscoveryDocumentResponse().getTokenEndpoint());
        return getAuthenticatorClient().retrieveAccessToken(TokenRequest.builder()
                .tokenUrl(getDiscoveryDocumentResponse().getTokenEndpoint())
                .clientId(getClientId())
                .code(authenticationResponse.getCode())
                .ssoToken(authenticationResponse.getSsoToken())
                .redirectUrl(getRedirectUrl())
                .codeVerifier(codeVerifier)
                .idpEnc(getDiscoveryDocumentResponse().getIdpEnc())
                .build());
    }

    public JsonObject getJsonObject(Response response) {
        String jsonString = response.readEntity(String.class);
        JsonObject jsonObject = JsonObject.EMPTY_JSON_OBJECT;

        if(StringUtils.isNotBlank(jsonString)) {
            try (JsonReader jsonReader = Json.createReader(new StringReader(jsonString))) {
                jsonObject = jsonReader.readObject();
            }
        }

        return jsonObject;
    }

    private static String decode(final String encoded) {
        return Optional.ofNullable(encoded)
                       .map(e -> URLDecoder.decode(e, StandardCharsets.UTF_8))
                       .orElse(null);
    }

    public IdpTokenResult loginWithSsoToken(final IdpJwe ssoToken) throws IdpClientException, IdpException {
        assertThatClientIsInitialized();

        final String codeVerifier = ClientUtilities.generateCodeVerifier();
        final String nonce = RandomStringUtils.randomAlphanumeric(20);

        // Authorization
        final String state = RandomStringUtils.randomAlphanumeric(20);
        LOG.debug("Performing Authorization with remote-URL: " +
            getDiscoveryDocumentResponse().getAuthorizationEndpoint());
        final AuthorizationResponse authorizationResponse =
            getAuthenticatorClient()
                .doAuthorizationRequest(AuthorizationRequest.builder()
                        .clientId(getClientId())
                        .link(getDiscoveryDocumentResponse().getAuthorizationEndpoint())
                        .codeChallenge(ClientUtilities.generateCodeChallenge(codeVerifier))
                        .codeChallengeMethod(getCodeChallengeMethod())
                        .redirectUri(getRedirectUrl())
                        .state(state)
                        .scopes(getScopes())
                        .nonce(nonce)
                        .build());

        // Authentication
        final String ssoChallengeEndpoint = getDiscoveryDocumentResponse().getAuthorizationEndpoint().replace(
            IdpConstants.BASIC_AUTHORIZATION_ENDPOINT, IdpConstants.SSO_ENDPOINT);
        LOG.debug("Performing Sso-Authentication with remote-URL: " + ssoChallengeEndpoint);
        final AuthenticationResponse authenticationResponse =
            getAuthenticatorClient()
                .performAuthenticationWithSsoToken(AuthenticationRequest.builder()
                        .authenticationEndpointUrl(ssoChallengeEndpoint)
                        .ssoToken(ssoToken.getRawString())
                        .challengeToken(authorizationResponse.getAuthenticationChallenge().getChallenge())
                        .build());
        if (isShouldVerifyState()) {
            final String stringInTokenUrl = UriUtils
                .extractParameterValue(authenticationResponse.getLocation(), "state");
            if (!state.equals(stringInTokenUrl)) {
                throw new IdpClientException("state-parameter unexpected changed");
            }
        }

        // Get Token
        LOG.debug("Performing getToken with remote-URL: " +
                getDiscoveryDocumentResponse().getTokenEndpoint());

        return getAuthenticatorClient().retrieveAccessToken(TokenRequest.builder()
                .tokenUrl(getDiscoveryDocumentResponse().getTokenEndpoint())
                .clientId(getClientId())
                .code(authenticationResponse.getCode())
                .ssoToken(ssoToken.getRawString())
                .redirectUrl(getRedirectUrl())
                .codeVerifier(codeVerifier)
                .idpEnc(getDiscoveryDocumentResponse().getIdpEnc())
                .build());
    }

    private void assertThatIdpIdentityIsValid(final PkiIdentity idpIdentity) {
        Objects.requireNonNull(idpIdentity);
        Objects.requireNonNull(idpIdentity.getCertificate());
        // Objects.requireNonNull(idpIdentity.getPrivateKey());
    }

    private IdpJwe signChallenge(
        final AuthenticationChallenge authenticationChallenge,
        final PkiIdentity idpIdentity) throws IdpJoseException {
        return AuthenticationResponseBuilder.builder().build()
            .buildResponseForChallenge(authenticationChallenge, idpIdentity)
            .getSignedChallenge()
            .encrypt(getDiscoveryDocumentResponse().getIdpEnc());
    }

    private void assertThatClientIsInitialized() throws IdpClientException {
        LOG.debug("Verifying IDP-Client initialization...");
        if (getDiscoveryDocumentResponse() == null ||
            StringUtils.isEmpty(getDiscoveryDocumentResponse().getAuthorizationEndpoint()) ||
            StringUtils.isEmpty(getDiscoveryDocumentResponse().getTokenEndpoint())) {
            throw new IdpClientException(
                "IDP-Client not initialized correctly! Call .initialize() before performing an actual operation.");
        }
    }

    @Override
    public IIdpClient initializeClient() throws IdpClientException, IdpException, IdpJoseException {
        LOG.info("Initializing using url: " + getDiscoveryDocumentUrl());
        setDiscoveryDocumentResponse(getAuthenticatorClient()
            .retrieveDiscoveryDocument(getDiscoveryDocumentUrl()));
        return this;
    }

    public void verifyAuthTokenToken(final IdpTokenResult authToken) throws IdpJoseException {
        authToken.getAccessToken()
            .verify(getDiscoveryDocumentResponse().getIdpSig().getPublicKey());
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getRedirectUrl() {
        return redirectUrl;
    }

    public void setRedirectUrl(String redirectUrl) {
        this.redirectUrl = redirectUrl;
    }

    public String getDiscoveryDocumentUrl() {
        return discoveryDocumentUrl;
    }

    public void setDiscoveryDocumentUrl(String discoveryDocumentUrl) {
        this.discoveryDocumentUrl = discoveryDocumentUrl;
    }

    public boolean isShouldVerifyState() {
        return shouldVerifyState;
    }

    public void setShouldVerifyState(boolean shouldVerifyState) {
        this.shouldVerifyState = shouldVerifyState;
    }

    public Set<IdpScope> getScopes() {
        return scopes;
    }

    public void setScopes(Set<IdpScope> scopes) {
        this.scopes = scopes;
    }

    public AuthenticatorClient getAuthenticatorClient() {
        return authenticatorClient;
    }

    public void setAuthenticatorClient(AuthenticatorClient authenticatorClient) {
        this.authenticatorClient = authenticatorClient;
    }

    public CodeChallengeMethod getCodeChallengeMethod() {
        return codeChallengeMethod;
    }

    public void setCodeChallengeMethod(CodeChallengeMethod codeChallengeMethod) {
        this.codeChallengeMethod = codeChallengeMethod;
    }

    public DiscoveryDocumentResponse getDiscoveryDocumentResponse() {
        return discoveryDocumentResponse;
    }

    public void setDiscoveryDocumentResponse(DiscoveryDocumentResponse discoveryDocumentResponse) {
        this.discoveryDocumentResponse = discoveryDocumentResponse;
    }

    private static boolean isAccessRequirementsNotFulfilledError(JoseException e) {
        if (e.getCause() instanceof FaultMessage) {
            FaultMessage authSignatureFaultMessage = (FaultMessage) e.getCause();
            // Zugriffsbedingungen nicht erfüllt
            return authSignatureFaultMessage.getFaultInfo().getTrace().stream().anyMatch(t -> t.getCode().equals(BigInteger.valueOf(4085L)));
        }

        return false;
    }
}