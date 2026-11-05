## ADDED Requirements

### Requirement: The web address can be verified as the Android app's
When `PUBLIC_ANDROID_CERT_SHA256` is set, the web container SHALL serve `/.well-known/assetlinks.json` as `application/json`, without a redirect, naming the Android app's package and that certificate fingerprint, written each time the container starts, so Android can verify that the web address belongs to the app. Without the setting no such file SHALL be served and nothing else SHALL change.

#### Scenario: Setting given
- **WHEN** `PUBLIC_ANDROID_CERT_SHA256` holds the release certificate's fingerprint and the stack starts
- **THEN** `/.well-known/assetlinks.json` is served as JSON with that fingerprint and the app's package name

#### Scenario: Another fingerprint
- **WHEN** the setting is changed and the stack is started again without rebuilding
- **THEN** the served file holds the new fingerprint

#### Scenario: Setting absent
- **WHEN** the setting is not in `.env`
- **THEN** the stack starts as before and `/.well-known/assetlinks.json` is not found

### Requirement: The fingerprint setting is checked and documented
The value of `PUBLIC_ANDROID_CERT_SHA256` SHALL be a SHA-256 fingerprint of 32 hexadecimal pairs separated by colons, and anything else SHALL stop the web container with an error naming the variable. Compose, the published-images compose file and `.env.example` SHALL list the variable, and the README SHALL say how to read the release certificate's fingerprint and how to check the verification on a device.

#### Scenario: Not a fingerprint
- **WHEN** the setting holds something that is not a colon-separated SHA-256 fingerprint
- **THEN** the web container stops and the error names `PUBLIC_ANDROID_CERT_SHA256`

#### Scenario: Listed for the owner
- **WHEN** a person copies `.env.example` to `.env`
- **THEN** the variable is there with a placeholder, and the README says where its value comes from
