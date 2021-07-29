#!/bin/bash
cp $1/* /secret
mvn compile exec:java -Dspring.config.additional-location=file:$1/application.yml
