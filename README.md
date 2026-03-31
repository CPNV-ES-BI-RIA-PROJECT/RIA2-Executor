# RIA2 Executor

`RIA2 Executor` is a Spring Boot service that fetches recent SQL files from a remote Bucket SQL Bridge, executes them against a MariaDB database, and reports which files were executed successfully.

The application runs under the `/api` context path, so the current HTTP API is exposed under `/api/v1`.

## What It Does

When you call the execution endpoint, the service:

1. Lists remote files from the configured Bucket SQL Bridge.
2. Keeps only files ending in `.sql`.
3. Keeps only files whose name matches the timestamp format `yyyyMMdd_HHmmss.sql`.
4. Compares those timestamps with the last executed timestamp kept by the application.
5. Downloads the matching files in chronological order.
6. Executes each SQL script against the configured MariaDB datasource.
7. Returns a summary with the number of files found, executed, and skipped due to failure.

## Current API

### Execute Recent SQL Files

`POST /api/v1/objects/recent-files/executions`

Response:

```json
{
  "filesGet": 3,
  "filesExecuted": 2,
  "filesNotExecuted": [
    "20260331_081500.sql"
  ]
}
```

Notes:

- There is currently one public endpoint.
- The endpoint returns `200 OK` with a JSON summary.
- Files are executed in ascending timestamp order.
- Only `.sql` files with a valid timestamp-based file name are considered.
- Invalid names and non-SQL files are ignored.

## Important Behavior

- The execution watermark is stored in memory by `ExecutionHistoryRepository`.
- Restarting the application resets that watermark to `19700101_000000`.
- SQL execution stops for a single file if Spring JDBC raises an error, but the batch loop continues with the next remote file.
- The watermark is updated to the latest successfully executed file in the batch.
- Because the watermark is a single timestamp, an older failed file can be skipped on a later run if a newer file succeeded in the same batch.

## Project Stack

- Java 21
- Spring Boot 4.0.1
- Spring Web MVC
- Spring JDBC
- MariaDB JDBC Driver
- springdoc OpenAPI
- JUnit 5
- Mockito
- Checkstyle
- SpotBugs
- Docker / Docker Compose

## Configuration

The application loads `.env` before Spring starts.

The repository includes `.env.exemple`, but the variables below are the authoritative list for the current implementation.

### Required Application Variables

| Variable | Purpose |
| --- | --- |
| `SERVER_PORT` | HTTP port used by the Spring Boot application |
| `BUCKET_SQL_BRIDGE_URL` | Base URL of the Bucket SQL Bridge service |
| `BUCKET_SQL_BRIDGE_REMOTE` | Remote folder/path queried on the bridge |
| `DB_DRIVER` | JDBC driver prefix, default `mariadb` |
| `DB_HOST` | MariaDB host |
| `DB_PORT` | MariaDB port |
| `DB_DATABASE` | Database name |
| `DB_USER` | Database user |
| `DB_PASSWORD` | Database password |

### Additional Variables For `docker-compose.yml`

| Variable | Purpose |
| --- | --- |
| `DB_ROOT_PASSWORD` | MariaDB root password |
| `DB_VERSION` | MariaDB image tag |
| `DB_EXPOSED_PORT` | Host port mapped to the MariaDB container |

### Example `.env`

```dotenv
SERVER_PORT=8082

BUCKET_SQL_BRIDGE_URL=http://host.docker.internal:8081
BUCKET_SQL_BRIDGE_REMOTE=my-bucket/sql/

DB_DRIVER=mariadb
DB_HOST=localhost
DB_PORT=3306
DB_DATABASE=executor
DB_USER=executor
DB_PASSWORD=executor_pwd

DB_ROOT_PASSWORD=root
DB_VERSION=11.7
DB_EXPOSED_PORT=3306
```

### With Docker Compose

The compose file starts:

- `app`

Run:

```bash
docker compose up --build
```

The app will be available on `http://localhost:${SERVER_PORT}/api`.

## Example Request

```bash
curl -X POST "http://localhost:8082/api/v1/objects/recent-files/executions"
```

If your `.env` uses another `SERVER_PORT`, replace `8082` accordingly.

## OpenAPI

When the application is running, Springdoc exposes:

- Swagger UI: `http://localhost:${SERVER_PORT}/api/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:${SERVER_PORT}/api/v3/api-docs`

## Development

Run the test suite:

```bash
./mvnw test
```

Run the same verification phase used in CI:

```bash
./mvnw clean verify
```

## Bucket Adapter
For the service to work, you need to run a Bucket Adapter, which is used to communicate with the bucket. Here is the repository link: [HERE](https://github.com/CPNV-ES-BI-RIA-PROJECT/RIA2-bucket-adapter.git)

To start the bucket, please follow the steps in the Bucket Adapter README:
1. Configuration
2. Docker build & run

If you need help, the magnificent author of these lines can be found behind the yellow door.