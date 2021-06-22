package health.ere.ps.resource.dgc;

import health.ere.ps.service.dgc.StatusService;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;

@Path("/api/certify/v2")
public class StatusResource {
    @Inject
    StatusService service;

    @Path("/status")
    @GET
    public Response status() {
        return Response.ok(service.collectStatus()).build();
    }
}
