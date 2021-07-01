#!/bin/sh

# This script uses the jar file as its sole argument.

# Configuration; see README.md for details
# The values that are not entered, have to be/ should be (depending on the category) filled out. Keys of values that
# are not entered, should be removed from this config file.

# required
CONNECTOR_BASE_URI=
CONNECTOR_USER_ID=

# required unless specified using X-headers, see doc/dgc-openapi.yml
CONNECTOR_CLIENT_SYSTEM_ID=
CONNECTOR_WORKPLACE_ID=

# optional but recommended; adjust to the connector that is in use, especially CONNECTOR_VERIFY_HOSTNAME=true may
# cause some problems
QUARKUS_HTTP_HOST=127.0.0.1
CONNECTOR_CERT_AUTH_STORE_FILE=
CONNECTOR_CERT_AUTH_STORE_FILE_PASSWORD=
CONNECTOR_CERT_TRUST_STORE_FILE=
CONNECTOR_CERT_TRUST_STORE_FILE_PASSWORD=
CONNECTOR_USER_PASSWORD=
CONNECTOR_VERIFY_HOSTNAME=true

# optional
CONNECTOR_CARD_HANDLE=

# fixed configuration for PU
IDP_BASE_URL=https://id.impfnachweis.info/auth/realms/bmg-ti-certify
IDP_CLIENT_ID=user-access-ti
DIGITAL_GREEN_CERTIFICATE_SERVICE_ISSUERAPIURL=https://api.impfnachweis.info

java -jar $1
