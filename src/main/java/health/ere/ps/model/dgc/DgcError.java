package health.ere.ps.model.dgc;

import java.util.Objects;

public class DgcError {
    private int code;

    private String message;

    public DgcError() {
        // default constructor
    }

    public DgcError(int code, String message) {
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
        DgcError dgcError = (DgcError) o;
        return code == dgcError.code && Objects.equals(message, dgcError.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code, message);
    }
}
