package health.ere.ps.resource.dgc;

import health.ere.ps.model.dgc.CallContext;
import health.ere.ps.model.dgc.CertificateRequest;
import health.ere.ps.model.dgc.RecoveryCertificateRequest;
import health.ere.ps.model.dgc.VaccinationCertificateRequest;
import health.ere.ps.service.dgc.DigitalGreenCertificateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.ws.rs.core.Response;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DigitalGreenCertificateResourceTest {
    @Spy
    private DigitalGreenCertificateService digitalGreenCertificateService;

    @InjectMocks
    private DigitalGreenCertificateResource digitalGreenCertificateResource;

    final byte[] bytes = new byte[]{123};

    @BeforeEach
    public void init() {
        // start with doReturn because null check.
        // TODO: move to test method with proper checks
        doReturn(bytes).when(digitalGreenCertificateService).issuePdf(any(), any());
    }

    @Test
    void issueVaccinationCertificate() {
        VaccinationCertificateRequest vaccinationCertificateRequest = mock(VaccinationCertificateRequest.class);

        String mandantId = "testMandantId";

        String clientSystem = "testClientSystem";

        String workplace = "testWorkplace";

        String cardHandle = "testCardHandle";

        Response response = digitalGreenCertificateResource.issue(vaccinationCertificateRequest, mandantId,
                clientSystem, workplace, cardHandle);

        verify(digitalGreenCertificateService, times(1)).issuePdf(vaccinationCertificateRequest,
                new CallContext(mandantId, clientSystem, workplace, cardHandle));
        assertNotNull(response);
        assertEquals(response.getStatus(), 200);
        assertEquals(response.getMediaType().toString(), "application/pdf");
        assertSame(response.getEntity(), bytes);

        verifyNoInteractions(vaccinationCertificateRequest);
    }

    @Test
    void issueVaccinationCertificateFromParameters() {
        String fn = "testFn";

        String gn = "testGn";

        LocalDate dob = LocalDate.of(1970, 10, 15);

        String id = "testId";

        String tg = "testTg";

        String vp = "testVp";

        String mp = "testMp";

        String ma = "testMa";

        Integer dn = 12;

        Integer sd = 34;

        LocalDate dt = LocalDate.of(2020, 10, 10);

        String mandantId = "testMandantId";

        String clientSystem = "testClientSystem";

        String workplace = "testWorkplace";

        String cardHandle = "testCardHandle";

        byte[] bytes = new byte[]{123};

        when(digitalGreenCertificateService.issueVaccinationCertificatePdf(fn, gn, dob, id, tg, vp, mp, ma, dn, sd, dt,
                new CallContext(mandantId, clientSystem, workplace, cardHandle))).thenReturn(bytes);

        Response response = digitalGreenCertificateResource.issue(fn, gn, dob, id, tg, vp, mp, ma, dn, sd, dt,
                mandantId, clientSystem, workplace, cardHandle);

        assertNotNull(response);
        assertEquals(response.getStatus(), 200);
        assertEquals(response.getMediaType().toString(), "application/pdf");
        assertSame(response.getEntity(), bytes);
    }

    @Test
    void issueRecoveryCertificate() {
        RecoveryCertificateRequest certificateRequest = mock(RecoveryCertificateRequest.class);

        String mandantId = "testMandantId";

        String clientSystem = "testClientSystem";

        String workplace = "testWorkplace";

        String cardHandle = "testCardHandle";

        Response response = digitalGreenCertificateResource.recovered(certificateRequest, mandantId, clientSystem,
                workplace, cardHandle);

        verify(digitalGreenCertificateService, times(1)).issuePdf(certificateRequest,
                new CallContext(mandantId, clientSystem, workplace, cardHandle));

        assertNotNull(response);
        assertEquals(response.getStatus(), 200);
        assertEquals(response.getMediaType().toString(), "application/pdf");
        assertSame(response.getEntity(), bytes);

        verifyNoInteractions(certificateRequest);
    }

    @Test
    void issueRecoveryCertificateFromParameters() {
        String fn = "testFn";

        String gn = "testGn";

        LocalDate dob = LocalDate.of(1990, 4, 5);

        String id = "testId";

        String tg = "testTg";

        LocalDate fr = LocalDate.of(2021, 6, 10);

        String is = "testIs";

        LocalDate df = LocalDate.of(2021, 7, 1);

        LocalDate du = LocalDate.of(2022, 1, 1);

        String mandantId = "testMandantId";

        String clientSystem = "testClientSystem";

        String workplace = "testWorkplace";

        String cardHandle = "testCardHandle";

        byte[] bytes = new byte[] {2, 4, 6, 8};

        when(digitalGreenCertificateService.issueRecoveryCertificatePdf(fn, gn, dob, id, tg, fr, is, df, du,
                new CallContext(mandantId, clientSystem, workplace, cardHandle))).thenReturn(bytes);

        Response response = digitalGreenCertificateResource.recovered(fn, gn, dob, id, tg, fr, is, df, du, mandantId,
                clientSystem, workplace, cardHandle);

        assertNotNull(response);
        assertEquals("application/pdf", response.getMediaType().toString());
        assertSame(bytes, response.getEntity());
    }
}
