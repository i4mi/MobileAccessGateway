# MobileAccessGateway

The MobileAccessGateway is an implementation based on the [CH EPR mHealth](http://build.fhir.org/ig/ehealthsuisse/ch-epr-mhealth/index.html) implementation guide.  
It provides a FHIR Gateway supporting the PRIM and MHD server actors and uses XDS/PIXV3 to communicate with an XDS Affinity Domain.

It uses [IPF](https://oehf.github.io/ipf/) and [HAPI-FHIR](https://hapifhir.io/). 

| IHE-Profile | ITI           | Transacation Name                                       | IHE Actor                          | Implemented in the Gateway with following actors                                                     |
|-------------|---------------|---------------------------------------------------------|------------------------------------|----------------------------------------------------------------------|
|     PMIR    |     ITI-83    |     Mobile   Patient Identifier Crossreference Query    |     Patient   Identity Manager     |     PIX V3 Patient Identifier      Cross-reference      Consumer     |
|     PMIR    |     ITI-93    |     Mobile   Patient Identity Feed                      |     Patient   Identity Manager     |     PIX V3 Patient Identitiy Source                                  |
|     MHD     |     ITI-65    |     Provide   Document Bundle                           |     Document   Recipient           |     XDS   Document Source, X-Service-User                            |
|     MHD     |     ITI-66    |     Find   Document Manifests                           |     Document   Responder           |     XDS   Document Consumer, X-Service-User                           |
|     MHD     |     ITI-67    |     Find   Document References                          |     Document   Responder           |     XDS   Document Consumer, X-Service-User                           |
|     MHD     |     ITI-68    |     Retrieve   Document                                 |     Document   Responder           |     XDS   Document Consumer, X-Service-User                           |


## Test setup

Current configuration works with [XDSTools7](https://ehealthsuisse.ihe-europe.net/xdstools7/), a [simulator](http://ehealthsuisse.ihe-europe.net:8280/xdstools7/sim/default__ahdis/reg/rb
) is setup where the MobileAccessGateway connects. 

[Patient Manager](https://ehealthsuisse.ihe-europe.net/PatientManager/home.seam) is used for simulating PIX V3.

See [client.http](client.http) for example calls to the Mobile Access Gateway.

## Run the JAR

1. Clone https://github.com/oehf/ipf.git 
2. run mvn clean install -DskipTests in this directory, this should produce 4.0-SNAPSHOT (you need at least jdk11)
3. Clone this repo 
4. Install the dependencies: `mvn install`
5. Either run it from your favorite IDE or in the CLI: `mvn clean compile && mvn exec:java -Dexec.mainClass="ch.bfh.ti.i4mi.mag.MobileAccessGateway"`

## Caution
- a @ComponentScan had to be added to the main Application class, otherwise the routes / component could note  be defined (see open issues)

## Dev environment

### Eclipse setup
- install [lombok](https://projectlombok.org/setup/eclipse)
- in pom.xml xpp3 has to be excluded, otherwise there is an error message with the java compiler (The package javax.xml.namespace is accessible from more than one module: <unnamed>, java.xml)

### VSCode
- Java Extension needed

### open issues
- ipf-platform-camel-ihe-fhir-r4-pixpdq works not nicely with spring-boot together, is the META-INF directory not added to the output source?