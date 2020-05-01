# IPF HAPI-FHIR IHE ITI springboot example 

This is a simple [IPF](https://oehf.github.io/ipf/) [HAPI-FHIR](https://hapifhir.io/) example. It demonstrates a mock [IHE PIXm actor Patient Identifier Crossreference Manager](https://oehf.github.io/ipf-docs/docs/ihe/iti83/) for the ITI-83 transaction. It does not use any Groovy code or XML configuration files for Camel, everything is done in Java code and in a single YAML file for the Spring Boot configuration. This example is based on a combination of [qligier/ipf-example](https://github.com/qligier/ipf-example) and [ipf-tutorials-fhir](https://github.com/oehf/ipf/tree/master/tutorials/fhir). 


## Run the JAR

1. Install the dependencies: `mvn install`
2. Either run it from your favorite IDE or in the CLI: `mvn clean compile && mvn exec:java -Dexec.mainClass="ch.ahdis.ipf.mag.MobileAccessGateway"`

### [ITI-83] PIXm Query
You should be able to run the query which returns for the sourceIdentifier 0815 in urn:oid:1.2.3 "mockid" in targetSystem urn:oid:1.2.3.4.6 in a console application with curl (use client.http with VSCode rest extension).

```
curl --request GET \
  --url 'http://localhost:9091/fhir/Patient/$ihe-pix?sourceIdentifier=urn%3Aoid%3A1.2.3.4%7C0815&targetSystem=urn%3Aoid%3A1.2.3.4.6' \
  --header 'accept: application/fhir+json' \
  --header 'content-type: application/fhir+json' 
```

and the response should be:

```
{
  "resourceType": "Parameters",
  "parameter": [
    {
      "name": "targetIdentifier",
      "valueIdentifier": {
        "system": "urn:oid:1.2.3.4.6",
        "value": "mockid"
      }
    }
  ]
}

```


## Caution

- a @ComponentScan had to be added to the main Application class, otherwise the routes / component could note  be defined (see open issues)


## Dev environment


### Eclipse setup
- install [groovy-eclipse plugin](https://github.com/groovy/groovy-eclipse)
- install [lombok](https://projectlombok.org/setup/eclipse)
- open [issue](https://groups.google.com/forum/?utm_medium=email&utm_source=footer#!msg/ipf-dev/DBDXZv3kfHE/hcg62rElBAAJ) when importing maven ipf projects in eclipse: "message": "The import org.openehealth.ipf.commons.map.BidiMappingService cannot be resolved" 

### open issues
- ipf-platform-camel-ihe-fhir-r4-pixpdq works not nicely with spring-boot together, is the META-INF directory not added to the output source?