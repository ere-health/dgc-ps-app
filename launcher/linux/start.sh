#!/bin/sh

# This script uses the jar file as its sole argument.

# Configuration; see README.md for details
# The values that are not entered, have to be/ should be (depending on the category) filled out. Keys of values that
# are not entered, should be removed from this config file.

# required
export CONNECTOR_BASE_URI=
export CONNECTOR_USER_ID=

# required unless specified using X-headers, see doc/dgc-openapi.yml
export CONNECTOR_CLIENT_SYSTEM_ID=
export CONNECTOR_WORKPLACE_ID=
export CONNECTOR_MANDANT_ID=

# optional but recommended; adjust to the connector that is in use, especially CONNECTOR_VERIFY_HOSTNAME=true may
# cause some problems as well as CONNECTOR_BASE_URI_CHECK=true
export QUARKUS_HTTP_HOST=127.0.0.1
export CONNECTOR_CERT_AUTH_STORE_FILE=
export CONNECTOR_CERT_AUTH_STORE_FILE_PASSWORD=
export CONNECTOR_CERT_TRUST_STORE_FILE=
export CONNECTOR_CERT_TRUST_STORE_FILE_PASSWORD=
export CONNECTOR_USER_PASSWORD=
export CONNECTOR_VERIFY_HOSTNAME=true
export CONNECTOR_BASE_URI_CHECK=true

# optional
export CONNECTOR_CARD_HANDLE=

# fixed configuration for PU
export IDP_BASE_URL=https://id.impfnachweis.info/auth/realms/bmg-ti-certify
export IDP_CLIENT_ID=user-access-ti
export DIGITAL_GREEN_CERTIFICATE_SERVICE_ISSUERAPIURL=https://api.impfnachweis.info

java -jar $1
