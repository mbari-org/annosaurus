![MBARI logo](annosaurus/src/site/images/logo-mbari-3b.png)

# annosaurus

![Build](https://github.com/mbari-org/annosaurus/actions/workflows/scala.yml/badge.svg)
 [![DOI](https://zenodo.org/badge/90171432.svg)](https://zenodo.org/badge/latestdoi/90171432)

## tl;dr

A web-service for creating image and video annotations. Swagger docs are available in your instance at `http://yourhostname.domain:<port>/docs`. __Here's an example of how to launch it using Docker:__

```bash
docker run -d \
    -p 8080:8080 \
    -e BASICJWT_CLIENT_SECRET="xxxx" \
    -e BASICJWT_SIGNING_SECRET="xxxx" \
    -e DATABASE_DRIVER="com.microsoft.sqlserver.jdbc.SQLServerDriver" \
    -e DATABASE_LOG_LEVEL=INFO \
    -e DATABASE_PASSWORD="xxx" \
    -e DATABASE_URL="jdbc:sqlserver://database.mbari.org:1433;databaseName=M3_ANNOTATIONS" \
    -e DATABASE_USER=dbuser \
    -e LOGBACK_LEVEL=WARN \
    --name=annosaurus \
    --restart unless-stopped \
    mbari/annosaurus
```

## Overview

The service in this repository is one component of our [Video Annotation and Reference System](https://github.com/mbari-org/m3-quickstart). _annosaurus_ is a REST-based web service that stores and retrieves annotations for videos and images. It is designed to work as a programming-language agnostic API that can be accessed from any programming language. The goal of this project is to provide a data service that allows developers and scientists to easily build their own tools for annotating video and images collections.

This service stands on its own and does not require any other video annotations services. It does require a database, either PostgreSQL or SQL Server.

## How-To

- [Security Handshake](annosaurus/src/site/docs/howto/security_handshake.md)

## Data Model

### Class Diagram

![Data Model](annosaurus/src/site/images/annosaurus_classes.png)

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

![ER Model](annosaurus/src/site/images/sqlserver-er-diagram.png)

## Building

This project is built using [SBT](http://www.scala-sbt.org/). To build a distribution from source, run `sbt stage`. The compiled application will be in `annosaurus/target/universal/stage`. You can start the service using `annosaurus/target/universal/stage/bin/annosaurus`.


## Deployment

Refer to [DEPLOYMENT.md](annosaurus/src/site/docs/DEPLOYMENT.md) for production deployment instructions.


## Related Projects

- [vampire-squid](https://github.com/mbari-org/vampire-squid): A service for registering and locating video files.
