# Mobile Access Gateway

The Mobile Access Gateway is an implementation based on the [CH EPR mHealth](https://fhir.ch/ig/ch-epr-mhealth/index.html) [(CI-Build)](http://build.fhir.org/ig/ehealthsuisse/ch-epr-mhealth/index.html) implementation guide.  
It provides a FHIR Gateway supporting the PIXm and MHD server actors and uses XDS/PIXV3 to communicate with an XDS Affinity Domain.

It uses [IPF](https://oehf.github.io/ipf/) and [HAPI-FHIR](https://hapifhir.io/).

## Test setup

Current configuration works with [XDSTools7](https://ehealthsuisse.ihe-europe.net/xdstools7/), a [simulator](http://ehealthsuisse.ihe-europe.net:8280/xdstools7/sim/default__ahdis/reg/rb) is setup where the Mobile Access Gateway connects.

[Patient Manager](https://ehealthsuisse.ihe-europe.net/PatientManager/home.seam) is used for simulating PIX V3.

See [client.http](client.http) for example calls to the Mobile Access Gateway.

## Run the JAR

1. Clone https://github.com/oehf/ipf.git
2. run mvn clean install -DskipTests in this directory, this should produce 4.0-SNAPSHOT (you need at least jdk11)
3. Clone this repo
4. Install the dependencies: `mvn install`
5. Either run it from your favorite IDE or in the CLI: `mvn clean compile && mvn exec:java -Dexec.mainClass="ch.bfh.ti.i4mi.mag.MobileAccessGateway"`

To run your own configuration stored in a properties file use the `-Dspring.config.additional-location` switch.
Any config parameter that is not specified in the file will be taken from the defaults.
If your config file is called "myownconfig.properties" run it using:
`mvn clean compile && mvn exec:java -Dexec.mainClass="ch.bfh.ti.i4mi.mag.MobileAccessGateway" -Dspring.config.additional-location=file:myownconfig.properties`


### publish docs

documentation is maintained in docs folder using [mkdocs-material](https://squidfunk.github.io/mkdocs-material/):

- develop docs: mkdocs serve
- publish docs: mkdocs gh-deploy --force

docs are then available at https://ahdis.github.io/matchbox/

## Caution

- a @ComponentScan had to be added to the main Application class, otherwise the routes / component could note be defined (see open issues)

## Dev environment

### Eclipse setup

- install [lombok](https://projectlombok.org/setup/eclipse)
- in pom.xml xpp3 has to be excluded, otherwise there is an error message with the java compiler (The package javax.xml.namespace is accessible from more than one module: <unnamed>, java.xml)

### VSCode

- Java Extension needed

### open issues

- ipf-platform-camel-ihe-fhir-r4-pixpdq works not nicely with spring-boot together, is the META-INF directory not added to the output source?

## GUI

The GUI is an Angular project stored in the _angular/_ directory. The compiled project is stored in the Java
application resources (_src/main/resources/static/_). If you modify the Angular project, you have to rebuild it:

```bash
cd angular
npm install
npm run build-mag
```

## Deployment

The Mobile Access Gateway can run in a docker container and can be deployed to a Kubernetes cluster.

### Building an image

To create a new docker image run:

```
mvn clean package
docker build -t mag:v030 .
```

Where "mag" is the image name and v030 is the version. Then push to a registry.

### Creating a configuration

- Create an empty folder ("**myconfig**" in this example) and copy the contents of the example-config directory.
- Edit the application.yml. Leave the pathes for the keystores as they are.
- Provide p12 or jks keystores for the client certificate, the server certificate and for IDP.

### Deploying to Kubernetes

- Edit myconfig/kubernetes-config.yml as you need it
- Create a config map for "application.yml"
  `kubectl create configmap mobile-access-gateway-configmap --from-file=application.yml=myconfig/application.yml`
- Create a secret for the certificates and keys
  `kubectl create secret generic mobile-access-gateway-secret --from-file=client.jks=myconfig/client-certificate.jks --from-file=server.p12=myconfig/server-certificate.jks --from-file=idp.jks=myconfig/idp.jks`
- Upload configuration
  `kubectl apply -f myconfig/kubernetes-config.yml`
