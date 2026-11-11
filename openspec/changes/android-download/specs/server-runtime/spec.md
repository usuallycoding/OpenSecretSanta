## MODIFIED Requirements

### Requirement: Health endpoint
The server SHALL expose `GET /health`, requiring no authentication, that returns HTTP 200 with a JSON body containing a `status` of `"ok"`, the current database `schemaVersion` and `androidDownload`, which is true when an APK is available at `/download_latest` and false otherwise. A downloads folder that is missing or cannot be read SHALL only make `androidDownload` false and SHALL NOT change the status of the answer.

#### Scenario: Healthy server
- **WHEN** a client requests `GET /health` on a started server
- **THEN** the response is 200 with `status` equal to `"ok"` and `schemaVersion` equal to the highest applied migration version

#### Scenario: An APK is available
- **WHEN** the downloads folder holds an APK and a client requests `GET /health`
- **THEN** the response is 200 and `androidDownload` is true

#### Scenario: No APK
- **WHEN** the downloads folder is empty and a client requests `GET /health`
- **THEN** the response is 200 and `androidDownload` is false

#### Scenario: Unreadable folder
- **WHEN** the downloads folder does not exist and a client requests `GET /health`
- **THEN** the response is 200 with `status` equal to `"ok"` and `androidDownload` equal to false

#### Scenario: Database unavailable
- **WHEN** a client requests `GET /health` and the database cannot be queried
- **THEN** the response is 503 with `status` equal to `"error"`

## ADDED Requirements

### Requirement: The downloads folder is configurable
The server SHALL read `DOWNLOADS_DIR`, the folder `/download_latest` takes the APK from, defaulting to `downloads` when it is not set or is blank. The folder SHALL NOT have to exist when the server starts.

#### Scenario: Default
- **WHEN** the server starts without `DOWNLOADS_DIR`
- **THEN** it looks for APKs in `downloads`

#### Scenario: Set
- **WHEN** the server starts with `DOWNLOADS_DIR=/downloads`
- **THEN** it looks for APKs in `/downloads`

#### Scenario: Not there yet
- **WHEN** the server starts and the folder does not exist
- **THEN** it starts normally and `/download_latest` answers 404
