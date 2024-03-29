package health.ere.ps.resource.exception;

import health.ere.ps.exception.dgc.DigitalGreenCertificateCertificateServiceAuthenticationException;
import health.ere.ps.exception.dgc.DigitalGreenCertificateException;
import health.ere.ps.exception.dgc.DigitalGreenCertificateInternalAuthenticationException;
import health.ere.ps.exception.dgc.DigitalGreenCertificateInvalidParametersException;
import health.ere.ps.exception.dgc.DigitalGreenCertificateRemoteException;
import health.ere.ps.model.dgc.DigitalGreenCertificateError;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class DigitalGreenCertificateExceptionMapper implements ExceptionMapper<Exception> {
    @Override
    public Response toResponse(Exception exception) {
        if (exception instanceof DigitalGreenCertificateException) {
            DigitalGreenCertificateException digitalGreenCertificateException =
                    (DigitalGreenCertificateException) exception;

            return Response.status(getStatus(digitalGreenCertificateException))
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .entity(new DigitalGreenCertificateError(digitalGreenCertificateException.getCode(), digitalGreenCertificateException.getMessage()))
                    .build();
        } else {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .entity(new DigitalGreenCertificateError(-1, "Unexpected error; check logs for details"))
                    .build();
        }
    }

    private static Response.Status getStatus(DigitalGreenCertificateException digitalGreenCertificateException) {
        if (digitalGreenCertificateException instanceof DigitalGreenCertificateInvalidParametersException) {
            return Response.Status.BAD_REQUEST;
        } else if (digitalGreenCertificateException instanceof DigitalGreenCertificateInternalAuthenticationException) {
            return Response.Status.UNAUTHORIZED;
        } else if (digitalGreenCertificateException instanceof DigitalGreenCertificateCertificateServiceAuthenticationException) {
            return Response.Status.FORBIDDEN;
        } else if (digitalGreenCertificateException instanceof DigitalGreenCertificateRemoteException) {
            return Response.Status.fromStatusCode(501);
        } else {
            return Response.Status.INTERNAL_SERVER_ERROR;
        }
    }
}
