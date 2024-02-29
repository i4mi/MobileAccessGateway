# mobile access container

you can download the mobile access gateway as a docker container:

```
docker pull europe-west6-docker.pkg.dev/ahdis-ch/ahdis/mag:v062
```

## Configurable base image:

run from the cloned [mobile access gateway](https://github.com/i4mi/MobileAccessGateway):

```bash
docker run -d -it --name mag -p 9090:9090 -v /Users/oegger/Documents/github/MobileAccessGateway/example-playground:/config/ europe-west6-docker.pkg.dev/ahdis-ch/ahdis/mag:v062
docker logs --follow mag
```

Server endpoint will then be accessible at http://localhost:9090/mag/fhir/metadata

You can check for example a PIXm query against the EPD playground:

```http
http://localhost:9090/mag/fhir/Patient/$ihe-pix?sourceIdentifier=urn%3Aoid%3A2.16.756.5.30.1.127.3.10.3%7C761337615395845832&targetSystem=urn%3Aoid%3A1.1.1.99.1&targetSystem=urn%3Aoid%3A2.16.756.5.30.1.127.3.10.3
```

The mobile access gateway supports also a GUI which is accessible at http://localhost:9090/mag/#/.

## Live and Readiness checks

To check if the container is live and ready you can check the health:

```http
GET http://localhost:9090/mag/actuator/health HTTP/1.1
Accept: application/vnd.spring-boot.actuator.v3+json

HTTP/1.1 200
Content-Type: application/vnd.spring-boot.actuator.v3+json
Transfer-Encoding: chunked
Date: Thu, 02 Feb 2023 15:55:12 GMT
Via: 1.1 google
Alt-Svc: h3=":443"; ma=2592000,h3-29=":443"; ma=2592000
Connection: close

{
  "status": "UP"
}
```