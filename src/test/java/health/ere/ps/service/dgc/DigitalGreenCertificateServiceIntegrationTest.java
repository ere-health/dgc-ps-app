package health.ere.ps.service.dgc;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import health.ere.ps.LocalOfflineQuarkusTestProfile;
import health.ere.ps.config.AppConfig;
import health.ere.ps.event.RequestBearerTokenFromIdpEvent;
import health.ere.ps.exception.dgc.DigitalGreenCertificateRemoteException;
import health.ere.ps.exception.idp.IdpClientException;
import health.ere.ps.model.dgc.CallContext;
import health.ere.ps.model.dgc.CertificateRequest;
import health.ere.ps.model.dgc.PersonName;
import health.ere.ps.model.dgc.RecoveryCertificateRequest;
import health.ere.ps.model.dgc.RecoveryEntry;
import health.ere.ps.model.dgc.V;
import health.ere.ps.model.dgc.VaccinationCertificateRequest;
import health.ere.ps.service.idp.IdPService;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.junit.mockito.InjectMock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.net.URL;
import java.time.LocalDate;
import java.util.Collections;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

@QuarkusTest
@TestProfile(LocalOfflineQuarkusTestProfile.class)
class DigitalGreenCertificateServiceIntegrationTest {
    @ApplicationScoped
    private static class RequestBearerTokenEventObserver {
        private String token;

        private CallContext callContext;

        private Exception exception;

        public void addToken(@Observes RequestBearerTokenFromIdpEvent requestBearerTokenFromIdpEvent) {
            assertEquals(callContext, requestBearerTokenFromIdpEvent.getCallContext());
            requestBearerTokenFromIdpEvent.setBearerToken(token);
            requestBearerTokenFromIdpEvent.setException(exception);
        }

        public void setToken(String token) {
            this.token = token;
        }

        public void setCallContext(CallContext callContext) {
            this.callContext = callContext;
        }

        public void setException(Exception exception) {
            this.exception = exception;
        }
    }

    @Inject
    private DigitalGreenCertificateService digitalGreenCertificateService;

    @Inject
    private RequestBearerTokenEventObserver requestBearerTokenEventObserver;

    // mock service to disable the @Observes annotation
    @InjectMock
    private IdPService idPService;

    @Inject
    AppConfig appConfig;

    private WireMockServer wireMockServer;

    private MappingBuilder serverMatcher;
    private byte[] response;

    @BeforeEach
    void startup() throws Exception {

        URL url = new URL(appConfig.getDigitalGreenCertificateServiceIssuerAPI());

        if (!"localhost".equals(url.getHost())) {
            throw new RuntimeException("Testing is only possible for localhost urls");
        }
        wireMockServer = new WireMockServer(wireMockConfig().port(url.getPort()).bindAddress("localhost"));
        wireMockServer.start();

        // mock setup for token
        String token = "testToken";

        requestBearerTokenEventObserver.setToken(token);
        requestBearerTokenEventObserver.setException(null);
        requestBearerTokenEventObserver.setCallContext(null);

        response = new byte[]{1, 2, 4, 8, 16};
        serverMatcher = post("/issue/api/certify/v2/issue")
                .withHeader("Authorization", equalTo("Bearer " + token))
                .withHeader("Accept", equalTo("application/pdf"))
                .withHeader("Content-Type", equalTo("application/vnd.dgc.v1+json"))
                .willReturn(ok().withBody(response));
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        wireMockServer.stop();
        // TODO wireMockServer need time to terminate, it exception.
        Thread.sleep(2000);
    }

    @Test
    void issueVaccinationCertificate() {

        // mock response

        LocalDate dob = LocalDate.of(1921, 1, 1);
        String name = "Testname Lastname";
        String givenName = "Testgiven Name";
        String id = "testId";
        String tg = "testTg";
        String vp = "testVp";
        String mp = "testMp";
        String ma = "testMa";
        int dn = 123;
        int sd = 345;
        LocalDate dt = LocalDate.of(2021, 1, 1);
        // we may use a mock here since the call context is supposed to be passed to the event only with no interaction
        CallContext callContext = mock(CallContext.class);

        requestBearerTokenEventObserver.setCallContext(callContext);

        wireMockServer.stubFor(serverMatcher
                .withRequestBody(equalToJson("{\"nam\":{" +
                        "\"fn\": \"" + name + "\"," +
                        "\"gn\": \"" + givenName + "\"" +
                        "}," +
                        "\"dob\": \"" + dob + "\"," +
                        "\"v\": [{" +
                        "\"id\": \"" + id + "\"," +
                        "\"tg\": \"" + tg + "\"," +
                        "\"vp\": \"" + vp + "\"," +
                        "\"mp\": \"" + mp + "\"," +
                        "\"ma\": \"" + ma + "\"," +
                        "\"dn\": " + dn + "," +
                        "\"sd\": " + sd + "," +
                        "\"dt\": \"" + dt + "\"" +
                        "}"+
                        "]}"))
        );

        final V v = new V();
        v.id = id;
        v.tg = tg;
        v.vp = vp;
        v.mp = mp;
        v.ma = ma;
        v.dn = dn;
        v.sd = sd;
        v.dt = dt;

        final VaccinationCertificateRequest vaccinationCertificateRequest = new VaccinationCertificateRequest();
        vaccinationCertificateRequest.setNam(new PersonName(name, givenName));
        vaccinationCertificateRequest.setDob(dob);
        vaccinationCertificateRequest.v = Collections.singletonList(v);

        byte[] actualResponse = digitalGreenCertificateService.issuePdf(vaccinationCertificateRequest, callContext);
        assertNotNull(actualResponse);
        assertArrayEquals(response, actualResponse);

        verifyNoInteractions(callContext);
    }

    @Test
    void issueRecoveryCertificate() {
        // mock response

        final String testId = "testId";
        final String testTg = "testTg";
        final String testDateFr = "2023-01-01";
        final String testDateDu = "2022-01-01";
        final String testDateDf = "2021-01-01";
        final String testDataDob = "1921-01-01";
        final String name = "Testname";
        final String givenName = "Testgiven Name";

        final String jsonContentResponse = "{\"nam\":{" +
                "\"fn\": \"" + name + "\"," +
                "\"gn\": \"" + givenName + "\"" +
                "}," +
                "\"dob\": \"" + testDataDob + "\"," +
                "\"r\": [{" +
                "\"id\": \"" + testId + "\"," +
                "\"tg\": \"" + testTg + "\"," +
                "\"fr\": \"" + testDateFr + "\"," +
                "\"du\": \"" + testDateDu + "\"," +
                "\"df\": \"" + testDateDf + "\""+
                "}]}";

        // we may use a mock here since the call context is supposed to be passed to the event only with no interaction
        CallContext callContext = mock(CallContext.class);

        requestBearerTokenEventObserver.setCallContext(callContext);

        wireMockServer.stubFor(serverMatcher
                .withRequestBody(equalToJson(jsonContentResponse))
                .withRequestBody(matchingJsonPath("r.length()", equalTo("1")))
        );

        final RecoveryEntry recoveryEntry = new RecoveryEntry();
        recoveryEntry.setId(testId);
        recoveryEntry.setTg(testTg);
        recoveryEntry.setFr(LocalDate.parse(testDateFr));
        recoveryEntry.setDu(LocalDate.parse(testDateDu));
        recoveryEntry.setDf(LocalDate.parse(testDateDf));

        final RecoveryCertificateRequest certificateRequest = new RecoveryCertificateRequest();
        certificateRequest.setNam(new PersonName(name, givenName));
        certificateRequest.setDob(LocalDate.parse(testDataDob));
        certificateRequest.addRItem(recoveryEntry);

        final byte[] actualResponse = digitalGreenCertificateService.issuePdf(certificateRequest, callContext);
        assertNotNull(actualResponse);
        assertArrayEquals(response, actualResponse);

        verifyNoInteractions(callContext);
    }

    @Test
    void issueRecoveryCertificateFromIndividualParams() {
        // mock response

        final String testId = "testId";
        final String testTg = "testTg";
        final String testDateFr = "2023-01-01";
        final String testDateDu = "2022-01-01";
        final String testDateDf = "2021-01-01";
        final String testDataDob = "1921-01-01";
        final String name = "Testname";
        final String givenName = "Testgiven Name";

        final String jsonContentResponse = "{\"nam\":{" +
                "\"fn\": \"" + name + "\"," +
                "\"gn\": \"" + givenName + "\"" +
                "}," +
                "\"dob\": \"" + testDataDob + "\"," +
                "\"r\": [{" +
                "\"id\": \"" + testId + "\"," +
                "\"tg\": \"" + testTg + "\"," +
                "\"fr\": \"" + testDateFr + "\"," +
                "\"du\": \"" + testDateDu + "\"," +
                "\"df\": \"" + testDateDf + "\""+
                "}]}";

        // we may use a mock here since the call context is supposed to be passed to the event only with no interaction
        CallContext callContext = mock(CallContext.class);

        requestBearerTokenEventObserver.setCallContext(callContext);

        wireMockServer.stubFor(serverMatcher
                .withRequestBody(equalToJson(jsonContentResponse))
                .withRequestBody(matchingJsonPath("r.length()", equalTo("1")))
        );

        final byte[] actualResponse = digitalGreenCertificateService.issueRecoveryCertificatePdf(name, givenName,
                LocalDate.parse(testDataDob), testId, testTg, LocalDate.parse(testDateFr),
                LocalDate.parse(testDateDf), LocalDate.parse(testDateDu), callContext);
        assertNotNull(actualResponse);
        assertArrayEquals(response, actualResponse);

        verifyNoInteractions(callContext);
    }

    @Test
    void issueVaccinationCertificateWithIdpClientException() {
        String message = "testMessage";

        requestBearerTokenEventObserver.setException(new IdpClientException(message, IdpClientException.Origin.CONNECTOR));

        assertThrows(DigitalGreenCertificateRemoteException.class,
                () -> digitalGreenCertificateService.issuePdf(new CertificateRequest() {}, null));
    }
}
