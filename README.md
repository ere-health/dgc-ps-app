# dgc-ps-app
DGC Primary System Desktop Client Application of the Gematik TI

### Set Up Video

https://www.youtube.com/watch?v=0Cyj9nBQzO0

### Result for the doctor

![](img/browser-pdf.png)

Our solution will show the pdf to the doctor.
 
### Result for the patient

![](img/impfzertifikat-anonymized.png)

In the end the patient can scan the QR code with the [Corona-Warn-App](https://github.com/corona-warn-app), the [CovPass app Android](https://play.google.com/store/apps/details?id=de.rki.covpass.app) or [iOS](https://apps.apple.com/de/app/covpass/id1566140352)

### Overview
The dgc-ps-app is comprised of two main components. 

* The dgc-ps-app back-end which is a Java 11 Quarkus (https://quarkus.io/) application.
* The dgc-ps-app front-end UI, which is a browser based HTML, CSS and JavaScript application.


### Configuring the DGC-PS-App Front-End UI

Open a terminal window and do the following:

* Clone the ere-ps-app back-end repository by running: 
  > git clone https://github.com/ere-health/dgc-ps-app.git

The source files of the front-end UI will be located in the following directory location:
  > src/main/resources/META-INF/resources/dgc/

At this point, you should now have access to the source files for both the backend and front-end of 
the application. The forms support helpers to [pre-fill parameters](doc/FORMS.md) using an url.

### Routing
In order to reach the German certificate API (that is used by this system), the corresponding
routing needs to be set up throgh the card connector.

#### Linux
```
ip route add 100.102.0.0/16 via <IP_OF_THE_CARD_CONNECTOR>
```
(depending on the used linux distribution)

#### Windows
```
route ADD 100.102.0.0 MASK 255.255.0.0 <IP_OF_THE_CARD_CONNECTOR>
```

### Running the DGC-PS-App Application

* #### Software Requirements
  1. Download and install the latest version of the OpenJDK 11 SDK. You can use your preferred 
     package manager software on your computer to handle this, or simply download an archive or 
     installer from a publishing site such as AdoptOpenJDK (https://adoptopenjdk.net/).  Make sure 
     to choose OpenJDK 11 (LTS) and the HotSpot version of the JVM.
     
  2. Download and install the latest version of Apache Maven (https://maven.apache.org/). 
    
  3. The latest Chrome Browser (https://www.google.com/chrome/).
 
    
* #### Running the Application (Development Mode)
  Open a terminal window and change to the parent dgc-ps-app directory of the back-end and then run 
  the following commands:
  
  Then run:
  
  > mvn quarkus:dev
  
  At this point, the application should be running as highlighted below.
  
  ```shell
    __  ____  __  _____   ___  __ ____  ______ 
     --/ __ \/ / / / _ | / _ \/ //_/ / / / __/ 
     -/ /_/ / /_/ / __ |/ , _/ ,< / /_/ /\ \   
    --\___\_\____/_/ |_/_/|_/_/|_|\____/___/   
    2021-06-14 08:25:16,406 INFO  [hea.ere.ps.ser.dgc.DigitalGreenCertificateService] (Quarkus Main Thread) Application started go to: http://localhost:8080/dgc/covid-19-certificate.html
    2021-06-14 08:25:16,479 INFO  [io.quarkus] (Quarkus Main Thread) ere-ps-app 1.0.0-SNAPSHOT on JVM (powered by Quarkus 1.13.1.Final) started in 2.250s. Listening on: http://0.0.0.0:8080
    2021-06-14 08:25:16,481 INFO  [io.quarkus] (Quarkus Main Thread) Profile dev activated. Live Coding activated.
    2021-06-14 08:25:16,481 INFO  [io.quarkus] (Quarkus Main Thread) Installed features: [cdi, rest-client, resteasy, resteasy-jsonb, servlet]
  ```
  You can access the front-end UI of the application by making reference to the following URL in a 
  browser!
  
  > http://localhost:8080/dgc/covid-19-certificate.html

### Docker image
To create a docker image, the corresponding quarkus extension may be used:
```
mvn package -Dquarkus.container-image.build=true -DskipTests=true
```

This command will create a docker image called `ere.health/dgc:latest` which contains a service
that listens on port 8080. To locally start the image, the command
```
docker run --rm -p 127.0.0.1:8080:8080 ere.health/dgc:latest
```
may be used.

### Environment Variables

For configuration purposes, the environment variables referenced in the `application.properties` file
are located in a file named `.env`. This file should be located in the root project folder
(dgc-ps-app).
See https://quarkus.io/guides/config-reference#environment_variables for additional information.

If needed and the `.env` file is not present, create a copy of the file `.env.example` with name `.env`.
In regard to file and directory paths, configure the values for the environment variables in the
`.env` file to reference paths on your local computer.

> Important! Configure the .env file to be ignored and not checked into the source code repository.

| Environment variable | Description | Example |
| ----- | ----- | ----- |
| `IDP_CONNECTOR_CERT_AUTH_STORE_FILE` | File path to the client certificate that will be used to connect to the connector; may be empty | `files/path/to/certificate.p12` |
| `IDP_CONNECTOR_CERT_AUTH_STORE_FILE_PASSWORD` | Password for accessing the certificate that is configured in `IDP_CONNECTOR_CERT_AUTH_STORE_FILE` | `changeit` |
| `IDP_CLIENT_ID` | Client id for the auth procedure with the IDP to get a token for the certificate creation service; should be `user-access-ti` | `user-access-ti` |
| `IDP_AUTH_REQUEST_REDIRECT_URL` | Redirect URL that will be called by the IDP; should be `connector://authenticated` | `connector://authenticated` |
| `IDP_CONNECTOR_CLIENT_SYSTEM_ID` | Client system id that will be used to get an IDP-token | `client123` |
| `IDP_CONNECTOR_MANDANT_ID` | Mandant that will be used to get an IDP-token | `MANDANT1234` |
| `IDP_CONNECTOR_WORKPLACE_ID` | Workplace id that will be used to get an IDP-token | `12345` |
| `IDP_CONNECTOR_CERTIFICATE_SERVICE_ENDPOINT_ADDRESS` | Endpoint for the certificate SOAP-service | `https://192.168.1.1/CertificateService` | 
| `IDP_CONNECTOR_CARD_HANDLE` | Card handle that will be used to get an IDP-token | `SMB-C-123` |
| `IDP_CONNECTOR_AUTH_SIGNATURE_ENDPOINT_ADDRESS` | Endpoint for the auth signature SOAP-service | `https://192.168.1.1/AuthSignatureService` |
| `SIGNATURE_SERVICE_CONTEXT_MANDANTID` | Mandant that will be used in the signature service | `MANDANT2345` |
| `SIGNATURE_SERVICE_CONTEXT_CLIENTSYSTEMID` | Client system id that will be used for the signature service | `client234` |
| `SIGNATURE_SERVICE_CONTEXT_WORKPLACEID` | Workplace id that will be used for the signature service | `23456` |
| `SIGNATURE_SERVICE_CONTEXT_USERID` | User id that will be used for the signature service | `user123` |
| `CONNECTOR_SIMULATOR_TITUSCLIENTCERTIFICATE` | Client certificate that will be used to access auth signature SOAP-service | `files/path/to/certificate.p12` |
| `CONNECTOR_SIMULATOR_TITUSCLIENTCERTIFICATEPASSWORD` | Password for accessing the certificate that is configured in `CONNECTOR_SIMULATOR_TITUSCLIENTCERTIFICATE` | `changeit` |
| `EVENT_SERVICE_ENDPOINTADDRESS` | Endpoint for the event SOAP-service | `https://192.168.1.1/EventService` |
| `CARD_SERVICE_ENDPOINTADDRESS` | Endpoint for the card SOAP-service | `https://192.168.1.1/CardService` |
| `AUTH_SIGNATURE_SERVICE_ENDPOINTADDRESS` | Endpoint for the auth signature SOAP-service | `https://192.168.1.1/AuthSignatureService` |
| `AUTH_SIGNATURE_SERVICE_SMBCCARDHANDLE` | Card handle that will be used for the auth signature SOAP-service | `SMB-C123` |
