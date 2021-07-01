package health.ere.ps.resource.dgc;

import health.ere.ps.exception.dgc.DigitalGreenCertificateCertificateServiceAuthenticationException;
import health.ere.ps.exception.dgc.DigitalGreenCertificateCertificateServiceException;
import health.ere.ps.exception.dgc.DigitalGreenCertificateException;
import health.ere.ps.exception.dgc.DigitalGreenCertificateInternalAuthenticationException;
import health.ere.ps.exception.dgc.DigitalGreenCertificateInvalidParametersException;
import health.ere.ps.exception.dgc.DigitalGreenCertificateRemoteException;
import health.ere.ps.model.dgc.CallContext;
import health.ere.ps.model.dgc.CertificateRequest;
import health.ere.ps.model.dgc.DigitalGreenCertificateError;
import health.ere.ps.model.dgc.PersonName;
import health.ere.ps.model.dgc.RecoveryCertificateRequest;
import health.ere.ps.model.dgc.RecoveryEntry;
import health.ere.ps.model.dgc.V;
import health.ere.ps.model.dgc.VaccinationCertificateRequest;
import health.ere.ps.service.dgc.DigitalGreenCertificateService;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectSpy;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.LocalDate;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

@QuarkusTest
class DigitalGreenCertificateResourceIntegrationTest {

    @InjectSpy
    private DigitalGreenCertificateService service;

    @TestHTTPEndpoint(DigitalGreenCertificateResource.class)
    @TestHTTPResource
    private URL url;

    @Test
    void issueVaccinationCertificate() throws Exception {

        // model copied from DigitalGreenCertificateServiceIntegrationTest
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
        String mandantId = "testMandantId";
        String clientSystem = "testClientSystem";
        String workplace = "testWorkplace";
        String cardHandle = "testCardHandle";

        byte[] pdf = new byte[]{1, 2, 3, 4};

        final String requestBody = "{\"nam\":{" +
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
                "}]}";

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

        Client client = ClientBuilder.newBuilder().build();

        // mock response
        final ArgumentCaptor<CertificateRequest> ac = ArgumentCaptor.forClass(CertificateRequest.class);
        // doReturn because of the null check in issuePdf
        doReturn(pdf).when(service).issuePdf(ac.capture(), eq(new CallContext(mandantId, clientSystem, workplace
                , cardHandle)));

        Response response = client.target(url.toURI().resolve("v2/issue"))
                .request("application/pdf")
                .headers(createHeaders(mandantId, clientSystem, workplace, cardHandle))
                .post(Entity.json(requestBody));

        // then
        assertEquals(200, response.getStatus());
        assertArrayEquals(pdf, response.readEntity(byte[].class));

        CertificateRequest value = ac.getValue();
        assertEquals(value, vaccinationCertificateRequest);

        // after
        client.close();
    }

    @Test
    void issueRecoveryCertificate() throws Exception {

        // model copied from DigitalGreenCertificateServiceIntegrationTest
        final String testId = "testId";
        final String testTg = "testTg";
        final String testDateFr = "2023-01-01";
        final String testDateDu = "2022-01-01";
        final String testDateDf = "2021-01-01";
        final String testDataDob = "1921-01-01";
        final String familyName = "Testname Lastname";
        final String givenName = "Testgiven Name";
        final String mandantId = "testMandantId";
        final String clientSystem = "testClientSystem";
        final String workplace = "testWorkplace";
        final String cardHandle = "testCardHandle";

        final String requestBody = "{\"nam\":{" +
                "\"fn\": \"" + familyName + "\"," +
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
        Client client = ClientBuilder.newBuilder().build();

        final RecoveryEntry recoveryEntry = new RecoveryEntry();
        recoveryEntry.setId(testId);
        recoveryEntry.setTg(testTg);
        recoveryEntry.setFr(LocalDate.parse(testDateFr));
        recoveryEntry.setDu(LocalDate.parse(testDateDu));
        recoveryEntry.setDf(LocalDate.parse(testDateDf));

        final RecoveryCertificateRequest certificateRequest = new RecoveryCertificateRequest();
        certificateRequest.setNam(new PersonName(familyName, givenName));
        certificateRequest.setDob(LocalDate.parse(testDataDob));
        certificateRequest.addRItem(recoveryEntry);

        byte[] pdf = new byte[]{34, 56};

        // mock response
        final ArgumentCaptor<CertificateRequest> ac = ArgumentCaptor.forClass(CertificateRequest.class);
        // doReturn because of the null check in issuePdf
        doReturn(pdf).when(service).issuePdf(ac.capture(), eq(new CallContext(mandantId, clientSystem, workplace,
                cardHandle)));

        Response response = client.target(url.toURI().resolve("v2/recovered"))
                .request("application/pdf")
                .headers(createHeaders(mandantId, clientSystem, workplace, cardHandle))
                .post(Entity.json(requestBody));

        // then
        assertEquals(200, response.getStatus());
        assertArrayEquals(pdf, response.readEntity(byte[].class));

        CertificateRequest value = ac.getValue();
        assertEquals(value, certificateRequest);

        // after
        client.close();
    }

    @Test
    void issueRecoveryCertificateFromIndividualParams() throws Exception {

        // model copied from DigitalGreenCertificateServiceIntegrationTest
        final String testId = "testId";
        final String testTg = "testTg";
        final String testDateFr = "2023-01-01";
        final String testDateDu = "2022-01-01";
        final String testDateDf = "2021-01-01";
        final String testDataDob = "1921-01-01";
        final String familyName = "Testname Lastname";
        final String givenName = "Testgiven Name";
        final String mandantId = "testMandantId";
        final String clientSystem = "testClientSystem";
        final String workplace = "testWorkplace";
        final String cardHandle = "testCardHandle";

        Client client = ClientBuilder.newBuilder().build();

        final RecoveryEntry recoveryEntry = new RecoveryEntry();
        recoveryEntry.setId(testId);
        recoveryEntry.setTg(testTg);
        recoveryEntry.setFr(LocalDate.parse(testDateFr));
        recoveryEntry.setDu(LocalDate.parse(testDateDu));
        recoveryEntry.setDf(LocalDate.parse(testDateDf));

        final RecoveryCertificateRequest certificateRequest = new RecoveryCertificateRequest();
        certificateRequest.setNam(new PersonName(familyName, givenName));
        certificateRequest.setDob(LocalDate.parse(testDataDob));
        certificateRequest.addRItem(recoveryEntry);

        byte[] pdf = new byte[]{34, 56};

        // mock response
        final ArgumentCaptor<CertificateRequest> ac = ArgumentCaptor.forClass(CertificateRequest.class);
        // doReturn because of the null check in issuePdf
        doReturn(pdf).when(service).issuePdf(ac.capture(), eq(new CallContext(mandantId, clientSystem, workplace,
                cardHandle)));

        Response response = client.target(url.toURI().resolve("v2/recovered"))
                .queryParam("fn", familyName)
                .queryParam("gn", givenName)
                .queryParam("dob", testDataDob)
                .queryParam("id", testId)
                .queryParam("tg", testTg)
                .queryParam("fr", testDateFr)
                .queryParam("df", testDateDf)
                .queryParam("du", testDateDu)
                .request("application/pdf")
                .headers(createHeaders(mandantId, clientSystem, workplace, cardHandle))
                .get();

        // then
        assertEquals(200, response.getStatus());
        assertArrayEquals(pdf, response.readEntity(byte[].class));

        CertificateRequest value = ac.getValue();
        assertEquals(value, certificateRequest);

        // after
        client.close();
    }

    @Test
    void testExceptionMapper() throws URISyntaxException {
        int code = 123456;

        testExceptionMapper(new DigitalGreenCertificateInvalidParametersException(code, ""), 400, code);
        testExceptionMapper(new DigitalGreenCertificateInternalAuthenticationException(), 401, 200401);
        testExceptionMapper(new DigitalGreenCertificateCertificateServiceAuthenticationException(code, ""), 403, code);
        testExceptionMapper(new DigitalGreenCertificateCertificateServiceException(code, ""), 500, code);
        testExceptionMapper(new DigitalGreenCertificateRemoteException(code, ""), 501, code);
        testExceptionMapper(new RuntimeException("test"), 500, -1);
    }

    private void testExceptionMapper(RuntimeException runtimeException, int expectedResponseCode, int expectedErrorCode)
            throws URISyntaxException {

        doThrow(runtimeException).when(service).issuePdf(any(), any());

        Client client = ClientBuilder.newBuilder().build();

        Response response = client.target(url.toURI().resolve("v2/recovered"))
                .request("application/pdf")
                .post(Entity.json("{}"));

        assertEquals(expectedResponseCode, response.getStatus());

        DigitalGreenCertificateError digitalGreenCertificateError = response.readEntity(DigitalGreenCertificateError.class);

        assertEquals(expectedErrorCode, digitalGreenCertificateError.getCode());
    }

    private static MultivaluedMap<String, Object> createHeaders(String mandantId, String clientSystem,
                                                                String workplace, String cardHandle) {
        MultivaluedHashMap<String, Object> multivaluedHashMap = new MultivaluedHashMap<>();

        multivaluedHashMap.put("X-Mandant", Collections.singletonList(mandantId));
        multivaluedHashMap.put("X-ClientSystem", Collections.singletonList(clientSystem));
        multivaluedHashMap.put("X-Workplace", Collections.singletonList(workplace));
        multivaluedHashMap.put("X-CardHandle", Collections.singletonList(cardHandle));

        return multivaluedHashMap;
    }
}
