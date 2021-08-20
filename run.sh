#!/bin/bash
mkdir -p secret
cp $1/* secret
mvn compile exec:java -Dspring.config.additional-location=file:$1/application.yml
rm -f secret/*
rmdir secret
