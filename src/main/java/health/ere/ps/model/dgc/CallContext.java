package health.ere.ps.model.dgc;

import java.util.Objects;

public class CallContext {
    private final String mandantId;

    private final String clientSystem;

    private final String workplace;

    private final String cardHandle;

    public CallContext(String mandantId, String clientSytem, String workplace, String cardHandle) {
        this.mandantId = mandantId;
        this.clientSystem = clientSytem;
        this.workplace = workplace;
        this.cardHandle = cardHandle;
    }

    public String getMandantId() {
        return mandantId;
    }

    public String getClientSystem() {
        return clientSystem;
    }

    public String getWorkplace() {
        return workplace;
    }

    public String getCardHandle() {
        return cardHandle;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CallContext that = (CallContext) o;
        return Objects.equals(mandantId, that.mandantId) && Objects.equals(clientSystem, that.clientSystem) && Objects.equals(workplace, that.workplace) && Objects.equals(cardHandle, that.cardHandle);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mandantId, clientSystem, workplace, cardHandle);
    }
}
