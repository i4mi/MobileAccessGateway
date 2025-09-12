FROM bellsoft/liberica-openjdk-alpine:21
MAINTAINER oliver egger <oliver.egger@ahdis.ch>
EXPOSE 9090
#EXPOSE 9091
VOLUME /tmp

ARG JAR_FILE=target/mobile-access-gateway-1.0-SNAPSHOT-spring-boot.jar

ENV LANG='en_US.UTF-8' LANGUAGE='en_US:en' LC_ALL='en_US.UTF-8'

COPY ${JAR_FILE} /app.jar
#COPY src/main/resources/oiste_wisekey.crt /usr/local/share/ca-certificates/oiste_wisekey.crt
#RUN apk add ca-certificates && update-ca-certificates
#RUN  ["keytool", \
#        "-importcert", \
#        "-noprompt", \
#        "-alias", "oistewisekeyglobalrootgbca", \
#        "-keystore", "$JAVA_HOME/lib/security/cacerts", \
#        "-file", "/usr/local/share/ca-certificates/oiste_wisekey.crt"]

ENTRYPOINT java -Xmx1G -jar /app.jar -Dspring.config.additional-location=file:/config/application.yml

# export PROJECT_ID="$(gcloud config get-value project -q)"
# docker build -t eu.gcr.io/${PROJECT_ID}/mag:v016 .
# docker push eu.gcr.io/${PROJECT_ID}/mag:v016
# docker run -d --name mag  -p 9090:9090 --memory="5G" --cpus="1" eu.gcr.io/fhir-ch/mag:v016
