#!/usr/bin/env bash

echo "--- Building annosaurus (reminder: run docker login first!!)"

sbt pack && \
    docker build -t mbari/annosaurus . && \
    docker push mbari/annosaurus