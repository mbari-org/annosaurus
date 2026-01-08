# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Annosaurus is a REST-based microservice for managing video and image annotations. It's part of MBARI's Video Annotation and Reference System and provides a programming-language agnostic API for storing and retrieving annotations.

- **Language**: Scala 3.7.4
- **Build tool**: SBT (Scala Build Tool)
- **JDK**: 21
- **Databases**: PostgreSQL or SQL Server
- **Web framework**: Tapir (type-safe REST API) with Vert.x server
- **ORM**: JPA/Hibernate with HikariCP connection pooling
- **JSON**: Circe
- **Testing**: MUnit with Testcontainers
- **Authentication**: JWT (Bearer tokens)

## Common Commands

### Building
```bash
# Compile the project
sbt compile

# Build distribution (creates staged application)
sbt stage

# The compiled application will be at:
# annosaurus/target/universal/stage/bin/annosaurus

# Clean build artifacts
sbt clean
# or clean everything including generated files
sbt cleanall
```

### Testing
```bash
# Run unit tests (main project only)
sbt annosaurus/test

# Run all PostgreSQL integration tests
sbt itPostgres/test

# Run all SQL Server integration tests
sbt itSqlserver/test

# Run a specific test suite (example)
sbt "itPostgres/testOnly org.mbari.annosaurus.endpoints.PostgresAnnotationEndpointsSuite"

# Run all integration tests (both databases)
sbt it/test itPostgres/test itSqlserver/test
```

### Running Locally
```bash
# Start the application
annosaurus/target/universal/stage/bin/annosaurus

# Configuration is read from:
# - annosaurus/target/universal/stage/conf/application.conf
# - annosaurus/target/universal/stage/conf/logback.xml
# - Environment variables (highest priority)
```

### Docker
```bash
# Build Docker image (requires staged application first)
sbt stage
docker build -t mbari/annosaurus .

# Run with Docker
docker run -d \
    -p 8080:8080 \
    -e DATABASE_DRIVER="org.postgresql.Driver" \
    -e DATABASE_URL="jdbc:postgresql://localhost:5432/annotations" \
    -e DATABASE_USER="dbuser" \
    -e DATABASE_PASSWORD="dbpass" \
    -e BASICJWT_CLIENT_SECRET="secret" \
    -e BASICJWT_SIGNING_SECRET="secret" \
    --name=annosaurus \
    mbari/annosaurus
```

## Architecture Overview

### Multi-Module Project Structure

```
annosaurus/          # Main application module
├── controllers/     # Business logic orchestration
├── endpoints/       # Tapir REST API definitions (15+ endpoint classes)
├── domain/          # DTOs and domain models
├── repository/      # Data access layer
│   ├── jpa/        # Hibernate/JPA DAOs for CRUD
│   └── jdbc/       # Raw JDBC for complex queries
└── etc/            # Utilities (JWT, JSON codecs, database config)

it/                 # Shared integration test infrastructure
it-postgres/        # PostgreSQL-specific integration tests
it-sqlserver/       # SQL Server-specific integration tests
```

### Request Flow (Layered Architecture)

```
HTTP Request (JSON)
    ↓
[Tapir Endpoint] - Route matching, validation, deserialization
    ↓
[Controller] - Business logic, transaction orchestration
    ↓
[DAO/Repository] - Data access
    ├── JPA (for CRUD operations)
    └── JDBC (for complex queries, analytics)
    ↓
[Database] - PostgreSQL or SQL Server
```

### Key Architectural Patterns

1. **Hybrid Persistence**: Combines JPA/Hibernate for standard CRUD with raw JDBC for complex queries and analytics
2. **Type-safe REST APIs**: Tapir framework provides compile-time checked endpoints with automatic Swagger docs
3. **Future-based concurrency**: All database operations return `Future[T]` and execute on thread pools
4. **Transaction management**: Controllers use `BaseController.exec()` to wrap DAO operations in transactions
5. **Database abstraction**: Same codebase supports both PostgreSQL and SQL Server via JPA and dialect-aware query builders

### Core Domain Model

The data model centers around video/image annotation:

- **ImagedMoment**: A reference to a moment in a video (via recordedDate, timecode, or elapsedTime)
  - Contains multiple Observations and ImageReferences
- **Observation**: An annotation at a moment (concept, observer, duration, group, activity)
  - Has multiple Associations
- **Association**: Metadata about an observation (format: `linkName | toConcept | linkValue`)
- **ImageReference**: Images/framegrabs linked to a moment
- **CachedAncillaryData**: Time-indexed metadata (position, CTD, sensors)
- **CachedVideoReferenceInfo**: Video deployment metadata

### Database Layer Details

**JPA/Hibernate** is used for:
- Standard CRUD operations on all entities
- Transaction management
- Entity relationships and lazy loading
- Audit logging via Hibernate Envers

**JDBC/JPQL** is used for:
- Complex filtering via `QueryConstraints` (multi-field, multi-table queries)
- Analytical queries (histograms, aggregations, counts)
- Performance-critical operations
- Database-specific optimizations

**Key classes**:
- `BaseDAO[T]` - Abstract base for all DAOs with transaction support
- `JdbcRepository` - Raw JDBC operations
- `AnalysisRepository` - Analytical/aggregation queries
- `QueryConstraintsSqlBuilder` - Converts API query constraints to SQL WHERE clauses

### Configuration Management

Configuration is loaded from (in priority order):
1. Environment variables (highest priority)
2. `application.conf` - Runtime configuration
3. `reference.conf` - Default values (in resources)

Key configuration sections in `application.conf`:
- `http.*` - Server settings (port, timeouts, context path)
- `database.*` - Database connection (driver, URL, credentials)
- `basicjwt.*` - JWT secrets for authentication
- `messaging.zeromq.*` - Optional ZeroMQ messaging

Environment variables can override any setting using UPPERCASE_WITH_UNDERSCORES format.

### Endpoints Organization

All REST endpoints are defined in `endpoints/` package:
- **AnnotationEndpoints** - CRUD for observations (annotations)
- **ImagedMomentEndpoints** - CRUD for video/image moments
- **QueryEndpoints** - Complex multi-constraint queries
- **AnalysisEndpoints** - Analytical queries (histograms, counts by concept)
- **FastAnnotationEndpoints** - Optimized annotation retrieval
- **AssociationEndpoints** - Association metadata operations
- **ImageReferenceEndpoints** - Image linking
- **CachedAncillaryDatumEndpoints** - Cached metadata CRUD
- **CachedVideoReferenceInfoEndpoints** - Video reference metadata
- **HealthEndpoints** - Health check
- **AuthorizationEndpoints** - JWT generation/verification

Endpoints are categorized as:
- **Non-blocking** - Fast GET operations (metadata queries)
- **Blocking** - POST/PUT/DELETE operations (run on worker thread pool)

### Database Migrations

Database schema is managed by **Flyway** migrations in `src/main/resources/db/migrations/`:
- `postgres/` - PostgreSQL migration scripts
- `sqlserver/` - SQL Server migration scripts

Migrations are automatically applied on application startup. Naming convention: `V{version}__{description}.sql`

## Testing Structure

### Test Organization

The project has 138+ test suites organized into modules:

1. **annosaurus/src/test/** - Unit tests (limited)
2. **it/** - Shared integration test infrastructure
   - `TestDAOFactory` - Creates test DAOs
   - `TestEntityFactory` - Generates test entities
   - Base test suites for DAOs and repositories
3. **it-postgres/** - PostgreSQL-specific endpoint tests
4. **it-sqlserver/** - SQL Server-specific endpoint tests

### Testing Pattern

Integration tests use:
- **MUnit** testing framework
- **Testcontainers** for database isolation (spins up real PostgreSQL/SQL Server containers)
- **Sequential execution** (parallelization disabled due to shared database state)
- **UTC timezone** enforced for all tests

Most tests follow this pattern:
```scala
class MyEndpointsSuite extends BaseEndpointsSuite {
  test("operation should work") {
    val result = Await.result(controller.operation(...), Duration.Inf)
    assert(result.isSuccess)
  }
}
```

### Running Specific Tests

To run a specific test suite:
```bash
# PostgreSQL tests
sbt "itPostgres/testOnly org.mbari.annosaurus.endpoints.PostgresQueryEndpointsSuite"

# SQL Server tests
sbt "itSqlserver/testOnly org.mbari.annosaurus.endpoints.SqlServerAnnotationEndpointsSuite"

# Test a specific DAO
sbt "it/testOnly org.mbari.annosaurus.repository.jpa.ObservationDAOSuite"
```

## Development Workflow

### Adding New Endpoints

1. Define endpoint in `endpoints/` package using Tapir DSL
2. Add endpoint to `Endpoints.scala` (either `all` or `allBlocking` list)
3. Implement controller logic in `controllers/`
4. Add DAO methods if needed in `repository/jpa/`
5. Create DTOs in `domain/` and add Circe codecs in `etc/circe/`
6. Add tests in `it-postgres/` and `it-sqlserver/`

### Working with the Database

- **Adding fields**: Modify JPA entities in `repository/jpa/entity/`, then create Flyway migration
- **Complex queries**: Add to `JdbcRepository` or `AnalysisRepository` with database-specific SQL
- **Query constraints**: Extend `QueryConstraintsSqlBuilder` for new filter types

### Dependency Management

Dependencies are defined in `project/Dependencies.scala` and referenced in `build.sbt`. To add a dependency:
1. Add the library to `Dependencies.scala`
2. Reference it in the appropriate project's `libraryDependencies` in `build.sbt`

### Entry Point

Application starts in `Main.scala`:
1. Initializes configuration from `AppConfig`
2. Sets up ZeroMQ publisher (optional)
3. Creates Vert.x server with worker thread pool
4. Registers all endpoints (blocking and non-blocking)
5. Starts HTTP server on configured port

## API Documentation

Swagger documentation is auto-generated from Tapir endpoint definitions and available at:
- `http://localhost:8080/docs` (when running locally)

The API supports JWT Bearer token authentication. Most endpoints require authentication via the `Authorization: Bearer <token>` header.

## Important Notes

- **Timezone**: All timestamps use UTC. Tests enforce UTC via JVM system property.
- **Connection pooling**: HikariCP is configured in `AppConfig`. Adjust pool size via `database.hikari.*` settings.
- **Security**: Never commit `BASICJWT_CLIENT_SECRET` or `BASICJWT_SIGNING_SECRET` to source control. Use environment variables.
- **Database support**: Code must work with both PostgreSQL and SQL Server. Test against both databases.
- **Transaction boundaries**: All DAO operations should be wrapped in transactions via `BaseDAO.runTransaction()`.
- **Async patterns**: Controllers return `Future[T]`. Use `map`/`flatMap` for composition, not `Await.result` (except in tests).
