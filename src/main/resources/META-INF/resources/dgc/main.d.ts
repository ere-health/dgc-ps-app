type HealthState = "OK" | "FAIL" | "UNKNOWN";

interface HealthStatus {
    cardState: HealthState
    connectorState: HealthState
    parameterState: HealthState
    identityProviderRouteState: HealthState
    identityProviderRoute: string | null
    certificateServiceRouteState: HealthState
    certificateServiceRoute: string | null
    connectorUrls: {
        AuthSignatureService: string
        CardService: string
        CertificateService: string
        EventService: string
    }
}