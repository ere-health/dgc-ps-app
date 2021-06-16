package health.ere.ps.exception.dgc;

public abstract class DigitalGreenCertificateException extends RuntimeException {
    private int code;

    public DigitalGreenCertificateException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
