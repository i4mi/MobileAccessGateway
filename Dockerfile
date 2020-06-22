FROM adoptopenjdk/openjdk11-openj9:alpine-slim
MAINTAINER oliver egger <oliver.egger@ahdis.ch>
EXPOSE 9091
VOLUME /tmp

ARG JAR_FILE=target/i4mi-ipf-hapifhir-gateway-1.0-SNAPSHOT-spring-boot.jar

ENV LANG='en_US.UTF-8' LANGUAGE='en_US:en' LC_ALL='en_US.UTF-8'

COPY ${JAR_FILE} /app.jar

ENTRYPOINT java -Xmx1G -Xshareclasses -Xquickstart -jar /app.jar


# export PROJECT_ID="$(gcloud config get-value project -q)"
# docker build -t eu.gcr.io/${PROJECT_ID}/mag:v010 .
# docker tag mag eu.gcr.io/${PROJECT_ID}/mag:v010
# docker push eu.gcr.io/${PROJECT_ID}/mag:v010
# docker run -d --name mag  -p 9091:9091 99fbdf8e9b12 --memory="5G" --cpus="1"
