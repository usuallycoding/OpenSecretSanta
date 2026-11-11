## ADDED Requirements

### Requirement: The downloads folder is mounted read-only
Both compose files SHALL mount the folder `./downloads` next to the compose file (or the folder `DOWNLOADS_HOST_DIR` names in `.env`, as a full path) into the server container at `/downloads`, for reading only, and SHALL give the server the setting `DOWNLOADS_DIR=/downloads`, so that an APK copied there is served at `/download_latest` without rebuilding or restarting anything. The folder SHALL NOT be part of any image and SHALL NOT be committed.

#### Scenario: Copy an APK
- **WHEN** an APK is copied into `./downloads` on the machine while the stack runs
- **THEN** `GET /download_latest` serves it with no restart

#### Scenario: Read-only
- **WHEN** the server container runs
- **THEN** it can read the folder and cannot change it

#### Scenario: Another folder
- **WHEN** `DOWNLOADS_HOST_DIR` in `.env` names a folder that holds an APK
- **THEN** the server serves that APK, and without it the folder `./downloads` next to the compose file is used

#### Scenario: Not committed
- **WHEN** an APK is in `./downloads`
- **THEN** `git status` does not list it

### Requirement: The README describes publishing an APK to the server
The README SHALL describe how to put an APK where the server serves it: build it with `scripts/distribute-android.sh --no-upload`, copy it to the machine's downloads folder under a temporary name ending in `.part`, rename it once the copy is complete, check `/download_latest`, and that the stack's public address must allow a download of that size.

#### Scenario: Following the README
- **WHEN** someone follows the section after running the script with `--no-upload`
- **THEN** `/download_latest` serves their APK, and they know to copy under a `.part` name first
