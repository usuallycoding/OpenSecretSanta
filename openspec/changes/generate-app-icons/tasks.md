## 1. The source and the script

- [x] 1.1 Move `secret-santa-icon.svg` to `assets/secret-santa-icon.svg`
- [x] 1.2 Add `scripts/generate-icons.sh` with `--source`, `--background` and `--help`: it checks the source and tools (`rsvg-convert`, `magick`) before writing, renders the master and makes a scratch folder it removes on exit
- [x] 1.3 Make its output deterministic (metadata stripped) and check that two runs leave `git status` unchanged

## 2. The platforms

- [x] 2.1 Android: the legacy `ic_launcher.png` and `ic_launcher_round.png` at five densities, the adaptive foreground PNG, the solid background vector, and removal of the template's `drawable-v24` foreground vector; check the two adaptive XML files still resolve
- [x] 2.2 iOS: the 1024×1024 `app-icon-1024.png` without alpha
- [x] 2.3 Web: `favicon.ico` (16/32/48), `apple-touch-icon.png`, `icon-192.png`, `icon-512.png` and `manifest.webmanifest` in the web resources
- [x] 2.4 Add the favicon, touch-icon, manifest and theme-color links to `app/webApp/resources/index.html`

## 3. Documentation

- [x] 3.1 Add a README section: where the artwork lives, the tools, `scripts/generate-icons.sh` and its options, what it writes, and that the results are committed

## 4. Verify

- [x] 4.1 Check every generated file's size and transparency with `magick identify` (iOS opaque, the others as specified) and look at a contact sheet of them
- [x] 4.2 Build the Android app (debug and release) and check with `aapt2` that the launcher icons are the new ones and the template files are gone; run `shellcheck` on the script
- [x] 4.3 Build the web app, serve it and check with headless Chrome that the icon, touch icon and manifest answer 200 and that the manifest has no errors; check the nginx image's content type for `.webmanifest` (it has none, so `nginx.conf` now sets one)
- [x] 4.4 `openspec validate generate-app-icons --strict`
- [x] 4.5 The person looks at the icon on a phone, in the browser tab and in the iOS project, and decides whether the background and the sizes suit
