package health.ere.ps.resource.exception;

import health.ere.ps.exception.dgc.DgcCertificateServiceAuthenticationException;
import health.ere.ps.exception.dgc.DgcException;
import health.ere.ps.exception.dgc.DgcInternalAuthenticationException;
import health.ere.ps.exception.dgc.DgcInvalidParametersException;
import health.ere.ps.model.dgc.DgcError;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class DgcExceptionMapper implements ExceptionMapper<DgcException> {
    @Override
    public Response toResponse(DgcException dgcException) {

        return Response.status(getStatus(dgcException))
                .type(MediaType.APPLICATION_JSON_TYPE)
                .entity(new DgcError(dgcException.getCode(), dgcException.getMessage()))
                .build();
    }

    private static Response.Status getStatus(DgcException dgcException) {
        if (dgcException instanceof DgcInvalidParametersException) {
            return Response.Status.BAD_REQUEST;
        } else if (dgcException instanceof DgcInternalAuthenticationException) {
            return Response.Status.UNAUTHORIZED;
        } else if (dgcException instanceof DgcCertificateServiceAuthenticationException) {
            return Response.Status.FORBIDDEN;
        } else {
            return Response.Status.INTERNAL_SERVER_ERROR;
        }
    }
}
