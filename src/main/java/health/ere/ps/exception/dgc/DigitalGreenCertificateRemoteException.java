package health.ere.ps.exception.dgc;

/**
 * Errors on the remote side of the certificate service.
 */
public class DigitalGreenCertificateRemoteException extends DigitalGreenCertificateException {

    public DigitalGreenCertificateRemoteException(int code, String message) {
        super(code, message);
    }
}
