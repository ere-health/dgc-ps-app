package health.ere.ps.model.dgc;

import java.util.Objects;

public class HealthStatus {

    enum StatusState {
        OK,
        FAIL,
        UNKNOWN,
    }

    private boolean connectorReachable;

    private boolean parameterCorrect;

    private boolean sMCBCardCorrect;

    private boolean iDPRouteCorrect;

    private boolean certificateServiceRouteCorrect;

    public HealthStatus() {

    }

    public HealthStatus(boolean connectorReachable, boolean parameterCorrect,
                        boolean sMCBCardCorrect, boolean iDPRouteCorrect, boolean certificateServiceRouteCorrect) {
        this.connectorReachable = connectorReachable;
        this.parameterCorrect = parameterCorrect;
        this.sMCBCardCorrect = sMCBCardCorrect;
        this.iDPRouteCorrect = iDPRouteCorrect;
        this.certificateServiceRouteCorrect = certificateServiceRouteCorrect;
    }

    public boolean isConnectorReachable() {
        return connectorReachable;
    }

    public void setConnectorReachable(boolean connectorReachable) {
        this.connectorReachable = connectorReachable;
    }

    public boolean isParameterCorrect() {
        return parameterCorrect;
    }

    public void setParameterCorrect(boolean parameterCorrect) {
        this.parameterCorrect = parameterCorrect;
    }

    public boolean issMCBCardCorrect() {
        return sMCBCardCorrect;
    }

    public void setsMCBCardCorrect(boolean sMCBCardCorrect) {
        this.sMCBCardCorrect = sMCBCardCorrect;
    }

    public boolean isiDPRouteCorrect() {
        return iDPRouteCorrect;
    }

    public void setiDPRouteCorrect(boolean iDPRouteCorrect) {
        this.iDPRouteCorrect = iDPRouteCorrect;
    }

    public boolean isCertificateServiceRouteCorrect() {
        return certificateServiceRouteCorrect;
    }

    public void setCertificateServiceRouteCorrect(boolean certificateServiceRouteCorrect) {
        this.certificateServiceRouteCorrect = certificateServiceRouteCorrect;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HealthStatus that = (HealthStatus) o;
        return connectorReachable == that.connectorReachable
                && parameterCorrect == that.parameterCorrect
                && sMCBCardCorrect == that.sMCBCardCorrect
                && iDPRouteCorrect == that.iDPRouteCorrect
                && certificateServiceRouteCorrect == that.certificateServiceRouteCorrect;
    }

    @Override
    public int hashCode() {
        return Objects.hash(connectorReachable, parameterCorrect, sMCBCardCorrect, iDPRouteCorrect, certificateServiceRouteCorrect);
    }

    @Override
    public String toString() {
        return "class HealthStatus {" +
                "connectorReachable:" + connectorReachable +
                ", parameterCorrect:" + parameterCorrect +
                ", sMCBCardCorrect:" + sMCBCardCorrect +
                ", iDPRouteCorrect:" + iDPRouteCorrect +
                ", certificateServiceRouteCorrect:" + certificateServiceRouteCorrect +
                '}';
    }
}
