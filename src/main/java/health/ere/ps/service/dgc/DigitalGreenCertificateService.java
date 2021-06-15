package health.ere.ps.service.dgc;

import health.ere.ps.event.RequestBearerTokenFromIdpEvent;
import health.ere.ps.model.dgc.CertificateRequest;
import health.ere.ps.model.dgc.PersonName;
import health.ere.ps.model.dgc.RecoveryCertificateRequest;
import health.ere.ps.model.dgc.RecoveryEntry;
import health.ere.ps.model.dgc.V;
import health.ere.ps.model.dgc.VaccinationCertificateRequest;
import health.ere.ps.ssl.SSLUtilities;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import io.quarkus.runtime.StartupEvent;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.enterprise.event.Observes;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;

@ApplicationScoped
public class DigitalGreenCertificateService {
    private static Logger log = Logger.getLogger(DigitalGreenCertificateService.class.getName());

    @ConfigProperty(name = "digital-green-certificate-service.issuerAPIUrl", defaultValue = "")
    String issuerAPIUrl;

    Client client;

    @Inject
    Event<RequestBearerTokenFromIdpEvent> requestBearerTokenFromIdp;

    void onStart(@Observes StartupEvent ev) {               
        log.info("Application started go to: http://localhost:8080/dgc/covid-19-certificate.html");
    }

    @PostConstruct
    public void init() {
        SSLUtilities.trustAllHostnames();
        SSLUtilities.trustAllHttpsCertificates();
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
    public byte[] issueVaccinationCertificatePdf(String fn, String gn, LocalDate dob,
                                                 String id, String tg, String vp, String mp, String ma, Integer dn,
                                                 Integer sd, String dt) {

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

        return issuePdf(vaccinationCertificateRequest);
    }

    private String standardize(String fn) {
        // TODO: implement me in a better way
        return fn.toUpperCase();
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
     * @param is  issuer of certificate
     * @param df  certificate validity date beginning
     * @param du  certificate validity date ending
     * @return bytes of certificate pdf
     */
    public byte[] issueRecoveryCertificatePdf(String fn, String gn, LocalDate dob, String id, String tg, LocalDate fr,
                                              String is, LocalDate df, LocalDate du) {

        RecoveryCertificateRequest recoveryCertificateRequest = new RecoveryCertificateRequest();

        recoveryCertificateRequest.setNam(new PersonName(fn, gn));
        recoveryCertificateRequest.setDob(dob);

        RecoveryEntry r = new RecoveryEntry();

        r.setId(id);
        r.setTg(tg);
        r.setFr(fr);
        r.setIs(is);
        r.setDf(df);
        r.setDu(du);

        recoveryCertificateRequest.setR(Collections.singletonList(r));

        return issuePdf(recoveryCertificateRequest);
    }

    /**
     * Request the certificate at the certificate backend enriched with the token for access.
     *
     * @param requestData       the data send to the backend, only allowed are: VaccinationCertificateRequest,
     *                          RecoveryCertificateRequest and TestCertificateRequest. Must be not null.
     * @return the serialized response.
     */
    public byte[] issuePdf(@NotNull CertificateRequest requestData) {

        Objects.requireNonNull(requestData); // can removed, if a validator is running.
        if(requestData instanceof VaccinationCertificateRequest) {
            VaccinationCertificateRequest vaccinationCertificateRequest = (VaccinationCertificateRequest) requestData;
            vaccinationCertificateRequest.getNam().fnt = standardize(vaccinationCertificateRequest.getNam().fn);
        }
        Entity<CertificateRequest> entity = Entity.entity(requestData, "application/vnd.dgc.v1+json");
        // entity = Entity.entity("{\r\n  \"ver\": \"1.0.0\",\r\n  \"nam\": {\r\n    \"fn\": \"d'Ars\u00F8ns - van Halen\",\r\n    \"gn\": \"Fran\u00E7ois-Joan\",\r\n    \"fnt\": \"DARSONS<VAN<HALEN\",\r\n    \"gnt\": \"FRANCOIS<JOAN\"\r\n  },\r\n  \"dob\": \"2009-02-28\",\r\n  \"v\": [\r\n    {\r\n      \"id\": \"123456\",\r\n      \"tg\": \"840539006\",\r\n      \"vp\": \"1119349007\",\r\n      \"mp\": \"EU/1/20/1528\",\r\n      \"ma\": \"ORG-100030215\",\r\n      \"dn\": 2,\r\n      \"sd\": 2,\r\n      \"dt\": \"2021-04-21\",\r\n      \"co\": \"NL\",\r\n      \"is\": \"Ministry of Public Health, Welfare and Sport\",\r\n      \"ci\": \"urn:uvci:01:NL:PlA8UWS60Z4RZXVALl6GAZ\"\r\n    }\r\n  ]\r\n}", "application/vnd.dgc.v1+json");
        Response response = client.target(issuerAPIUrl)
                .path("/api/certify/v2/issue")
                .request("application/pdf")
                .header("Authorization", "Bearer " + getToken())
                .post(entity);

        byte[] responseData;

        try {
            responseData = response.readEntity(InputStream.class).readAllBytes();
            if (Response.Status.Family.familyOf(response.getStatus()) != Response.Status.Family.SUCCESSFUL) {
                throw new RuntimeException(new String(responseData));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return responseData;
    }

    private String getToken() {
        RequestBearerTokenFromIdpEvent event = new RequestBearerTokenFromIdpEvent();

        requestBearerTokenFromIdp.fire(event);

        return Optional.ofNullable(event.getBearerToken()).orElseThrow(IllegalArgumentException::new);
    }
}
