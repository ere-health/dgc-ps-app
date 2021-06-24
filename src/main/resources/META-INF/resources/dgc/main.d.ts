type HealthState = "OK" | "FAIL" | "UNKNOWN";

interface HealthStatus {
    cardState: HealthState
    certificateServiceRouteState: HealthState
    connectorState: HealthState
    identityProviderRouteState: HealthState
    parameterState: HealthState
}