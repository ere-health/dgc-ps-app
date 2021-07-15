package health.ere.ps.exception.idp;

public class IdpClientException extends Exception {
    public enum Origin {
        CONNECTOR,

        IDP,

        INTERNAL,
    }

    private static final long serialVersionUID = -3280232274428362762L;

    private Origin origin;

    public IdpClientException(final Exception e, final Origin origin) {
        super(e);
        this.origin = origin;
    }

    public IdpClientException(final Exception e) {
        this(e, Origin.INTERNAL);
    }

    public IdpClientException(final String s, final Origin origin) {
        super(s);
        this.origin = origin;
    }

    public IdpClientException(final String s) {
        this(s, Origin.INTERNAL);
    }

    public IdpClientException(final String message, final Exception e, final Origin origin) {
        super(message, e);
        this.origin = origin;
    }

    public IdpClientException(final String message, final Exception e) {
        this(message, e, Origin.INTERNAL);
    }

    public Origin getOrigin() {
        return origin;
    }
}
