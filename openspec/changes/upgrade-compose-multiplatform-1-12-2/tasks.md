## 1. Upgrade

- [x] 1.1 Set `settings.compose.version: 1.12.2` in `app/shared`, `app/webApp`, `app/androidApp` and `app/iosApp`, and the back-handler version in `libs.versions.toml`
- [x] 1.2 Check which versions of the Compose libraries, Material 3 among them, the build now resolves

## 2. Check

- [x] 2.1 Run `./kotlin test -m shared` and `./kotlin build`
- [ ] 2.2 On an iPhone with iOS 27, open the web app in Safari and tap the sign-in fields: the keyboard opens, typing works, and the Show password eye still works
- [ ] 2.3 Look at the main screens on Android and iOS, and on the web in a desktop browser, for anything the new release changed
