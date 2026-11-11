## MODIFIED Requirements

### Requirement: Health endpoint
The server SHALL expose `GET /health`, requiring no authentication, that returns HTTP 200 with a JSON body containing a `status` of `"ok"`, the current database `schemaVersion`, the API's `apiVersion` and `androidDownload`, which is true when an APK is available at `/download_latest` and false otherwise. A downloads folder that is missing or cannot be read SHALL only make `androidDownload` false and SHALL NOT change the status of the answer.

#### Scenario: Healthy server
- **WHEN** a client requests `GET /health` on a started server
- **THEN** the response is 200 with `status` equal to `"ok"` and `schemaVersion` equal to the highest applied migration version

#### Scenario: The API's version
- **WHEN** a client requests `GET /health` on a started server
- **THEN** the response carries `apiVersion` equal to the version the server's API has

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
- **THEN** the response is 503 with `status` equal to `"error"` and no `apiVersion`

## ADDED Requirements

### Requirement: The API has a version
The API SHALL have one version number, a whole number starting at 1 that the server and the apps are built with, and the server SHALL report it as `apiVersion` in `GET /health`. It SHALL be raised only for a change that an app built with the earlier number would misbehave against, and SHALL NOT be raised for a change such an app still copes with, such as a new optional field or a new route.

#### Scenario: A breaking change
- **WHEN** the API changes so that an app built before it would fail or show wrong data
- **THEN** the version is raised by one, and the server reports the new number

#### Scenario: A compatible change
- **WHEN** a field is added that an older app ignores
- **THEN** the version stays as it is

#### Scenario: Same build, same number
- **WHEN** the server and an app are built from the same revision
- **THEN** the number the server reports is the number the app was built with
