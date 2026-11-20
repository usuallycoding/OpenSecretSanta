# Spec Delta

## ADDED Requirements

### Requirement: Health responses are not cached
`GET /health` SHALL include `Cache-Control: no-store` so browsers and intermediate proxies never serve a stale `apiVersion` or health status.

#### Scenario: Healthy response carries no-store
- **WHEN** a client requests `GET /health` on a started server
- **THEN** the response carries `Cache-Control: no-store`
