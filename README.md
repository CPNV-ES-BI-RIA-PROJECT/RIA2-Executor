# RIA2 SQL Bridge

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

The repository includes `.env.example`, but the variables below are the authoritative list for the current implementation.

### Required Application Variables

| Variable | Purpose |
| --- | --- |
| `SERVER_PORT` | HTTP port used by the Spring Boot application |
| `BUCKET_ADAPTER_SQL_BRIDGE_SERVER_PORT` | HTTP port exposed by the Bucket Adapter service |
| `BUCKET_SQL_BRIDGE_URL` | Base URL of the Bucket Adapter service reachable from SQL-Bridge |
| `BUCKET_SQL_BRIDGE_REMOTE` | Remote bucket folder/path queried by SQL-Bridge |
| `PROVIDER_IMPL` | Cloud provider implementation to use, for example `AWS` |
| `AWS_REGION` | AWS region used by the Bucket Adapter |
| `AWS_ACCESS_KEY_ID` | AWS access key |
| `AWS_SECRET_ACCESS_KEY` | AWS secret access key |
| `DB_DRIVER` | JDBC driver prefix, for example `mariadb` |
| `DB_HOST` | Database host |
| `DB_PORT` | Database port |
| `DB_DATABASE` | Database name |
| `DB_USER` | Database user |
| `DB_PASSWORD` | Database password |
| `DB_ROOT_PASSWORD` | Database root password |

### Notes

- When SQL-Bridge and Bucket Adapter run in the same `docker-compose.yml`, `BUCKET_SQL_BRIDGE_URL` must use the Docker service name, not `localhost` and not `host.docker.internal`.
- Example:
    - `http://bucket-adapter-sql-bridge:8084`
- Replace `BUCKET-NAME/path/of/directory` with the actual bucket and remote directory to read.
- Do not commit AWS credentials into the repository.

### Example `.env`

```dotenv
SERVER_PORT=8082
BUCKET_ADAPTER_SQL_BRIDGE_SERVER_PORT=8084

BUCKET_SQL_BRIDGE_URL=http://bucket-adapter-sql-bridge:8084
BUCKET_SQL_BRIDGE_REMOTE=BUCKET-NAME/path/of/directory

PROVIDER_IMPL=AWS

AWS_REGION=eu-west-1
AWS_ACCESS_KEY_ID=
AWS_SECRET_ACCESS_KEY=

DB_DRIVER=mariadb
DB_HOST=name-of-domain
DB_DATABASE=name-of-database
DB_USER=root
DB_PASSWORD=root
DB_ROOT_PASSWORD=root
DB_PORT=3306
```

### SQL
Please create the database :
```sql
CREATE DATABASE <DB_DATABASE>;
```

Please create table events :
```sql
CREATE TABLE events (
    uid VARCHAR(255),
    dtstamp TEXT,
    dtstart TEXT,
    dtend TEXT,
    summary TEXT,
    description TEXT,
    categories TEXT,
    organizer TEXT,
    attendee TEXT,
    location TEXT,
    timezone TEXT
);
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
For the service to work, you need to run a Bucket Adapter, which is used to communicate with the bucket. Here is the repository link: [HERE](https://github.com/orgs/CPNV-ES-BI-RIA-PROJECT/packages/container/package/ria2-sql-bridge-bucket-adapter)

To start the bucket, please follow the steps in the Bucket Adapter README:
1. Configuration
2. Docker build & run

If you need help, the magnificent author of these lines can be found behind the yellow door.