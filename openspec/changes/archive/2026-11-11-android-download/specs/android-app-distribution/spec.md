## ADDED Requirements

### Requirement: A built APK can be published to the server
The APK the script leaves in `build/distribution/` under its name `opensecretsanta-<version>-<code>.apk` SHALL be usable as it is, with no renaming, as the file the server serves at `/download_latest`. The script SHALL NOT upload to the server itself.

#### Scenario: Name as built
- **WHEN** the script finishes with `--no-upload` and the APK is copied into the server's downloads folder
- **THEN** the server serves it as the newest APK, as its version code is higher than every earlier build's

#### Scenario: No upload by the script
- **WHEN** the script runs with or without `--no-upload`
- **THEN** it sends nothing to the server's downloads folder
