#!/usr/bin/env bash

echo "--- Building annosaurus (reminder: run docker login first!!)"

sbt pack && \
    docker build -t hohonuuli/annosaurus . && \
    docker push hohonuuli/annosaurus