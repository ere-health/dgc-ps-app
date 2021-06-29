type HealthState = "OK" | "FAIL" | "UNKNOWN";

interface HealthStatus {
    cardState: HealthState
    connectorState: HealthState
    parameterState: HealthState
    identityProviderRouteState: HealthState
    identityProviderConfigurationState: HealthState
    certificateServiceRouteState: HealthState
    certificateServiceConfigurationState: HealthState
    connectorUrls: {
        AuthSignatureService: string
        CardService: string
        CertificateService: string
        EventService: string
    }
}