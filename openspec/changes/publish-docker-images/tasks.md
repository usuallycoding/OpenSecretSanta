## 1. Images that build for both platforms

- [x] 1.1 Make the build stage of `server/Dockerfile` and `app/webApp/Dockerfile` run on the build platform (`--platform=$BUILDPLATFORM`)
- [x] 1.2 Build both images for `linux/arm64` locally, run the server image and check that `GET /health` answers 200 (so SQLite's arm64 library works), and check the web image serves `config.json` for another `PUBLIC_SERVER_URL`
- [x] 1.3 Check that the built images hold no `.env`, `secrets` or `.db` file

## 2. The workflow

- [x] 2.1 Add `.github/workflows/docker-images.yml`: triggers on `main`, `vX.Y.Z` tags and by hand; QEMU, Buildx, GHCR login with `GITHUB_TOKEN`, metadata tags, and a matrix of the two images built for `linux/amd64,linux/arm64`
- [x] 2.2 Check the workflow file for mistakes (YAML parses, the permissions are `contents: read` and `packages: write` only, `latest` only for `main`)

## 3. Compose and documentation

- [x] 3.1 Add the `image:` lines to `docker-compose.yml` and `IMAGE_TAG` to `.env.example`; check `docker compose config` and that `up --build` still builds
- [x] 3.2 Add the Raspberry Pi section to the README: the files to copy, `docker compose pull && docker compose up -d`, updating, rolling back with `IMAGE_TAG`, making the packages public once, and what the images do not contain

## 4. Verify

- [x] 4.1 `openspec validate publish-docker-images --strict`
- [ ] 4.2 The person pushes, watches the first run on GitHub, makes the two packages public, and runs the stack on the Pi
