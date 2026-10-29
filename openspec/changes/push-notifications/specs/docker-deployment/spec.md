# Spec Delta

## ADDED Requirements

### Requirement: Push settings are optional and stay out of git
The compose setup SHALL pass the optional Firebase settings from `.env`: the path of the service-account key file to the server, which SHALL receive the file read-only, and the public Firebase web settings to the web container only. The key file SHALL live in a folder git ignores, SHALL NOT be copied into any image and SHALL NOT be served by the web container. `.env.example` SHALL list the variables with placeholders. Without them compose SHALL start as before with push off.

#### Scenario: Push off
- **WHEN** the Firebase variables are not set in `.env`
- **THEN** compose starts, the server runs with push off and the web app works

#### Scenario: Key reaches only the server
- **WHEN** the key file is configured
- **THEN** the server container can read it and the web container and the web app's files do not contain it

#### Scenario: Key not committed
- **WHEN** the key file sits in the ignored folder
- **THEN** git ignores it and `.env.example` is tracked

#### Scenario: Another address
- **WHEN** the web address in `.env` is changed and the stack is started again without rebuilding
- **THEN** the web app's served `config.json` carries the public Firebase settings and the new server address
