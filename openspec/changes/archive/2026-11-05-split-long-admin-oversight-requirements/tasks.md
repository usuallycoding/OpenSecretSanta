# Tasks

## 1. The spec

- [x] 1.1 Cut "The draw", "Participant wishlist" and "User sheet" to one behaviour each, keeping their headers and every scenario, and add the seven requirements for the behaviours taken out, each with its own scenario; verified by `openspec validate split-long-admin-oversight-requirements --strict` passing with no warning
- [x] 1.2 Archive the change so `client-admin-oversight` picks up the split; verified by `openspec validate client-admin-oversight --type spec --strict` reporting no warning
