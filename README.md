![MBARI logo](src/site/images/logo-mbari-3b.png)

# annosaurus

[![Build Status](https://travis-ci.org/mbari-media-management/annosaurus.svg?branch=master)](https://travis-ci.org/mbari-media-management/annosaurus)  [![DOI](https://zenodo.org/badge/90171432.svg)](https://zenodo.org/badge/latestdoi/90171432)

## tl;dr

A web-service for creating image and video annotations. 

We're working on API documentation; [it's still incomplete](https://i.pinimg.com/originals/10/a0/cc/10a0cc1e941644f78ef777a946898148.jpg). You can view them in their _in-progress_ state on [swaggerhub](https://app.swaggerhub.com/apis/mbari/annosaurus/1.0.0-oas3)

Here's an example of how to launch it using Docker:

```bash
docker run -d \
    -p 8100:8080 \
    -e AUTHENTICATION_SERVICE="org.mbari.vars.annotation.auth.BasicJwtService" \
    -e BASICJWT_CLIENT_SECRET="xxxx" \
    -e BASICJWT_SIGNING_SECRET="xxxx" \
    -e DATABASE_ENVIRONMENT=production \
    -e DATABASE_LOG_LEVEL=INFO \
    -e HTTP_CONTEXT_PATH="/anno" \
    -e LOGBACK_LEVEL=WARN \
    -e ORG_MBARI_VARS_ANNOTATION_DATABASE_PRODUCTION_DRIVER="com.microsoft.sqlserver.jdbc.SQLServerDriver" \
    -e ORG_MBARI_VARS_ANNOTATION_DATABASE_PRODUCTION_NAME=SQLServer \
    -e ORG_MBARI_VARS_ANNOTATION_DATABASE_PRODUCTION_PASSWORD="xxx" \
    -e ORG_MBARI_VARS_ANNOTATION_DATABASE_PRODUCTION_URL="jdbc:sqlserver://database.mbari.org:1433;databaseName=M3_ANNOTATIONS" \
    -e ORG_MBARI_VARS_ANNOTATION_DATABASE_PRODUCTION_USER=dbuser \
    --name=annosaurus \
    --restart unless-stopped \
    mbari/annosaurus

```

## Overview

MBARI is updating its [Video Annotation and Reference System](https://hohonuuli.github.io/vars/) for modern video workflows. The service in this repository is one component of our next generation system. _annosaurus_ stores and retrieves annotations for videos and images. It is designed to work as a programming-language agnostic API that can be accessed from any programming language. The goal of this project is to provide a data service that allows developers and scientists to easily build their own tools for annotating video and images collections.

This service stands on its own and does not require any other video annotations services. Out-of-the-box it provides an in-memory database suitable for testing and development. For production use, a external database is required. Setup for your own database is very straightforward.

## How-To

- [Security Handshake](src/site/docs/howto/security_handshake.md)

## Data Model

### Class Diagram

![Data Model](src/site/images/annosaurus_classes.png)

- `ImagedMoment`: Reference to some index in a particular video. It can contain zero or more _Observations_ and zero or more _ImageReferences_. It can use any or all of the following as indices, but at least one _must_ be present:

  - _recordedDate_: The moment in time when the frame or image was recorded
  - _timecode_: Typically this is a tape timecode, but it could be pulled from a timecode track in a video too.
  - _elapsedTime_: This is the time since the start of the video clip. This is the most commonly used index for video files.

- `Observation`: Represents an `annotation`. Includes the annotation term (i.e. concept), an optional duration, and tracks who made the observation. The group and activity fields can be used to further categorize annotations for example at MBARI we might use groups like: _images_, _ROV_, and _AUV_ and activities like: _descent_, _ascent_, _transect_, and _cruising_.
- `Association`: Information that augments an observation. Very flexible, the format is `linkName | toConcept | linkValue`. Some examples; `eating | Aegina | nil`, `surface-color | self | red`, `audio-comment | nil | first sighting on this mission`.
- `ImageReference`: Images, such as framegrabs, linked to the moment. It will also be possible to load image references for image annotation projects.
- `CachedAnxillaryData`: For performance reason, we may want to cache some time indexed information, such as position, CTD, etc, in side the same database as the annotations.
- `CachedMissionInfo`: This may contain information describing a camera deployment.

### ER Diagram

![ER Model](src/site/images/sqlserver-er-diagram.png)

## Building

This project is built using [SBT](http://www.scala-sbt.org/). To build a distribution from source, run `sbt pack`. The compiled application will be in `target/pack`. You can start the service using `target/pack/binjetty-main`.

## Testing

To take it for a quick spin using an in-memory (i.e. temporary) database, you do _not_ need to clone or build the project. Instead, install [Docker](https://www.docker.com/) and run the following:

`docker run --name=annosaurus -p 8080:8080 mbari/annosaurus`

You can shut it down using:

```
docker stop annosaurus && docker rm annosaurus
```

## Deployment

Refer to [DEPLOYMENT.md](DEPLOYMENT.md) for production deployment instructions.

## Usage

We are putting together an API doc and tutorial on usage. That's just been started but you can see some python examples in [src/pack/bin](src/pack/bin)

## Related Projects

- [vampire-squid](https://github.com/underwatervideo/vampire-squid): A service for registering and locating video files.
