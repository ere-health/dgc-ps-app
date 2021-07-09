package health.ere.ps.resource.dgc;

import health.ere.ps.service.dgc.StatusService;
import io.smallrye.common.annotation.Blocking;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

@Path("/api/certify/v2")
public class StatusResource {
    @Inject
    StatusService statusService;

    @Path("/status")
    @GET
    @Blocking
    public Response status() {
        return Response.ok(statusService.collectStatus()).build();
    }
}
