package health.ere.ps.resource.dgc;

import health.ere.ps.exception.dgc.DigitalGreenCertificateCertificateServiceAuthenticationException;
import health.ere.ps.exception.dgc.DigitalGreenCertificateCertificateServiceException;
import health.ere.ps.exception.dgc.DigitalGreenCertificateInternalAuthenticationException;
import health.ere.ps.exception.dgc.DigitalGreenCertificateInvalidParametersException;
import health.ere.ps.exception.dgc.DigitalGreenCertificateRemoteException;
import health.ere.ps.model.dgc.VaccinationCertificateRequest;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;

/**
 * This class is entirely for testing purposes only and aims to support the PVS creators integration this certificate
 * service.
 */
@Path("/test-api/certify/v2")
public class TestDigitalGreenCertificateResource {
    @Path("/issue")
    @POST
    public Response issue(VaccinationCertificateRequest vaccinationCertificateRequest) throws IOException {
        if (vaccinationCertificateRequest.getNam() == null) {
            throw new DigitalGreenCertificateCertificateServiceException(999998, "Missing nam element");
        } else if (vaccinationCertificateRequest.v == null) {
            throw new DigitalGreenCertificateCertificateServiceException(999999, "Missing v element");
        } else if ("401".equals(vaccinationCertificateRequest.getNam().gn)) {
            throw new DigitalGreenCertificateInternalAuthenticationException();
        } else if ("403".equals(vaccinationCertificateRequest.getNam().gn)) {
            throw new DigitalGreenCertificateCertificateServiceAuthenticationException(123456, "Auth error from IDP");
        } else if ("400".equals(vaccinationCertificateRequest.getNam().gn)) {
            throw new DigitalGreenCertificateInvalidParametersException(999999, "Generic 400 error");
        } else if ("501".equals(vaccinationCertificateRequest.getNam().gn)) {
            throw new DigitalGreenCertificateRemoteException(999999, "Remote exception from certificate service");
        } else {
            try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("test/testpdf.pdf")) {
                if (inputStream == null) {
                    return Response.status(Response.Status.NOT_FOUND).build();
                }

                return Response.ok(inputStream.readAllBytes(), "application/pdf").build();
            }
        }
    }
}
