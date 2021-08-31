#!/bin/bash
mkdir -p secret
cp $1/* secret
mvn clean compile exec:java -Dspring.config.additional-location=file:$1/application.yml
rm -f secret/*
rmdir secret
