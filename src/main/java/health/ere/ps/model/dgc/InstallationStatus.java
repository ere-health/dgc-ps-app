package health.ere.ps.model.dgc;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.util.Map;
import java.util.Objects;

public class InstallationStatus {

    /**
     * The state of the elements of installation.
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    public enum State {
        /**
         * All right, good to go.
         */
        OK,
        /**
         * Not good, fix it.
         */
        FAIL,
        /**
         * The state is not known or not set. (Default value).
         */
        UNKNOWN,
    }

    /**
     * Check if the connector is reachable.
     */
    private State connectorState = State.UNKNOWN;

    /**
     * Check if the parameters are correctly set up
     */
    private State parameterState = State.UNKNOWN;

    /**
     * Check if the SMC-B card handle is correct set up and available via connector.
     */
    private State cardState = State.UNKNOWN;

    /**
     * Check if the IDP is callable via GET.
     */
    private State identityProviderRouteState = State.UNKNOWN;

    private String identityProviderRoute = null;

    /**
     * Check if the DGC issuer is callable via GET.
     */
    private State certificateServiceRouteState = State.UNKNOWN;

    private String certificateServiceRoute = null;

    private Map<String, String> connectorUrls;

    public State getConnectorState() {
        return connectorState;
    }

    public void setConnectorState(State connectorState) {
        this.connectorState = connectorState;
    }

    public State getParameterState() {
        return parameterState;
    }

    public void setParameterState(State parameterState) {
        this.parameterState = parameterState;
    }

    public State getCardState() {
        return cardState;
    }

    public void setCardState(State cardState) {
        this.cardState = cardState;
    }

    public State getIdentityProviderRouteState() {
        return identityProviderRouteState;
    }

    public void setIdentityProviderRouteState(State identityProviderRouteState) {
        this.identityProviderRouteState = identityProviderRouteState;
    }

    public State getCertificateServiceRouteState() {
        return certificateServiceRouteState;
    }

    public void setCertificateServiceRouteState(State certificateServiceRouteState) {
        this.certificateServiceRouteState = certificateServiceRouteState;
    }

    public String getIdentityProviderRoute() {
        return identityProviderRoute;
    }

    public void setIdentityProviderRoute(String identityProviderRoute) {
        this.identityProviderRoute = identityProviderRoute;
    }

    public String getCertificateServiceRoute() {
        return certificateServiceRoute;
    }

    public void setCertificateServiceRoute(String certificateServiceRoute) {
        this.certificateServiceRoute = certificateServiceRoute;
    }

    public Map<String, String> getConnectorUrls() {
        return connectorUrls;
    }

    public void setConnectorUrls(Map<String, String> connectorUrls) {
        this.connectorUrls = connectorUrls;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InstallationStatus that = (InstallationStatus) o;
        return connectorState == that.connectorState && parameterState == that.parameterState
                && cardState == that.cardState
                && identityProviderRouteState == that.identityProviderRouteState
                && Objects.equals(identityProviderRoute, that.identityProviderRoute)
                && certificateServiceRouteState == that.certificateServiceRouteState
                && Objects.equals(certificateServiceRoute, that.certificateServiceRoute)
                && Objects.equals(connectorUrls, that.connectorUrls);
    }

    @Override
    public int hashCode() {
        return Objects.hash(connectorState, parameterState, cardState,
                identityProviderRouteState, identityProviderRoute,
                certificateServiceRouteState, certificateServiceRoute, connectorUrls);
    }

    @Override
    public String toString() {
        return "InstallationStatus{" +
                "connectorState=" + connectorState +
                ", parameterState=" + parameterState +
                ", cardState=" + cardState +
                ", identityProviderConfigurationState=" + identityProviderRouteState +
                ", identityProviderRoute='" + identityProviderRoute + '\'' +
                ", certificateServiceConfigurationState=" + certificateServiceRouteState +
                ", certificateServiceRoute='" + certificateServiceRoute + '\'' +
                ", connectorUrls=" + connectorUrls +
                '}';
    }
}
