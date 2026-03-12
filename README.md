# RIA2 Executor

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

The code currently relies on `.env` values being loaded into JVM system properties by `DotenvInitializer`. For local development, keep `.env` in the project root.

### Required variables actually used by the code

```bash
SERVER_PORT=8090
PROVIDER_IMPL=AWS

AWS_REGION=your-region
AWS_ACCESS_KEY_ID=your-access-key-id
AWS_SECRET_ACCESS_KEY=your-secret-access-key

GOOGLE_CLOUD_PROJECT=your-project-id
GOOGLE_APPLICATION_CREDENTIALS=/absolute/or/project-relative/path-to-credentials.json

MARIADB_VERSION=11.4
MARIADB_PORT=3306
MARIADB_ROOT_PASSWORD=root
MARIADB_DATABASE=executor
MARIADB_USER=executor
MARIADB_PASSWORD=executor_pwd

spring.datasource.url=jdbc:mariadb://localhost:3306/executor
spring.datasource.username=executor
spring.datasource.password=executor_pwd
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
docker compose up -d mariadb
```

Run the application:

```bash
./mvnw spring-boot:run
```

Open Swagger:

```text
http://localhost:8090/api/swagger-ui/index.html
```

### Full Docker Compose

The repository also contains an `app` service in `docker-compose.yml`:

```bash
docker compose up --build
```

However, the current Java configuration loads provider credentials from JVM system properties populated by `.env`. If you run the app inside Docker, make sure those properties are still available to the JVM inside the container; otherwise AWS/GCP client initialization can fail.

## Build and Test

Build:

```bash
./mvnw clean install
```

Run tests:

```bash
./mvnw test
```

## API Endpoints

### Object storage endpoints

Base: `/api/v1/objects`

- `GET /api/v1/objects?remote=<bucket-or-prefix>&recursive=<true|false>`
  Lists bucket contents.
- `POST /api/v1/objects?remote=<bucket/key.sql>`
  Uploads a file as `multipart/form-data` using the `file` part.
- `DELETE /api/v1/objects?remote=<bucket/key-or-prefix>&recursive=<true|false>`
  Deletes a file or a prefix.
- `GET /api/v1/objects/share?remote=<bucket/key>&expirationTime=<seconds>`
  Returns a temporary URL.
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

### 1. Upload a SQL file to the selected bucket

```bash
curl -X POST "http://localhost:8090/api/v1/objects?remote=my-bucket/sql/realdata.sql" \
  -F "file=@data/script/realdata.sql"
```

### 2. Download it locally through the API

```bash
curl "http://localhost:8090/api/v1/objects/download?remote=my-bucket/sql/realdata.sql"
```

### 3. Execute the saved script

```bash
curl -X POST "http://localhost:8090/api/v1/execute-script?localPath=data/script/realdata.sql"
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
