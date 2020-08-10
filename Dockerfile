FROM adoptopenjdk/openjdk11-openj9:alpine-slim
MAINTAINER oliver egger <oliver.egger@ahdis.ch>
EXPOSE 9090
VOLUME /tmp

ARG JAR_FILE=target/i4mi-ipf-hapifhir-gateway-1.0-SNAPSHOT-spring-boot.jar

ENV LANG='en_US.UTF-8' LANGUAGE='en_US:en' LC_ALL='en_US.UTF-8'

COPY ${JAR_FILE} /app.jar

ENTRYPOINT java -Xmx1G -Xshareclasses -Xquickstart -jar /app.jar


# export PROJECT_ID="$(gcloud config get-value project -q)"
# docker build -t eu.gcr.io/${PROJECT_ID}/mag:v013 .
# docker push eu.gcr.io/${PROJECT_ID}/mag:v013
# docker run -d --name mag  -p 9090:9090 --memory="5G" --cpus="1" eu.gcr.io/fhir-ch/mag:v013
