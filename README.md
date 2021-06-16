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
the application.


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
  
### Environment Variables
#### Development Mode
For dev mode purposes, the environment variables referenced in the application.properties file 
are located in a file named .env. This file should be located in the root project folder 
(ere-ps-app).

In regards to file and directory paths, configure the values for the environment variables in the
.env file to reference paths on your local computer.

> Important! Configure the .env file to be ignored and not checked into the source code repository.

