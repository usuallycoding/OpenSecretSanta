# app-download Specification

## Purpose

Lets anyone download the latest Android app from a stable address on the server, from an APK that the maintainer copies into a folder on the machine the server runs on.

## Requirements

### Requirement: The latest APK can be downloaded
The server SHALL expose `GET /download_latest`, requiring no authentication, that answers 200 with the newest APK in its downloads folder as a file download, with the media type `application/vnd.android.package-archive` and a `Content-Disposition` that names the file as an attachment under its own name.

#### Scenario: Download
- **WHEN** the downloads folder holds `opensecretsanta-1.1.5-28900000.apk` and a client requests `GET /download_latest` with no credentials
- **THEN** the response is 200 with the whole file, as an attachment named `opensecretsanta-1.1.5-28900000.apk`

#### Scenario: Not for a browser page
- **WHEN** a browser follows a link to `/download_latest`
- **THEN** it downloads the file and does not show it as a page

### Requirement: The newest APK is the highest version code
The newest APK SHALL be the one whose name has the form `opensecretsanta-<version>-<code>.apk` with the highest numeric `<code>`. A name of any other form that ends in `.apk` SHALL count only after every name of that form, the most recently modified first. Files that do not end in `.apk` SHALL be ignored, and so SHALL directories and files that cannot be read.

#### Scenario: Highest code wins
- **WHEN** the folder holds `opensecretsanta-1.1.4-100.apk` and `opensecretsanta-1.1.5-9000.apk`
- **THEN** the download is `opensecretsanta-1.1.5-9000.apk`

#### Scenario: Codes are compared as numbers
- **WHEN** the folder holds `opensecretsanta-1.1.4-900.apk` and `opensecretsanta-1.1.5-10000.apk`
- **THEN** the download is `opensecretsanta-1.1.5-10000.apk`

#### Scenario: Other names come last
- **WHEN** the folder holds `opensecretsanta-1.1.4-100.apk` and a more recently modified `santa.apk`
- **THEN** the download is `opensecretsanta-1.1.4-100.apk`

#### Scenario: Only other names
- **WHEN** the folder holds `old.apk` and a more recently modified `new.apk`
- **THEN** the download is `new.apk`

#### Scenario: Not an APK
- **WHEN** the folder holds `opensecretsanta-1.1.5-9000.apk.part`, `notes.txt` and a subfolder
- **THEN** none of them is offered

### Requirement: No APK, no download
When the downloads folder is empty, holds no APK, is missing or cannot be read, `GET /download_latest` SHALL answer 404 with a short message and nothing SHALL be sent as a file.

#### Scenario: Empty folder
- **WHEN** the folder holds no `.apk` file
- **THEN** the response is 404 with a short message

#### Scenario: Missing folder
- **WHEN** the downloads folder does not exist
- **THEN** the response is 404 with a short message and the server keeps running

### Requirement: A file being copied is not served
The server SHALL serve an APK only once it is complete. A file that is still being copied under another name, such as `opensecretsanta-1.1.5-9000.apk.part`, SHALL NOT be offered, so that copying under a temporary name and then renaming never lets anyone fetch half a file.

#### Scenario: Uploading
- **WHEN** a new APK is being copied as `opensecretsanta-1.1.6-9100.apk.part` next to a finished `opensecretsanta-1.1.5-9000.apk`
- **THEN** the download is still `opensecretsanta-1.1.5-9000.apk`

#### Scenario: Finished
- **WHEN** the copy is renamed to `opensecretsanta-1.1.6-9100.apk`
- **THEN** the next download is `opensecretsanta-1.1.6-9100.apk`

### Requirement: Nothing else in the folder is reachable
The server SHALL send only the file it chose for `GET /download_latest`; the folder SHALL NOT be browsable and no request SHALL be able to name a file in it or reach outside it.

#### Scenario: No listing
- **WHEN** a client requests `GET /download_latest/` or a path under it
- **THEN** no file or listing of the folder is returned

#### Scenario: Only the latest
- **WHEN** the folder holds three APKs
- **THEN** only the newest can be fetched, from `/download_latest`
