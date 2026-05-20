# Deployment

## Using Docker (Recommended)

Pre-built multi-platform images (amd64/arm64) are published to Docker Hub as `mbari/annosaurus`.

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

Database schema migrations are applied automatically by Flyway on startup. The database user must have permission to create and alter tables on the first run.

## Building from Source

```bash
sbt stage
```

The staged application is written to `annosaurus/target/universal/stage/`. Run it with:

```bash
annosaurus/target/universal/stage/bin/annosaurus
```

## Building and Publishing a Docker Image

```bash
sbt stage
docker build -t mbari/annosaurus .
docker login
docker push mbari/annosaurus
```

## Running as a systemd Service

If your deployment machine uses systemd, you can manage the container as a service.

1. Copy `docker.annosaurus.service` to `/etc/systemd/system/`.
2. Test the service: `sudo systemctl start docker.annosaurus`
3. Check the status: `sudo systemctl status docker.annosaurus`
4. Enable it to start on boot: `sudo systemctl enable docker.annosaurus`

To restart after pulling a new image:

```bash
docker pull mbari/annosaurus
sudo systemctl restart docker.annosaurus
```

## Configuration

All settings can be overridden with environment variables. Key options:

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

For the full list of options, see `annosaurus/src/universal/conf/application.conf`.
