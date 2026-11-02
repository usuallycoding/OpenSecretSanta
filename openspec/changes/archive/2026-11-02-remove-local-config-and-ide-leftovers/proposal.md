# Proposal

## Why

`app/webApp/resources/config.json` is a stale second copy of a file the web container already generates from `.env` every time it starts, and it carries a Firebase web key pair and sender settings of a Firebase project that no longer exists. Local runs go through `docker compose` and its `.env`, so the copy has no remaining use. Ten duplicate iOS run configurations in `.idea/runConfigurations` are the same kind of leftover, two of them with a personal Apple team ID.

## What Changes

- Delete `app/webApp/resources/config.json`. The web container keeps generating `config.json` at start, and a web app served without one already talks to its own origin and offers no push.
- Fix `README.md`: remove the sentence about the sample `config.json`, say that the web app needs the server's address from `PUBLIC_SERVER_URL` through docker compose, and stop listing `./kotlin run -m webApp` as a way to run the web app on its own.
- Delete `.idea/runConfigurations/iosApp2.xml` to `iosApp11.xml`, which declare the same run configuration as `iosApp.xml`, and keep `iosApp.xml`.

## Capabilities

### New Capabilities

### Modified Capabilities

No requirement changes: the specs already describe the web app without a `config.json`, and the generated one. The change sets `skip_specs: true`.

## Impact

- Removed: `app/webApp/resources/config.json` and ten files under `.idea/runConfigurations/`.
- Edited: `README.md`.
- The web app run alone with `./kotlin run -m webApp` no longer finds a server; use `docker compose up --build`.
- Not in this change: the old service-account key in `secrets/`, the `DEVELOPMENT_SERVER` address and the earlier `opensecretsanta.db`, which was deleted by hand and git ignored.
- The old key stays in git history and in earlier published images; it belongs to a deleted Firebase project and does nothing.
