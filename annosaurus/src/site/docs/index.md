# annosaurus

![MBARI logo](assets/images/logo-mbari-3b.png)

Annosaurus is a REST API for creating and managing video and image annotations. It is a core service of MBARI's Video Annotation and Reference System (VARS), providing a language-agnostic interface so annotations can be created and retrieved from any programming environment.

The service is self-contained: it needs a PostgreSQL or SQL Server database and nothing else. Schema migrations are applied automatically at startup, and an interactive Swagger UI is published at `/docs` once it is running.

Source code and issue tracker: <https://github.com/mbari-org/annosaurus>

## Where to start

| If you are… | Start here |
| --- | --- |
| Deciding whether annosaurus fits your project | [Overview](overview.md) — what it stores, what it deliberately leaves to other services |
| Reading annotations out of an existing deployment | [Fetching Annotations](howto/fetch_annotations.md) |
| Assembling a dataset with filters on depth, time, platform, or concept | [Advanced Queries](howto/advanced-queries.md) |
| Writing annotations, or anything that changes data | [Security Handshake](howto/security_handshake.md), then the Swagger UI at `/docs` |
| Standing up a server | [Deployment](DEPLOYMENT.md), plus [For staff deploying annosaurus](#for-staff-deploying-annosaurus) below |

## Authentication in one paragraph

**Read-only endpoints are open — no token, no API key.** Anyone who can reach the service can fetch and query annotations. **Endpoints that create, update, or delete data require a JWT Bearer token**, which you obtain by exchanging an API key for one. That exchange, and how to use the resulting token, is described in [Security Handshake](howto/security_handshake.md).

In practice:

```text
GET  /v1/fast/videoreference/{uuid}        # no auth needed
POST /v1/query/download                    # no auth needed (a query, despite being a POST)
POST /v1/annotations                       # Authorization: Bearer <jwt>
PUT  /v1/observations/{uuid}               # Authorization: Bearer <jwt>
```

Note that several read-only operations use `POST` because they take a JSON request body — searching by a list of video references, or submitting a query. Those still need no token. The rule follows what the endpoint *does*, not which verb it uses.

## For staff deploying annosaurus

The [Deployment guide](DEPLOYMENT.md) has the full procedure. The points that most often bite:

**Both JWT secrets are required.** `BASICJWT_CLIENT_SECRET` and `BASICJWT_SIGNING_SECRET` have no defaults — the service refuses to start without them, the same way it refuses to start without a database URL. Set them to real secrets and keep them out of source control. Earlier releases fell back to the placeholders `secret` and `supersecret`, so if you are upgrading a deployment that never set them, treat any token issued so far as compromised and rotate.

**The database is the only dependency.** Point `DATABASE_DRIVER`, `DATABASE_URL`, `DATABASE_USER`, and `DATABASE_PASSWORD` at a PostgreSQL or SQL Server instance. Flyway migrations run on startup, so the account needs DDL rights on first launch. Both databases are supported and tested.

**Endpoints worth knowing as an operator:**

| Path | Purpose |
| --- | --- |
| `/v1/health` | Health check — returns version, JDK, and memory figures. Use for readiness and liveness probes. |
| `/metrics` | Prometheus metrics for scraping. |
| `/docs` | Swagger UI. Auto-generated from the running build, so it always matches the deployed version. |

**Restrict the service, not just the tokens.** Writes require a token, but reads do not, so network placement is part of your security posture. Put annosaurus behind a reverse proxy or on a private network unless your annotations are meant to be readable by anyone who can reach the host.

**Optional change notifications.** Annosaurus can publish `CREATED`/`UPDATED`/`DELETED` messages to a [NATS](https://nats.io) topic as observations and associations change, so downstream systems can stay in sync. Off by default; enable with `MESSAGING_NATS_ENABLE`.

**Configuration precedence.** Environment variables override `application.conf`, which overrides the built-in defaults in `reference.conf`. Not every setting has an environment variable — the ones that do are declared with an uppercase, underscore-separated name in `reference.conf`, which is the authoritative list.

## The data model in brief

An **imaged moment** is a point in a video or an image, indexed by recorded timestamp, timecode, and/or elapsed time. It holds **observations** — the actual annotations, each with a concept and an observer — and **image references** for framegrabs. Each observation can carry **associations**, structured detail in `linkName | toConcept | linkValue` form. **Ancillary data** (position, depth, CTD) is cached against the moment so that spatial and environmental queries stay fast.

Annotations are keyed to video by `videoReferenceUuid`, a UUID that comes from the video asset service rather than from annosaurus. See the [Overview](overview.md) for how that boundary is drawn.
