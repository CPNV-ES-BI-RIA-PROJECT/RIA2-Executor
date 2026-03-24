# RIA2 SQL-Bridge

## Overview

This project is a Spring Boot service that executes SQL scripts against MariaDB after downloading them locally.


1. Retrieve a `.sql` file from a cloud bucket.
2. Save the downloaded file under `data/script/`.
3. Execute that local SQL file against the configured MariaDB datasource.

Base path: `/api`  
API version prefix: `/v1`

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

- `data/script/TIMESTAMP_OF_DOWLOAD.sql`

It inserts one row into `events`.

## Running the Project

### Recommended local workflow

Start only App:

```bash
docker compose up --build -d app
```
## API Endpoints

### Object storage endpoints

Base: `/api/v1/objects`

- `POST /api/v1/objects/import`
  Downloads a file from a shared HTTP(S) URL and stores it locally. The request body is:

```json
{
  "url": "https://example.com/FILE_SHARED_BY_ORCHESTRATOR"
}
```

  The response body is:

```json
{
  "path": "data/script/TIMESTAMP_OF_DOWNLOAD.sql"
}
```

### SQL execution endpoint

- `POST /api/v1/execute-script?localPath=<path-to-local-sql-file>`

Example:

```bash
curl -X POST "http://localhost:8082/api/v1/execute-script?localPath=data/script/realdata.sql"
```

Current success response:

```text
HTTP 200 with a plain-text success message
```

### Batch SQL execution endpoint

- `POST /api/v1/execute-scripts`

Executes every `.sql` file currently present in `data/script`, in sorted order. Each file is
deleted immediately after its successful execution. If one script fails, the failing file and any
remaining files stay on disk.

## Example End-to-End Usage

### 1. Import a SQL file from a shared URL

```bash
curl -X POST "http://localhost:8082/api/v1/objects/import" \
  -H "Content-Type: application/json" \
  -d '{"url":"https://example.com/file.sql"}'
```

### 2. Execute the saved script

```bash
curl -X POST "http://localhost:8082/api/v1/execute-script?localPath=local/path/file.sql"
```

You can also execute any existing local SQL file directly, as long as the path is accessible by the running application.

### 3. Execute every queued script

```bash
curl -X POST "http://localhost:8082/api/v1/execute-scripts"
```

The examples above assume `SERVER_PORT=8082`. If your `.env` uses another port, replace `8082` accordingly.
