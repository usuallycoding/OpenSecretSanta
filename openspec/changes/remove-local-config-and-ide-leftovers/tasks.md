# Tasks

## 1. The stale web config

- [x] 1.1 Delete `app/webApp/resources/config.json` with `git rm`; verify `git ls-files | grep config.json` lists nothing and `./kotlin build -m webApp -p wasmJs -v release` still succeeds
- [x] 1.2 In `README.md`, replace the sentence about the sample in `app/webApp/resources/config.json` (the paragraph that starts "The web app reads `config.json` every time it starts") with the facts that apply now: without a `config.json` the web app uses its own origin, and under docker compose the web container writes it from `PUBLIC_SERVER_URL`; verify with `grep -n "resources/config.json" README.md`, which finds nothing
- [x] 1.3 In `README.md`'s "Running" list, replace the `./kotlin run -m webApp` line with a pointer to `docker compose up --build` for the web app, and keep the other three lines; verify the list still reads correctly and that no other README line tells the reader to run the web app on its own
- [x] 1.4 Verify the web stack still starts with its generated config: run `docker compose up --build -d`, then `curl http://localhost:8080/config.json` and confirm it names the `PUBLIC_SERVER_URL` in `.env`, then stop the stack

## 2. The duplicate iOS run configurations

- [x] 2.1 Delete `.idea/runConfigurations/iosApp2.xml` to `iosApp11.xml` with `git rm`, keeping `iosApp.xml`; verify `git ls-files .idea/runConfigurations` lists only `iosApp.xml` and `grep -rn DEVELOPMENT_TEAM=\"2` .idea` finds nothing
