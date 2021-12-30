#!/usr/bin/env bash

echo "--- Building annosaurus (reminder: run docker login first!!)"

BUILD_DATE=`date -u +"%Y-%m-%dT%H:%M:%SZ"`
VCS_REF=`git tag | sort -V | tail -1`


# sbt pack && \
    docker build --build-arg BUILD_DATE=$BUILD_DATE \
                 --build-arg VCS_REF=$VCS_REF \
                  -t mbari/annosaurus:${VCS_REF} \
                  -t mbari/annosaurus:latest . && \
    docker push mbari/annosaurus

# sbt pack && \
#     docker buildx build \
#         --platform linux/amd64,linux/arm64 \
#         -t mbari/annosaurus:${VCS_REF} \
#         -t mbari/annosaurus:latest 
#         --push . 

# For M1 use:
# docker buildx build --load -t mbari/annosaurus:${VCS_REF} -t mbari/annosaurus:latest .
