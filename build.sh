#!/usr/bin/env bash

echo "--- Building annosaurus (reminder: run docker login first!!)"

BUILD_DATE=`date -u +"%Y-%m-%dT%H:%M:%SZ"`
VCS_REF=`git tag | sort -V | tail -1`

SCRIPT_DIR="$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"
echo "Working directory is $SCRIPT_DIR"
cd $SCRIPT_DIR

sbt stage

ARCH=$(uname -m)
if [[ $ARCH == 'arm64' ]]; then
    # https://betterprogramming.pub/how-to-actually-deploy-docker-images-built-on-a-m1-macs-with-apple-silicon-a35e39318e97
    docker buildx build \
      --platform linux/amd64,linux/arm64 \
      -t mbari/annosaurus:${VCS_REF} \
      -t mbari/annosaurus:latest \
      --push . && \
    docker pull mbari/annosaurus:${VCS_REF}
else
    docker build --build-arg BUILD_DATE=$BUILD_DATE \
                 --build-arg VCS_REF=$VCS_REF \
                  -t mbari/annosaurus:${VCS_REF} \
                  -t mbari/annosaurus:latest . && \
    docker push mbari/annosaurus
fi
 

# For M1 use:
# docker buildx build --load  -t mbari/annosaurus:latest .


# sbt pack && \
#     docker build -t mbari/annosaurus:latest