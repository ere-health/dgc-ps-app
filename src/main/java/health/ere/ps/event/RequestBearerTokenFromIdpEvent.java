package health.ere.ps.event;

import health.ere.ps.model.dgc.CallContext;

import java.util.Objects;

public class RequestBearerTokenFromIdpEvent {
    private String bearerToken;

    private CallContext callContext;

    private Exception exception;

    public String getBearerToken() {
        return this.bearerToken;
    }

    public void setBearerToken(String bearerToken) {
        this.bearerToken = bearerToken;
    }

    public CallContext getCallContext() {
        return callContext;
    }

    public void setCallContext(CallContext callContext) {
        this.callContext = callContext;
    }

    public Exception getException() {
        return exception;
    }

    public void setException(Exception exception) {
        this.exception = exception;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RequestBearerTokenFromIdpEvent that = (RequestBearerTokenFromIdpEvent) o;
        return Objects.equals(bearerToken, that.bearerToken) && Objects.equals(callContext, that.callContext)
                && Objects.equals(exception, that.exception);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bearerToken, callContext, exception);
    }
}
