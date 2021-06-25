package health.ere.ps.service.dgc;

import health.ere.ps.config.AppConfig;
import health.ere.ps.event.RequestBearerTokenFromIdpEvent;
import health.ere.ps.exception.dgc.DigitalGreenCertificateCertificateServiceAuthenticationException;
import health.ere.ps.exception.dgc.DigitalGreenCertificateCertificateServiceException;
import health.ere.ps.exception.dgc.DigitalGreenCertificateInternalAuthenticationException;
import health.ere.ps.exception.dgc.DigitalGreenCertificateInvalidParametersException;
import health.ere.ps.model.dgc.CallContext;
import health.ere.ps.model.dgc.CertificateRequest;
import health.ere.ps.model.dgc.PersonName;
import health.ere.ps.model.dgc.RecoveryCertificateRequest;
import health.ere.ps.model.dgc.RecoveryEntry;
import health.ere.ps.model.dgc.V;
import health.ere.ps.model.dgc.VaccinationCertificateRequest;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class DigitalGreenCertificateService {
    private static final Logger LOG = Logger.getLogger(DigitalGreenCertificateService.class.getName());

    @Inject
    AppConfig appConfig;

    Client client;

    @Inject
    Event<RequestBearerTokenFromIdpEvent> requestBearerTokenFromIdp;

//    void onStart(@Observes StartupEvent ev) {               
//        LOG.info("Application started go to: http://localhost:8080/dgc/covid-19-vaccination-certificate.html");
//    }

    @PostConstruct
    public void init() {
        ClientBuilder clientBuilder = ClientBuilder.newBuilder();
        client = clientBuilder.build();
    }

    @PreDestroy
    public void destroy() {
        if (client != null) {
            client.close();
        }
    }

    /**
     * Issue a certificate based on the given values.
     *
     * @param fn  (family) name
     * @param gn  given name
     * @param dob date of birth
     * @param id administering instance id
     * @param tg disease
     * @param vp vaccine
     * @param mp product
     * @param ma manufacturer
     * @param dn dose number
     * @param sd total dose count
     * @param dt vaccination date
     * @return bytes of certificate pdf
     */
    public byte[] issueVaccinationCertificatePdf(String fn, String gn, String dob,
                                                 String id, String tg, String vp, String mp, String ma, Integer dn,
                                                 Integer sd, String dt, CallContext callContext) {

        VaccinationCertificateRequest vaccinationCertificateRequest = new VaccinationCertificateRequest();

        vaccinationCertificateRequest.setDob(dob);
        vaccinationCertificateRequest.setNam(new PersonName(fn, gn));

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

        return issuePdf(vaccinationCertificateRequest, callContext);
    }

    /**
     * Create a recovery certificate pdf.
     *
     * @param fn  (family) name
     * @param gn  given name
     * @param dob date of birth
     * @param id  administering instance id
     * @param tg  disease
     * @param fr  date of test result, that has been positive
     * @param df  certificate validity date beginning
     * @param du  certificate validity date ending
     * @param callContext optional call context that specifies the tenant
     * @return bytes of certificate pdf
     */
    public byte[] issueRecoveryCertificatePdf(String fn, String gn, String dob, String id, String tg, String fr,
                                              String df, String du, CallContext callContext) {

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

        return issuePdf(recoveryCertificateRequest, callContext);
    }

    /**
     * Request the certificate at the certificate backend enriched with the token for access.
     *
     * @param requestData       the data send to the backend, only allowed are: VaccinationCertificateRequest,
     *                          RecoveryCertificateRequest and TestCertificateRequest. Must be not null.
     * @param callContext       call context that specifies the tenant; optional
     * @return the serialized response.
     */
    public byte[] issuePdf(@NotNull CertificateRequest requestData, CallContext callContext) {

        Objects.requireNonNull(requestData); // can removed, if a validator is running.

        Entity<CertificateRequest> entity = Entity.entity(requestData, "application/vnd.dgc.v1+json");
        // entity = Entity.entity("{\r\n  \"ver\": \"1.0.0\",\r\n  \"nam\": {\r\n    \"fn\": \"d'Ars\u00F8ns - van Halen\",\r\n    \"gn\": \"Fran\u00E7ois-Joan\",\r\n    \"fnt\": \"DARSONS<VAN<HALEN\",\r\n    \"gnt\": \"FRANCOIS<JOAN\"\r\n  },\r\n  \"dob\": \"2009-02-28\",\r\n  \"v\": [\r\n    {\r\n      \"id\": \"123456\",\r\n      \"tg\": \"840539006\",\r\n      \"vp\": \"1119349007\",\r\n      \"mp\": \"EU/1/20/1528\",\r\n      \"ma\": \"ORG-100030215\",\r\n      \"dn\": 2,\r\n      \"sd\": 2,\r\n      \"dt\": \"2021-04-21\",\r\n      \"co\": \"NL\",\r\n      \"is\": \"Ministry of Public Health, Welfare and Sport\",\r\n      \"ci\": \"urn:uvci:01:NL:PlA8UWS60Z4RZXVALl6GAZ\"\r\n    }\r\n  ]\r\n}", "application/vnd.dgc.v1+json");
        Response response = client.target(appConfig.getDigitalGreenCertificateServiceIssuerAPI())
                .path("/api/certify/v2/issue")
                .request("application/pdf")
                .header("Authorization", "Bearer " + getToken(callContext))
                .post(entity);

        // the error codes in the exceptions reflect the response https status from the api request
        // the offset is added to support future error codes; offset 100000 is used for errors that
        // originate in the pdf creation webservice

        switch (response.getStatus()) {
            case 200: {
                try {
                    byte[] pdf = response.readEntity(InputStream.class).readAllBytes();
                    return pdf;
                } catch (IOException ioe) {
                    throw new DigitalGreenCertificateCertificateServiceException(100200, "Could not read response from certificate service");
                }
            }
            case 400:
            case 406: {
                String error = getError(response);
                throw new DigitalGreenCertificateInvalidParametersException(100000 + response.getStatus(), "Invalid parameters in request" +
                        " to certificate service: "+error);
            }
            case 401:
            case 403: {
                throw new DigitalGreenCertificateCertificateServiceAuthenticationException(100000 + response.getStatus(), "Credentials " +
                        "were not accepted by certificate service");
            }
            case 500: {
                String error = getError(response);
                throw new DigitalGreenCertificateCertificateServiceException(100500, "Internal server error in certificate service "+error);
            }
            default: {
                throw new DigitalGreenCertificateCertificateServiceException(100000 + response.getStatus(), "Unspecified response from " +
                        "certificate service");
            }
        }
    }

    private String getError(Response response) {
        String error = "";
        try {
            InputStream inputStream = response.readEntity(InputStream.class);
            if(inputStream != null) {
                error = new String(inputStream.readAllBytes());
            }
        } catch (IOException e) {
            throw new DigitalGreenCertificateCertificateServiceException(100200, "Could not read response from certificate service");
        }
        return error;
    }

    private String getToken(CallContext callContext) {
        RequestBearerTokenFromIdpEvent event = new RequestBearerTokenFromIdpEvent();

        event.setCallContext(callContext);

        try {
            requestBearerTokenFromIdp.fire(event);
        } catch (Throwable t) {
            LOG.log(Level.SEVERE, t, () -> "Error fetching token");
            throw new DigitalGreenCertificateInternalAuthenticationException();
        }

        return Optional.ofNullable(event.getBearerToken())
                .orElseThrow(DigitalGreenCertificateInternalAuthenticationException::new);
    }
}
