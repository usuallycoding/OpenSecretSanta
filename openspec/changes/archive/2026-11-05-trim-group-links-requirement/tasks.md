# Tasks

## 1. The spec

- [x] 1.1 Cut "Group links open in the Android app" to one behaviour (keeping its header and all four scenarios) and add "A link opens its screen as a notification does" with its own scenarios; verified by `openspec validate trim-group-links-requirement --strict` passing
- [x] 1.2 Archive the change so `client-app-links` picks up the split; verified by `openspec validate client-app-links --type spec --strict` reporting no warning
