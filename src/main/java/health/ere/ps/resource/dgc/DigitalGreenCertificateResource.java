package health.ere.ps.resource.dgc;

import health.ere.ps.model.dgc.CallContext;
import health.ere.ps.model.dgc.VaccinationCertificateRequest;
import health.ere.ps.service.dgc.DigitalGreenCertificateService;
import health.ere.ps.model.dgc.RecoveryCertificateRequest;

import javax.inject.Inject;
import javax.mail.Header;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.time.LocalDate;
import java.util.logging.Logger;

@Path("/api/certify/v2")
public class DigitalGreenCertificateResource {
    private static final String HEADER_MANDANTID = "X-Mandant";

    private static final String HEADER_CLIENTSYSTEM = "X-ClientSystem";

    private static final String HEADER_WORKPLACE = "X-Workplace";

    private static final String HEADER_CARDHANDLE = "X-CardHandle";

    @Inject
    DigitalGreenCertificateService digitalGreenCertificateService;

    @Path("/issue")
    @POST
    public Response issue(VaccinationCertificateRequest vaccinationCertificateRequest,
                          @HeaderParam(HEADER_MANDANTID) String mandantId,
                          @HeaderParam(HEADER_CLIENTSYSTEM) String clientSystem,
                          @HeaderParam(HEADER_WORKPLACE) String workplace,
                          @HeaderParam(HEADER_CARDHANDLE) String cardHandle) {

        return okPdf(digitalGreenCertificateService.issuePdf(vaccinationCertificateRequest, new CallContext(mandantId,
                clientSystem, workplace, cardHandle)));
    }

    @Path("/issue")
    @GET
    public Response issue(@QueryParam("fn") String fn, @QueryParam("gn") String gn, @QueryParam("dob") LocalDate dob,
                          @QueryParam("id") String id, @QueryParam("tg") String tg, @QueryParam("vp") String vp,
                          @QueryParam("mp") String mp, @QueryParam("ma") String ma, @QueryParam("dn") Integer dn,
                          @QueryParam("sd") Integer sd, @QueryParam("dt") LocalDate dt,
                          @HeaderParam(HEADER_MANDANTID) String mandantId,
                          @HeaderParam(HEADER_CLIENTSYSTEM) String clientSystem,
                          @HeaderParam(HEADER_WORKPLACE) String workplace,
                          @HeaderParam(HEADER_CARDHANDLE) String cardHandle) {

        return okPdf(digitalGreenCertificateService.issueVaccinationCertificatePdf(fn, gn, dob, id, tg, vp, mp, ma,
                dn, sd, dt, new CallContext(mandantId, clientSystem, workplace, cardHandle)));
    }

    @Path("/recovered")
    @POST
    public Response recovered(RecoveryCertificateRequest recoveryCertificateRequest,
                              @HeaderParam(HEADER_MANDANTID) String mandantId,
                              @HeaderParam(HEADER_CLIENTSYSTEM) String clientSystem,
                              @HeaderParam(HEADER_WORKPLACE) String workplace,
                              @HeaderParam(HEADER_CARDHANDLE) String cardHandle) {
        return okPdf(digitalGreenCertificateService.issuePdf(recoveryCertificateRequest, new CallContext(mandantId,
                clientSystem, workplace, cardHandle)));
    }

    @Path("/recovered")
    @GET
    public Response recovered(@QueryParam("fn") String fn, @QueryParam("gn") String gn,
                              @QueryParam("dob") LocalDate dob, @QueryParam("id") String id,
                              @QueryParam("tg") String tg, @QueryParam("fr") LocalDate fr,
                              @QueryParam("df") LocalDate df, @QueryParam("du") LocalDate du,
                              @HeaderParam(HEADER_MANDANTID) String mandantId,
                              @HeaderParam(HEADER_CLIENTSYSTEM) String clientSystem,
                              @HeaderParam(HEADER_WORKPLACE) String workplace,
                              @HeaderParam(HEADER_CARDHANDLE) String cardHandle) {

        return okPdf(digitalGreenCertificateService.issueRecoveryCertificatePdf(fn, gn, dob, id, tg, fr, df, du,
                new CallContext(mandantId, clientSystem, workplace, cardHandle)));
    }

    private static Response okPdf(byte[] bytes) {
        return Response.ok(bytes, "application/pdf").build();
    }

}
