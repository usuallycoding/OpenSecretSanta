# Spec Delta

## Purpose

Defines how the Open Secret Santa client apps (Android, iOS and web) start up so that shared dependencies are available identically on every platform before any screen is shown.

## ADDED Requirements

### Requirement: Dependency graph initialized before UI
Each client platform SHALL initialize the shared dependency graph exactly once at launch, before the first screen is composed.

#### Scenario: Cold start
- **WHEN** the Android, iOS or web app launches
- **THEN** the dependency graph is ready before the root UI requests any dependency, and no resolution error occurs

#### Scenario: Repeated host re-creation
- **WHEN** the host UI is recreated (for example an Android configuration change)
- **THEN** the dependency graph is not initialized a second time and does not fail

### Requirement: Platform-independent dependency resolution
The shared dependency modules SHALL resolve every declared dependency on all supported client platforms using only shared code plus per-platform bindings explicitly supplied by each host.

#### Scenario: Complete graph
- **WHEN** the module graph is verified in a test
- **THEN** every declared dependency is resolvable with no missing definitions

### Requirement: Configurable server address
The client apps SHALL obtain the backend base URL from a single platform-supplied setting, so that all shared data access targets one configurable address.

#### Scenario: Base URL provided by host
- **WHEN** a platform host supplies a base URL at startup
- **THEN** shared data access components use that URL for all requests to the server
