package health.ere.ps.exception.dgc;

/**
 * Exception that occurred when authenticating against the certificate service.
 */
public class DigitalGreenCertificateCertificateServiceAuthenticationException extends DigitalGreenCertificateException {
    public DigitalGreenCertificateCertificateServiceAuthenticationException(int code, String message) {
        super(code, message);
    }
}
