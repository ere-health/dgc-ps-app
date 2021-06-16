package health.ere.ps.exception.dgc;

/**
 * This exception will be triggered in case there is an exception w.r.t. the authentication with the local connector.
 */
public class DigitalGreenCertificateInternalAuthenticationException extends DigitalGreenCertificateException {
    public DigitalGreenCertificateInternalAuthenticationException() {
        super(200401, "Could not get authentication token from internal connector");
    }
}
