package health.ere.ps.service.idp.client;

import health.ere.ps.exception.idp.IdpException;
import health.ere.ps.exception.idp.IdpClientException;
import health.ere.ps.exception.idp.IdpJoseException;
import health.ere.ps.exception.idp.crypto.IdpCryptoException;
import health.ere.ps.model.idp.client.IdpTokenResult;
import health.ere.ps.model.idp.crypto.PkiIdentity;

public interface IIdpClient {

    /**
     * Log into the idp.
     *
     * @param idpIdentity  client identity
     * @param mandantId    mandant (optional; if not supplied, configuration values will be used)
     * @param clientSystem client system id (optional; if not supplied, configuration values will be used)
     * @param workplace    workplace id (optional; if not supplied, configuration values will be used)
     * @param cardHandle   card handle (optional; if not supplied, configuration values will be used)
     * @return idp token result
     */
    IdpTokenResult login(PkiIdentity idpIdentity, String mandantId, String clientSystem, String workplace, String cardHandle) throws IdpException, IdpClientException, IdpJoseException, IdpCryptoException;

    default IdpTokenResult login(PkiIdentity idpIdentity) throws IdpException, IdpClientException, IdpJoseException, IdpCryptoException {
        return login(idpIdentity, null, null, null, null);
    }

    IIdpClient initializeClient() throws IdpClientException, IdpException, IdpCryptoException, IdpJoseException;
}
