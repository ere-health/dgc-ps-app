package health.ere.ps.service.dgc;

import health.ere.ps.model.dgc.HealthStatus;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class StatusService {

    public HealthStatus collectStatus() {
        HealthStatus healthStatus = new HealthStatus();
        return healthStatus;
    }

    @PostConstruct
    public void init() {

    }

    public boolean isConnectorReachable() {

        return false;
    }

    public boolean isParameterCorrect() {
        return false;
    }

    public boolean isSMCBCardCorrect() {
        return false;
    }

    public boolean isIDPRouteCorrect() {
        return false;
    }

    public boolean isCertificateServiceRouteCorrect() {
        return false;
    }
}
