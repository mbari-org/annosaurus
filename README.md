![MBARI logo](annosaurus/src/site/images/logo-mbari-3b.png)

# annosaurus

[![Ask DeepWiki](https://deepwiki.com/badge.svg)](https://deepwiki.com/mbari-org/annosaurus) ![Build](https://github.com/mbari-org/annosaurus/actions/workflows/scala.yml/badge.svg) [![DOI](https://zenodo.org/badge/90171432.svg)](https://zenodo.org/badge/latestdoi/90171432)

A RESTful microservice for creating and managing video and image annotations. Built with Scala, Tapir, and Hibernate, annosaurus provides a type-safe, language-agnostic API for annotation management with support for PostgreSQL and SQL Server.

## Features

- **RESTful API** - Programming-language agnostic REST interface with auto-generated Swagger documentation
- **Flexible Indexing** - Support for multiple time indices (recordedDate, timecode, elapsedTime)
- **Rich Annotations** - Complex associations, image references, and cached ancillary data
- **JWT Authentication** - Secure endpoints with JWT Bearer token support
- **Multi-Database Support** - Works with PostgreSQL or SQL Server
- **Database Migrations** - Automated schema management with Flyway
- **Docker Ready** - Available as multi-platform Docker images (amd64/arm64)
- **Health Monitoring** - Built-in health checks and Prometheus metrics

## Quick Start

### Using Docker (Recommended)

**PostgreSQL:**
```bash
docker run -d \
    -p 8080:8080 \
    -e BASICJWT_CLIENT_SECRET="your-client-secret" \
    -e BASICJWT_SIGNING_SECRET="your-signing-secret" \
    -e DATABASE_DRIVER="org.postgresql.Driver" \
    -e DATABASE_URL="jdbc:postgresql://localhost:5432/annotations" \
    -e DATABASE_USER="dbuser" \
    -e DATABASE_PASSWORD="dbpass" \
    -e LOGBACK_LEVEL=INFO \
    --name=annosaurus \
    --restart unless-stopped \
    mbari/annosaurus
```

**SQL Server:**
```bash
docker run -d \
    -p 8080:8080 \
    -e BASICJWT_CLIENT_SECRET="your-client-secret" \
    -e BASICJWT_SIGNING_SECRET="your-signing-secret" \
    -e DATABASE_DRIVER="com.microsoft.sqlserver.jdbc.SQLServerDriver" \
    -e DATABASE_URL="jdbc:sqlserver://localhost:1433;databaseName=annotations" \
    -e DATABASE_USER="dbuser" \
    -e DATABASE_PASSWORD="dbpass" \
    -e LOGBACK_LEVEL=INFO \
    --name=annosaurus \
    --restart unless-stopped \
    mbari/annosaurus
```

Once running, access the Swagger API documentation at `http://localhost:8080/docs`.

## Overview

Annosaurus is one component of MBARI's [Video Annotation and Reference System](https://github.com/mbari-org/m3-quickstart). It is a REST-based web service that stores and retrieves annotations for videos and images, providing a language-agnostic API so developers and scientists can build their own annotation tools against any tech stack.

The service is self-contained and requires only a database — either PostgreSQL or SQL Server.

## Documentation

- [Security Handshake](annosaurus/src/site/docs/howto/security_handshake.md)
- [Developer Guide (CLAUDE.md)](CLAUDE.md) - Architecture and development patterns
- [Deployment Guide](annosaurus/src/site/docs/DEPLOYMENT.md) - Production deployment

## Data Model

### Class Diagram

```mermaid
classDiagram
    class ImagedMoment {
        +UUID uuid
        +UUID videoReferenceUuid
        +Instant recordedTimestamp
        +Timecode timecode
        +Duration elapsedTime
    }
    class Observation {
        +UUID uuid
        +String concept
        +String observer
        +Instant observationTimestamp
        +Duration duration
        +String group
        +String activity
    }
    class Association {
        +UUID uuid
        +String linkName
        +String toConcept
        +String linkValue
        +String mimeType
    }
    class ImageReference {
        +UUID uuid
        +URL url
        +String description
        +Integer width
        +Integer height
        +String format
    }
    class CachedAncillaryDatum {
        +UUID uuid
        +Double latitude
        +Double longitude
        +Float depthMeters
        +Float altitude
        +Float salinity
        +Float temperatureCelsius
        +Float oxygenMlL
        +Float pressureDbar
        +Float lightTransmission
        +Double x
        +Double y
        +Double z
        +Double phi
        +Double theta
        +Double psi
        +String crs
        +String posePositionUnits
    }
    class CachedVideoReferenceInfo {
        +UUID uuid
        +UUID videoReferenceUuid
        +String missionId
        +String platformName
        +String missionContact
    }

    ImagedMoment "1" --> "0..*" Observation : contains
    ImagedMoment "1" --> "0..*" ImageReference : has
    ImagedMoment "1" --> "0..1" CachedAncillaryDatum : has
    Observation "1" --> "0..*" Association : has
```

- `ImagedMoment`: A reference to a specific point in a video or image collection. It can hold zero or more _Observations_ and zero or more _ImageReferences_. At least one of the following time indices must be present:

  - _recordedTimestamp_: The wall-clock time when the frame was captured.
  - _timecode_: A tape or video-track timecode (e.g. `HH:MM:SS:FF`).
  - _elapsedTime_: Time elapsed since the start of the video clip — the most commonly used index for video files.

- `Observation`: A single annotation at an `ImagedMoment`. Records the concept (taxon or object label), observer, timestamp, optional duration, and optional `group` and `activity` tags. At MBARI, groups include _ROV_, _AUV_, and _images_; activities include _descent_, _ascent_, _transect_, and _cruising_.
- `Association`: Structured metadata that augments an observation. The format is `linkName | toConcept | linkValue`, for example: `eating | Aegina | nil`, `surface-color | self | red`, or `audio-comment | nil | first sighting on this mission`.
- `ImageReference`: An image or framegrab linked to an `ImagedMoment`, identified by URL. Used for both video framegrabs and standalone image annotation projects.
- `CachedAncillaryDatum`: Time-indexed environmental and positional data (latitude, longitude, depth, CTD readings, vehicle pose, etc.) cached alongside annotations for query performance.
- `CachedVideoReferenceInfo`: Deployment metadata for a video reference — platform name, mission ID, and mission contact — cached to avoid round-trips to the video asset service.

## Prerequisites

- **JDK 21** - Required for building and running
- **SBT** - Scala Build Tool for compilation
- **Database** - PostgreSQL 12+ or SQL Server 2019+
- **Docker** (optional) - For containerized deployment

## Building from Source

This project uses [SBT (Scala Build Tool)](http://www.scala-sbt.org/).

```bash
# Compile the project
sbt compile

# Build a distribution
sbt stage

# The compiled application will be at:
# annosaurus/target/universal/stage/

# Run the service
annosaurus/target/universal/stage/bin/annosaurus
```

### Building Docker Image

```bash
# First, stage the application
sbt stage

# Build the Docker image
docker build -t mbari/annosaurus .
```

## Configuration

The service is configured through `application.conf` or environment variables. Key configuration options:

| Environment Variable | Description | Example |
|---------------------|-------------|---------|
| `DATABASE_URL` | JDBC connection URL | `jdbc:postgresql://localhost:5432/annotations` |
| `DATABASE_DRIVER` | JDBC driver class | `org.postgresql.Driver` |
| `DATABASE_USER` | Database username | `dbuser` |
| `DATABASE_PASSWORD` | Database password | `dbpass` |
| `BASICJWT_CLIENT_SECRET` | JWT client secret | `your-secret` |
| `BASICJWT_SIGNING_SECRET` | JWT signing secret | `your-secret` |
| `HTTP_PORT` | HTTP server port | `8080` |
| `LOGBACK_LEVEL` | Logging level | `INFO`, `DEBUG`, `WARN` |

For complete configuration options, see `annosaurus/src/universal/conf/application.conf`.

### NATS Messaging

Annosaurus can optionally publish change notifications to a [NATS](https://nats.io) messaging server whenever observations or associations are created, updated, or deleted. This is disabled by default.

| Environment Variable | Description | Default |
|---------------------|-------------|---------|
| `MESSAGING_NATS_ENABLE` | Enable NATS publishing (`true`/`false`) | `false` |
| `MESSAGING_NATS_URL` | NATS server URL | `nats://localhost:4222` |
| `MESSAGING_NATS_TOPIC` | NATS subject/topic to publish to | `vars` |

Equivalent `application.conf` keys:

```hocon
messaging.nats {
  enable = true
  url    = "nats://nats-server:4222"
  topic  = "vars"
}
```

Each published message is a compact JSON notification — not the full record. Receivers should query the REST API for the full object using the provided UUID.

```json
{
  "action":   "CREATED",
  "dataType": "OBSERVATION",
  "uuid":     "018f1a2b-3c4d-7e5f-8a9b-0c1d2e3f4a5b"
}
```

- **action** — one of `CREATED`, `UPDATED`, or `DELETED`
- **dataType** — one of `OBSERVATION` or `ASSOCIATION`
- **uuid** — the UUID of the affected record

## API Documentation

Once the service is running, Swagger documentation is available at:
- `http://localhost:8080/docs` (interactive API explorer)

### Authentication

Most endpoints require JWT authentication. Include the token in the Authorization header:
```bash
Authorization: Bearer <your-jwt-token>
```

Generate a JWT token using the `/auth/authorize` endpoint.

### Example API Calls

**Create an annotation:**
```bash
curl -X POST http://localhost:8080/v1/observations \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "concept": "Nanomia bijuga",
    "observer": "researcher",
    "videoReferenceUuid": "uuid-here",
    "recordedTimestamp": "2024-01-15T10:30:00Z"
  }'
```

**Query annotations:**
```bash
curl -X GET "http://localhost:8080/v1/query?concept=Nanomia%20bijuga" \
  -H "Authorization: Bearer <token>"
```

## Testing

The project includes comprehensive integration tests for both PostgreSQL and SQL Server.

```bash
# Run unit tests
sbt annosaurus/test

# Run PostgreSQL integration tests
sbt itPostgres/test

# Run SQL Server integration tests
sbt itSqlserver/test

# Run a specific test suite
sbt "itPostgres/testOnly org.mbari.annosaurus.endpoints.PostgresAnnotationEndpointsSuite"
```

## Development

For developers working on annosaurus, see [CLAUDE.md](CLAUDE.md) for detailed architecture documentation, development workflows, and coding patterns.

### Technology Stack

- **Scala 3.7.4** - Primary programming language
- **Tapir** - Type-safe REST API framework
- **Vert.x** - Async HTTP server
- **Hibernate/JPA** - ORM for database persistence
- **Circe** - JSON serialization
- **Flyway** - Database migration management
- **HikariCP** - Connection pooling
- **MUnit** - Testing framework
- **Testcontainers** - Integration testing with real databases

## Deployment

Refer to [DEPLOYMENT.md](annosaurus/src/site/docs/DEPLOYMENT.md) for production deployment instructions, including:
- Kubernetes deployment
- Database setup and migrations
- Performance tuning
- Monitoring and logging

## Related Projects

This service is part of MBARI's [Video Annotation and Reference System](https://github.com/mbari-org/m3-quickstart):

- [vampire-squid](https://github.com/mbari-org/vampire-squid) - Video asset management service
- [m3-quickstart](https://github.com/mbari-org/m3-quickstart) - Complete system deployment guide

## Contributing

Contributions are welcome! Please:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/your-feature`)
3. Make your changes and add tests
4. Ensure all tests pass (`sbt annosaurus/test itPostgres/test itSqlserver/test`)
5. Commit your changes (`git commit -am 'Add new feature'`)
6. Push to the branch (`git push origin feature/your-feature`)
7. Create a Pull Request

## License

This project is licensed under the Apache License 2.0 - see the LICENSE file for details.

## Support

- **Issues**: Report bugs or request features via [GitHub Issues](https://github.com/mbari-org/annosaurus/issues)
- **Documentation**: See the [Swagger API docs](http://localhost:8080/docs) and [CLAUDE.md](CLAUDE.md)
- **Organization**: [Monterey Bay Aquarium Research Institute (MBARI)](https://www.mbari.org)
