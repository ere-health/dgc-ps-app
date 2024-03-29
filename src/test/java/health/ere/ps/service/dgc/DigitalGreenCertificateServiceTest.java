package health.ere.ps.service.dgc;

import health.ere.ps.config.AppConfig;
import health.ere.ps.event.RequestBearerTokenFromIdpEvent;
import health.ere.ps.exception.dgc.DigitalGreenCertificateCertificateServiceAuthenticationException;
import health.ere.ps.exception.dgc.DigitalGreenCertificateCertificateServiceException;
import health.ere.ps.exception.dgc.DigitalGreenCertificateException;
import health.ere.ps.exception.dgc.DigitalGreenCertificateInvalidParametersException;
import health.ere.ps.exception.dgc.DigitalGreenCertificateRemoteException;
import health.ere.ps.exception.idp.IdpClientException;
import health.ere.ps.model.dgc.CallContext;
import health.ere.ps.model.dgc.CertificateRequest;
import health.ere.ps.model.dgc.PersonName;
import health.ere.ps.model.dgc.RecoveryCertificateRequest;
import health.ere.ps.model.dgc.RecoveryEntry;
import health.ere.ps.model.dgc.V;
import health.ere.ps.model.dgc.VaccinationCertificateRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.enterprise.event.Event;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DigitalGreenCertificateServiceTest {
    @Mock
    private Event<RequestBearerTokenFromIdpEvent> requestBearerTokenFromIdpEventEvent;

    @InjectMocks
    @Spy
    private DigitalGreenCertificateService digitalGreenCertificateService;

    @Spy
    private AppConfig appConfig;

    @Test
    void issuePdf() throws IOException {
        InputStream inputStream = mock(InputStream.class);

        CertificateRequest certificateRequest = mock(CertificateRequest.class);

        CallContext callContext = mock(CallContext.class);

        byte[] bytes = new byte[]{34, 45, 56};

        Response response = mockAuthenticatedResponse(certificateRequest, callContext);

        when(response.getStatus()).thenReturn(200);
        when(response.readEntity(InputStream.class)).thenReturn(inputStream);
        when(inputStream.readAllBytes()).thenReturn(bytes);

        assertSame(bytes, digitalGreenCertificateService.issuePdf(certificateRequest, callContext));

        verifyNoInteractions(certificateRequest);
        verifyNoInteractions(callContext);
    }

    @Test
    void issuePdfWithCertificateServiceException() {
        when(appConfig.getDigitalGreenCertificateServiceIssuerAPI()).thenReturn("");
        issuePdfWithCertificateServiceException(401, DigitalGreenCertificateCertificateServiceAuthenticationException.class, 100401);
        issuePdfWithCertificateServiceException(403, DigitalGreenCertificateCertificateServiceAuthenticationException.class, 100403);
        issuePdfWithCertificateServiceException(400, DigitalGreenCertificateInvalidParametersException.class, 100400);
        issuePdfWithCertificateServiceException(406, DigitalGreenCertificateInvalidParametersException.class, 100406);
        issuePdfWithCertificateServiceException(500, DigitalGreenCertificateRemoteException.class, 100500);
        // other unknown codes
        issuePdfWithCertificateServiceException(543, DigitalGreenCertificateCertificateServiceException.class, 100543);
    }

    @Test
    void issuePdfWithTokenException() {
        // mock web request
        Client client = mock(Client.class);

        String issuerAPIUrl = "testIssuerAPIUrl";

        digitalGreenCertificateService.client = client;
        when(appConfig.getDigitalGreenCertificateServiceIssuerAPI()).thenReturn(issuerAPIUrl);
        when(client.target(issuerAPIUrl)).thenThrow(new RuntimeException());
        doAnswer((invocation) -> {
            ((RequestBearerTokenFromIdpEvent) invocation.getArgument(0)).setBearerToken("testToken");
            return null;
        }).when(requestBearerTokenFromIdpEventEvent).fire(any());

        // now, the getToken function will be called

        assertThrows(DigitalGreenCertificateRemoteException.class,
                () -> digitalGreenCertificateService.issuePdf(mock(CertificateRequest.class), null));
    }

    @Test
    void issuePdfWithIdpClientExceptionInConnector() {
        String message = "testMessage";

        // first, the getToken function will be called
        doAnswer(invocationOnMock -> {
            RequestBearerTokenFromIdpEvent requestBearerTokenFromIdpEvent = invocationOnMock.getArgument(0);

            requestBearerTokenFromIdpEvent.setException(new IdpClientException(message, IdpClientException.Origin.CONNECTOR));
            return null;
        }).when(requestBearerTokenFromIdpEventEvent).fire(any());

        assertThrows(DigitalGreenCertificateRemoteException.class,
                () -> digitalGreenCertificateService.issuePdf(mock(CertificateRequest.class), null));
    }

    @Test
    void issueVaccinationCertificate() {
        String fn = "testName";

        String gn = "testGivenName";

        LocalDate dob = LocalDate.of(1967, 8, 9);

        String id = "testId";

        String tg = "testTg";

        String vp = "testVp";

        String mp = "testMp";

        String ma = "testMa";

        Integer dn = 1;

        Integer sd = 2;

        LocalDate dt = LocalDate.of(2021, 3, 4);

        VaccinationCertificateRequest vaccinationCertificateRequest = new VaccinationCertificateRequest();

        vaccinationCertificateRequest.setNam(new PersonName(fn, gn));
        vaccinationCertificateRequest.setDob(dob);

        V v = new V();

        v.id = id;
        v.tg = tg;
        v.vp = vp;
        v.mp = mp;
        v.ma = ma;
        v.dn = dn;
        v.sd = sd;
        v.dt = dt;

        vaccinationCertificateRequest.v = Collections.singletonList(v);

        CallContext callContext = mock(CallContext.class);

        byte[] response = new byte[]{123, 124, 125};

        // doReturn because of the null check in issuePdf
        doReturn(response).when(digitalGreenCertificateService).issuePdf(vaccinationCertificateRequest, callContext);

        assertEquals(response, digitalGreenCertificateService.issueVaccinationCertificatePdf(fn, gn, dob, id, tg, vp, mp,
                ma, dn, sd, dt, callContext));
        verifyNoInteractions(callContext);
    }

    @Test
    void issueRecoveryCertificatePdf() {
        String fn = "testFn";

        String gn = "testGn";

        LocalDate dob = LocalDate.of(2000, 1, 1);

        String id = "testId";

        String tg = "testTg";

        LocalDate fr = LocalDate.of(2021, 5, 31);

        LocalDate df = LocalDate.of(2021, 7, 1);

        LocalDate du = LocalDate.of(2022, 7, 1);

        byte[] bytes = new byte[]{56, 67, 78};

        RecoveryCertificateRequest recoveryCertificateRequest = new RecoveryCertificateRequest();

        recoveryCertificateRequest.setNam(new PersonName(fn, gn));
        recoveryCertificateRequest.setDob(dob);

        RecoveryEntry r = new RecoveryEntry();

        r.setId(id);
        r.setTg(tg);
        r.setFr(fr);
        r.setDf(df);
        r.setDu(du);

        recoveryCertificateRequest.setR(Collections.singletonList(r));

        CallContext callContext = mock(CallContext.class);

        // doReturn because of the null check in issuePdf
        doReturn(bytes).when(digitalGreenCertificateService).issuePdf(recoveryCertificateRequest, callContext);

        assertSame(bytes, digitalGreenCertificateService.issueRecoveryCertificatePdf(fn, gn, dob, id, tg, fr, df,
                du, callContext));
        verifyNoInteractions(callContext);
    }

    private void issuePdfWithCertificateServiceException(int responseCode, Class<? extends DigitalGreenCertificateException> expectedExceptionClass,
                                                         int expectedErrorCode) {

        CertificateRequest certificateRequest = mock(CertificateRequest.class);

        CallContext callContext = mock(CallContext.class);

        Response response = mockAuthenticatedResponse(certificateRequest, callContext);

        when(response.getStatus()).thenReturn(responseCode);

        DigitalGreenCertificateException digitalGreenCertificateException = assertThrows(expectedExceptionClass
                ,() -> digitalGreenCertificateService.issuePdf(certificateRequest, callContext));

        assertEquals(expectedErrorCode, digitalGreenCertificateException.getCode());

        verifyNoInteractions(certificateRequest);
        verifyNoInteractions(callContext);
    }

    private Response mockAuthenticatedResponse(CertificateRequest certificateRequest, CallContext callContext) {
        // mock token
        String token = "testToken";

        RequestBearerTokenFromIdpEvent eventContext = new RequestBearerTokenFromIdpEvent();

        eventContext.setCallContext(callContext);

        doAnswer((invocation) -> {
            ((RequestBearerTokenFromIdpEvent) invocation.getArgument(0)).setBearerToken(token);
            return null;
        }).when(requestBearerTokenFromIdpEventEvent).fire(eventContext);

        // mock web request
        Client client = mock(Client.class);

        String issuerAPIUrl = "testIssuerAPIUrl";

        WebTarget webTarget = mock(WebTarget.class);

        Invocation.Builder builder1 = mock(Invocation.Builder.class);

        Invocation.Builder builder2 = mock(Invocation.Builder.class);

        Response response = mock(Response.class);

        digitalGreenCertificateService.client = client;
        when(appConfig.getDigitalGreenCertificateServiceIssuerAPI()).thenReturn(issuerAPIUrl);
        when(client.target(issuerAPIUrl)).thenReturn(webTarget);
        when(webTarget.path("/api/certify/v2/issue")).thenReturn(webTarget);
        when(webTarget.request("application/pdf")).thenReturn(builder1);
        when(builder1.header("Authorization", "Bearer " + token)).thenReturn(builder2);
        when(builder2.post(Entity.entity(certificateRequest, "application/vnd.dgc.v1+json"))).thenReturn(response);

        return response;
    }
}
