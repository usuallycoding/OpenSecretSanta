# Spec Delta

## ADDED Requirements

### Requirement: Web app bundles revalidate on reload
The web container SHALL serve unhashed web application bundles (`.mjs`, `.js`, `.wasm`, and `.css`) with `Cache-Control: no-cache` so browsers revalidate them with nginx before reusing cached copies and pick up a newly deployed web version on a normal reload without clearing cache or cookies.

#### Scenario: Reload after a new deployment
- **WHEN** a new web container image is deployed with updated `.mjs`, `.wasm`, or `.css` files and a browser reloads the page
- **THEN** the browser revalidates the assets against nginx, receives the updated bundle, and runs the new version without clearing cache or cookies

#### Scenario: Unchanged deployment keeps fast conditional caching
- **WHEN** a browser reloads the page while the deployed web assets have not changed
- **THEN** nginx serves `Cache-Control: no-cache` with ETag validation so unchanged assets answer `304 Not Modified` without re-downloading the bundle
