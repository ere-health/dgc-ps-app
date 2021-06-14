package health.ere.ps.service.connector.certificate;

import de.gematik.ws.conn.connectorcontext.v2.ContextType;

import org.apache.commons.lang3.StringUtils;

/**
 * Represents a calling context
 */
public class InvocationContext {

    private String clientId;
    private String clientSystemId;
    private String workplaceId;
    private String userId;
    private String mandantId;

    public InvocationContext() {

    }

    /**
     * Constructor
     *
     * @param clientId     client Id
     * @param clientSystemId client system
     * @param workplaceId    work place
     */
    public InvocationContext(String clientId, String clientSystemId, String workplaceId) {
        this.clientId = clientId;
        this.clientSystemId = clientSystemId;
        this.workplaceId = workplaceId;
    }

    /**
     * Constructor
     *
     * @param clientId     client Id
     * @param clientSystemId client system
     * @param workplaceId    work place
     * @param userId       user Id
     */
    public InvocationContext(String clientId, String clientSystemId, String workplaceId, String userId) {
        this.clientId = clientId;
        this.clientSystemId = clientSystemId;
        this.workplaceId = workplaceId;
        this.userId = userId;
    }

    public String getClientId() {
        return clientId;
    }

    public String getMandantId() {
        return mandantId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSystemId() {
        return clientSystemId;
    }

    public void setClientSystemId(String clientSystemId) {
        this.clientSystemId = clientSystemId;
    }

    public String getWorkplaceId() {
        return workplaceId;
    }

    public void setWorkplaceId(String workplaceId) {
        this.workplaceId = workplaceId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    /**
     * Converts the call context into the representation required for the SOAP interface.
     *
     * @return Call context in the representation required for the SOAP interface.
     */
    public ContextType convertToContextType() {
        ContextType contextType = new ContextType();
        contextType.setMandantId(getMandantId());
        contextType.setClientSystemId(getClientSystemId());
        contextType.setWorkplaceId(getWorkplaceId());
        contextType.setUserId(getUserId());
        return contextType;
    }

    /**
     * Roughly checks the calling context. <b> A content check does not take place. </b>
     *
     * @return true if {@link InvocationContext} is apparently valid (client, client system and
     * Workplaces are set.
     */
    public boolean isValidInvocationContext() {
        return (null != convertToContextType()
                && StringUtils.isNotBlank(getClientId())
                && StringUtils.isNotBlank(getClientSystemId())
                && StringUtils.isNotBlank(getWorkplaceId()));
    }
}
