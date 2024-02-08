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
docker build -t mag:v059 .
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
  
## Adding a new identity provider

### 1. Preparations
Select a short identifier (no spaces, no special characters) for the IDP you want to connect to. This identifier will be called <idp-name> in this guide. (example: "trustid") 

Which instance of the MAG should be able to connect to the IDP? You need a separate registration for each instance. The base-url of the MAG instance including protokoll, domain, port (if not 80 or 443) and base path will be called <baseurl>. (example: "https://test.ahids.ch/mag-test")    

### 2. Ask for IDP metadata
You need a metadata file from the IDP. Either this metadata is freely available in the internet under a fixed URL or you need to receive the file through another channel and store the metadata XML file in the MAGs configuration directory.
An example IDP metadata file (for gazelle) can be found here: https://ehealthsuisse.ihe-europe.net/metadata/idp-metadata.xml
 
### 3. Generate signing and encryption keys
Signing and encryption keys for SAML are in an extra keystore specified in the "mag.iua.idp" section. It is possible to reuse keys from the keystore for multiple IDPs, but it might be better
to have separate keys for each provider so that you can exchange the keys for a single provider only. Each provider might have different requirements about the keys and certificates used.
(for example the certificate needs to contain a support email address) 
Here is an example how to create a signing key. Adopt according to requirements of IDP.
```
openssl req -x509 -sha256 -nodes -days 365 -newkey rsa:4096 -keyout signKey.key -out signKey.crt
```

Repeat the same for the TLS key: (another example with extended requirements)
```
openssl req -x509 -sha256 -nodes -days 1460 -newkey rsa:4096 -extensions client_ext -extfile myssl.conf -outform PEM -keyout tlsKey.key -out tlsKey.cer 

myssl.conf must contain section:

# Client Certificate Extensions
[ client_ext ]
basicConstraints        = CA:FALSE
keyUsage                = critical, digitalSignature
extendedKeyUsage        = critical, clientAuth
subjectKeyIdentifier    = hash
authorityKeyIdentifier  = keyid:always
issuerAltName           = issuer:copy

```

Convert your keys into keystores and merge those.
Choose an "alias" (a name) for both keys. Replace <sign-key-alias> and <tls-key-alias> with the chosen aliases.
```
openssl pkcs12 -export -in signKey.crt -inkey signKey.key -name "<sign-key-alias>" -out signKey.p12
openssl pkcs12 -export -in tlsKey.crt -inkey tlsKey.key -name "<tls-key-alias>" -out tlsKey.p12
keytool -importkeystore -srckeystore signKey.p12 -destkeystore tlsKey.p12 -alias <sign-key-alias>
```

### 5. Change MAGs configuration file
In the application.yml file for the MAG instance add a section to "mag.iua.idps". A section looks like this:

```
mag:
  iua:
     idps:
        <idp-name>:
          metadata-url: <metadata-url>
          key-alias: <sign-key-alias>
          key-password: <sign-key-password>
          tls-key-alias: <tls-key-alias>
          tls-key-password: <tls-key-password>    
```
The <metadata-url> is either the URL from step 2 or if you have a file instead it is the file path (without a prefix) for example "secret/metadata.xml".
If the IDP has problems with the artifact resolution step you can put "noArtifactResolution: true" into the section.
For the <sign-key-password> and <tls-key-password> use the keystore password for keys without additional password.

### 6. Run MAG and retrieve SP metadata file/url
Run the MAG with the updated configuration. You can now download the SP metadata file which needs to be communicated to the IDP provider.
The metadata XML file may be downloaded from <baseurl>/saml/metadata/alias/<idp-name>
This file contains certificates, the SP entityID and URLs.

### 7. Register instance to IDP
Write an email to the IDP provider with either the metadata file itself and/or the URL where the metadata file can be downloaded. 

### 8. Adopt frontend
In angular/src/app/mag/mag.component.html is the provider select list which may be extended with new entries.
