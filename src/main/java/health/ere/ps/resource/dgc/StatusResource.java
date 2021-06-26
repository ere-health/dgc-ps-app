package health.ere.ps.resource.dgc;

import health.ere.ps.service.dgc.StatusService;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

@Path("/api/certify/v2")
public class StatusResource {
    @Inject
    StatusService statusService;

    @Path("/status")
    @GET
    @Produces(value = "application/json")
    public Response status() {
        return Response.ok(statusService.collectStatus()).build();
    }
}
