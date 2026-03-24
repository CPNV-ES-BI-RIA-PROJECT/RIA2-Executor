# RIA2 Executor

## For OPS !
Do not implement the RIA2-SQL-Bridge in the data warehouse yet. You can retrieve the SQL script by performing a
curl request on the bucket adapter:
```bash
curl http://<DOMAIN-BUCKET-ADAPTER>:8081/api/v1/objects/download?remote=<YOUR-BUCKET-NAME/FOLDER/FILE.SQL>
```

`<DOMAIN-BUCKET-ADAPTER>` => Name of the domain where the bucket is hosted.
`<YOUR-BUCKET-NAME/FOLDER/FILE.SQL>` => Path of the file to download.

For now, manually execute the INSERT returned by the download.

> I work as fast as possible, but as slowly as necessary ;)

## Overview

This project is a Spring Boot service that combines two responsibilities:

1. Object storage access through a provider adapter (`AWS` or `GCP` today).
2. SQL script execution against MariaDB after the script has been downloaded locally.

The current codebase is not just a bucket adapter anymore. The main workflow implemented in the code is:

1. Retrieve a `.sql` file from a cloud bucket.
2. Save the downloaded file under `data/script/`.
3. Execute that local SQL file against the configured MariaDB datasource.

Base path: `/api`  
API version prefix: `/v1`

Swagger UI: `http://localhost:<port>/api/swagger-ui/index.html`


## Tech Stack

- Java 21
- Spring Boot 4
- Spring Web MVC
- Spring JDBC
- MariaDB JDBC driver
- AWS SDK v2 for S3
- Google Cloud Storage SDK
- Springdoc OpenAPI
- JUnit 5
- Mockito
- Docker / Docker Compose

## Configuration

Copy the example file first:

```bash
cp .env.exemple .env
```

### Important current behavior

- `PROVIDER_IMPL` selects the adapter used by `BucketAdapterFactory`.
- Remote bucket paths are expected in the form `bucket/key.sql` or `gs://bucket/key.sql`.
- Downloaded scripts are saved under `data/script/`.
- The saved file name is sanitized, and `.sql` is appended automatically if missing.
- `AwsClientConfig` and `GcpStorageConfig` currently read JVM system properties, so the current implementation expects `.env` loading semantics instead of plain environment-variable lookup.
- `AZURE` exists as a bean name, but the implementation is empty.

## Database Initialization

If this is the first time the database is run, create the `events` table before executing the sample insert script:

```sql
CREATE TABLE events (
    uid VARCHAR(255) NOT NULL PRIMARY KEY,
    dtstamp DATETIME,
    dtstart DATETIME,
    dtend DATETIME,
    summary TEXT,
    description TEXT,
    categories TEXT,
    organizer VARCHAR(255),
    attendee TEXT,
    location TEXT,
    timezone VARCHAR(64)
);
```

The sample file already present in the repository is:

- `data/script/realdata.sql`

It inserts one row into `events`.

## Running the Project

### Recommended local workflow

The most reliable workflow with the current code is:

1. Start MariaDB with Docker Compose.
2. Run the Spring Boot app locally with Maven so `.env` is loaded by `DotenvInitializer`.

Start only MariaDB:

```bash
docker compose up --build -d app
```

## API Endpoints

### Object storage endpoints

Base: `/api/v1/objects`

- `GET /api/v1/objects/download?remote=<bucket/key.sql>`
  Downloads the remote object and saves it locally. The response body is:

```json
{
  "remote": "bucket/path/script.sql",
  "filename": "script.sql",
  "localPath": "data/script/script.sql"
}
```

### SQL execution endpoint

- `POST /api/v1/execute-script?localPath=<path-to-local-sql-file>`

Example:

```bash
curl -X POST "http://localhost:8090/api/v1/execute-script?localPath=data/script/realdata.sql"
```

Current success response:

```text
HTTP 200 with a plain-text success message
```

## Example End-to-End Usage

### 1. Download it locally through the API

```bash
curl "http://localhost:8082/api/v1/objects/download?remote=my-bucket/folder/file.sql"
```

### 2. Execute the saved script

```bash
curl -X POST "http://localhost:8082/api/v1/execute-script?localPath=local/path/file.sql"
```

You can also execute any existing local SQL file directly, as long as the path is accessible by the running application.

## Notes About the Current Codebase

- The Maven artifact and some OpenAPI labels still use the old `bucket-adapter` naming.
- The application name in `application.properties` is still `bucket-adapter`.
- The README here reflects the actual code behavior, not the older project wording.
- Tests currently cover the AWS and GCP adapter implementations only.

## Useful Files

- `src/main/java/com/executor/controllers/BucketController.java`
- `src/main/java/com/executor/controllers/SqlScriptExecutorController.java`
- `src/main/java/com/executor/services/SqlExecutorService.java`
- `src/main/java/com/executor/services/LocalScriptStorageService.java`
- `src/main/resources/application.properties`
- `docker-compose.yml`
- `data/script/realdata.sql`

## DEV ZONE
### Build and Test

Build:

```bash
./mvnw clean install
```

Run tests:

```bash
./mvnw test
```
