# Tasks

## 1. The specs

- [x] 1.1 Split the 22 long requirements, keeping each header and every scenario and giving each moved behaviour its own requirement and scenario; verified by `openspec validate split-long-requirements --strict` passing with no warning
- [x] 1.2 Archive the change so the ten specs pick up the split; verified by `openspec validate <spec> --type spec --strict` reporting no warning for each of them, and by every old scenario and every word of each old requirement still being found in the specs
