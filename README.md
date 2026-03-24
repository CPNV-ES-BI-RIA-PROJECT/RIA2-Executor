# RIA2 Executor

## Overview

This project is a Spring Boot service that:

1. downloads a SQL file from an HTTP(S) URL,
2. stores it under `data/script/`,
3. executes queued `.sql` files against a MariaDB database,
4. deletes each script immediately after a successful execution.

The application runs under the `/api` context path, so the current HTTP API lives under `/api/v1`.

## Current API

### Import a SQL file

`POST /api/v1/objects/import`

Request body:

```json
{
  "url": "https://example.com/file.sql"
}
```

Success response:

```json
{
  "path": "data/script/20260324_153045.sql"
}
```

Notes:

- Only `http` and `https` URLs are accepted.
- Local/private addresses are rejected.
- Downloads are limited to 50 MB.
- The endpoint returns `201 Created` on success.

### Execute all queued SQL scripts

`POST /api/v1/execute-scripts`

Success response:

```text
Scripts SQL exécutés avec succès
```

Execution behavior:

- Only `.sql` files from `data/script/` are executed.
- Files are processed in sorted order.
- Each file is deleted right after a successful execution.
- If one script fails, the failing script and the remaining scripts stay on disk.
- Symbolic links are rejected.

## Stack

- Java 21
- Spring Boot 4.0.1
- Spring Web MVC
- Spring JDBC
- MariaDB JDBC driver
- springdoc OpenAPI
- JUnit 5
- Mockito
- Docker / Docker Compose

## Configuration

The application loads variables from `.env` before Spring starts.

Start from the example file:

```bash
cp .env.exemple .env
```

Core application variables:

| Variable | Purpose |
| --- | --- |
| `SERVER_PORT` | HTTP port used by the Spring application |
| `DB_DRIVER` | JDBC driver prefix, default is `mariadb` |
| `DB_HOST` | MariaDB host |
| `DB_PORT` | MariaDB port |
| `DB_DATABASE` | Database name |
| `DB_USER` | Database user |
| `DB_PASSWORD` | Database password |

Additional variables used by `docker-compose.yml`:

| Variable | Purpose |
| --- | --- |
| `DB_ROOT_PASSWORD` | MariaDB root password |
| `DB_VERSION` | MariaDB image tag |
| `DB_EXPOSED_PORT` | Host port mapped to the MariaDB container |

The JDBC URL is built from these values in `application.properties`.

## Running Locally

### Docker Compose

Make sure `.env` also defines `DB_VERSION`, `DB_EXPOSED_PORT`, and a database host reachable by the app container. For the bundled `mariadb` service, that host should be `mariadb`.

```bash
docker compose up --build app
```

Useful services:

- `app`: Spring Boot application

The compose setup mounts:

- `./data` to persist downloaded SQL files

## Example Usage

Import a SQL file:

```bash
curl -X POST "http://localhost:8082/api/v1/objects/import" \
  -H "Content-Type: application/json" \
  -d '{"url":"https://URL_GIVEN_BY_ORCHESTRATOR"}'
```

Execute every queued script:

```bash
curl -X POST "http://localhost:8082/api/v1/execute-scripts"
```

If your `.env` uses a different `SERVER_PORT`, replace `8082` in the examples.

## Tests

Run the test suite with:

```bash
./mvnw test
```

## Notes

- Downloaded scripts are timestamp-based, for example `20260324_153045.sql`.
- The local script directory defaults to `data/script`.
- There is currently no HTTP endpoint for executing a single specific SQL file.
