#!/usr/bin/env bash

echo "--- Building annosaurus (reminder: run docker login first!!)"

sbt pack && \
    docker build -t mbari/annosaurus:java-11 . && \
    docker push mbari/annosaurus:java-11