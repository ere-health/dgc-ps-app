package health.ere.ps.model.dgc;

import java.util.Objects;

/**
 * Dto class for error responses.
 */
public class DigitalGreenCertificateError {
    private int code;

    private String message;

    public DigitalGreenCertificateError() {
        // default constructor
    }

    public DigitalGreenCertificateError(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DigitalGreenCertificateError digitalGreenCertificateError = (DigitalGreenCertificateError) o;
        return code == digitalGreenCertificateError.code && Objects.equals(message, digitalGreenCertificateError.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code, message);
    }
}
